package com.majidbahmani.cesto.feature.receipts.data.extraction

import com.majidbahmani.cesto.core.network.createHttpClient
import com.majidbahmani.cesto.llm.GeminiApi
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GeminiReceiptItemExtractorTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun extractor(status: HttpStatusCode = HttpStatusCode.OK, answer: String = "") =
        extractor(answer) { status }

    /** [statusFor] decides each answer's status by request number (0, 1, …). */
    private fun extractor(answer: String, statusFor: (Int) -> HttpStatusCode) = GeminiReceiptItemExtractor(
        GeminiApi(
            createHttpClient(
                MockEngine { request ->
                    val status = statusFor(requests.size)
                    requests += request
                    val body = """{"candidates":[{"content":{"parts":[{"text":${Json.encodeToString(answer)}}]},"finishReason":"STOP"}]}"""
                    respond(if (status == HttpStatusCode.OK) body else "{}", status, headersOf(HttpHeaders.ContentType, "application/json"))
                },
            ),
        ),
    )

    /** What Gemini's structured output looks like for the two lines of the invented sample. */
    private val answer = """{"lines":[
        {"kind":"ITEM","rawName":"LEITE PAST GORDO 1L","normalizedName":"Leite pasteurizado gordo 1 L","category":"Laticinios/Beb. Veg.",
         "quantity":1,"unit":"UNIT","unitsPerPack":1,"unitPrice":1.19,"lineTotal":1.19},
        {"kind":"ITEM","rawName":"BANANA","normalizedName":"Banana","category":"Frutas e Legumes",
         "quantity":0.76,"unit":"KG","unitsPerPack":1,"unitPrice":1.19,"lineTotal":0.9},
        {"kind":"DEPOSIT","rawName":"VALOR DE DEPOSITO UN","normalizedName":"Valor de depósito","category":null,
         "quantity":1,"unit":"UNIT","unitsPerPack":1,"unitPrice":0.1,"lineTotal":0.1}]}"""

    @Test
    fun answer_becomesLinesInCents() = runTest {
        val lines = extractor(answer = answer).extract("AIza-test", "section text")

        assertEquals(listOf(119L, 90L, 10L), lines.map { it.lineTotalCents })
        assertEquals(ExtractedLine.Unit.KG, lines[1].unit)
        assertEquals(0.76, lines[1].quantity)
        assertEquals(ExtractedLine.Kind.DEPOSIT, lines[2].kind)
        assertEquals("Leite pasteurizado gordo 1 L", lines[0].normalizedName)
    }

    @Test
    fun request_onlyTheSection_keyInHeader_structuredOutput() = runTest {
        extractor(answer = answer).extract("AIza-test", "(A) BANANA\n0,760 X 1,19 0,90")

        val request = requests.single()
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/${GeminiApi.EXTRACTION_MODEL}:generateContent",
            request.url.toString(),
        )
        assertEquals("AIza-test", request.headers[GeminiApi.API_KEY_HEADER])
        val body = Json.parseToJsonElement((request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()).jsonObject
        val prompt = body["contents"]!!.jsonArray[0].jsonObject["parts"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content
        assertEquals("(A) BANANA\n0,760 X 1,19 0,90", prompt)
        val config = body["generationConfig"]!!.jsonObject
        assertEquals("application/json", config["responseMimeType"]!!.jsonPrimitive.content)
        assertEquals("OBJECT", config["responseSchema"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertTrue(body["systemInstruction"].toString().contains("Continente"))
    }

    @Test
    fun rejectedKeyOrQuota_stopsExtraction() = runTest {
        listOf(HttpStatusCode.BadRequest, HttpStatusCode.Forbidden, HttpStatusCode.TooManyRequests).forEach { status ->
            assertFailsWith<ExtractionUnavailableException>("for $status") { extractor(status).extract("x", "s") }
        }
    }

    @Test
    fun overloaded_isRetried() = runTest {
        val lines = extractor(answer) { attempt -> if (attempt == 0) HttpStatusCode.ServiceUnavailable else HttpStatusCode.OK }
            .extract("AIza-test", "section")

        assertEquals(3, lines.size)
        assertEquals(2, requests.size)
    }

    @Test
    fun mainModelStaysOverloaded_theFallbackModelIsUsed() = runTest {
        extractor(answer) { attempt -> if (attempt < 2) HttpStatusCode.ServiceUnavailable else HttpStatusCode.OK }
            .extract("AIza-test", "section")

        assertTrue(requests[0].url.toString().contains(GeminiApi.EXTRACTION_MODEL))
        assertTrue(requests[2].url.toString().contains(GeminiApi.FALLBACK_MODEL))
    }

    @Test
    fun bothModelsOverloaded_pausesExtraction() = runTest {
        assertFailsWith<ExtractionUnavailableException> {
            extractor(answer) { HttpStatusCode.ServiceUnavailable }.extract("AIza-test", "section")
        }
        assertEquals(4, requests.size) // 2 tries per model, then stop
    }
}
