package com.majidbahmani.cesto.feature.chat.data.agent

import com.majidbahmani.cesto.core.network.createHttpClient
import com.majidbahmani.cesto.database.CestoDatabase
import com.majidbahmani.cesto.feature.chat.domain.model.AgentTurn
import com.majidbahmani.cesto.feature.chat.domain.model.AgentUnavailableException
import com.majidbahmani.cesto.feature.chat.domain.model.AskFailure
import com.majidbahmani.cesto.feature.chat.domain.model.ModelReply
import com.majidbahmani.cesto.feature.chat.domain.model.ToolResult
import com.majidbahmani.cesto.feature.chat.fake.FakeGeminiKeyStore
import com.majidbahmani.cesto.feature.chat.fake.createTestDriver
import com.majidbahmani.cesto.llm.GeminiApi
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.utils.io.errors.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class GeminiAgentModelTest {

    private val bodies = mutableListOf<JsonObject>()
    private val keys = FakeGeminiKeyStore()

    private val prompt = ReceiptAgentPrompt(
        database = CestoDatabase(createTestDriver()),
        ioDispatcher = Dispatchers.Unconfined,
        currentTimeMillis = { 1_791_288_000_000 } // 2026-10-06 12:00 UTC
    )

    /** Each request gets the next answer: a (status, body) pair. */
    private fun model(vararg answers: Pair<HttpStatusCode, String>): GeminiAgentModel {
        val queue = ArrayDeque(answers.toList())
        val engine = MockEngine { request ->
            bodies += Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            val (status, body) = queue.removeFirst()
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return GeminiAgentModel(GeminiApi(createHttpClient(engine)), keys, prompt, retryWaitMillis = 0)
    }

    private fun ok(content: String) = HttpStatusCode.OK to """{"candidates":[{"content":$content,"finishReason":"STOP"}]}"""

    private val callContent =
        """{"role":"model","parts":[{"functionCall":{"name":"findProducts","args":{"keywords":["iogurt"]}},""" +
            """"thoughtSignature":"c2lnbmF0dXJl"}]}"""

    @Test
    fun functionCall_becomesToolCalls_andTheModelTurnGoesBackUnchanged() = runTest {
        val model = model(ok(callContent), ok("""{"role":"model","parts":[{"text":"You bought 24 yogurts."}]}"""))
        val question = AgentTurn.User("How many yogurts?")

        val first = assertIs<ModelReply.UseTools>(model.reply(listOf(question)))
        val call = first.calls.single()
        assertEquals("findProducts", call.name)
        assertEquals("iogurt", call.arguments["keywords"]!!.jsonArray.single().jsonPrimitive.content)

        val output = buildJsonObject { put("count", 2) }
        val second = model.reply(listOf(question, first.turn, AgentTurn.ToolResults(listOf(ToolResult(call, output)))))

        assertEquals("You bought 24 yogurts.", assertIs<ModelReply.Answer>(second).text)
        val contents = bodies[1]["contents"]!!.jsonArray
        assertEquals(Json.parseToJsonElement(callContent), contents[1]) // thoughtSignature kept
        val response = contents[2].jsonObject["parts"]!!.jsonArray.single().jsonObject["functionResponse"]!!.jsonObject
        assertEquals("findProducts", response["name"]!!.jsonPrimitive.content)
        assertEquals(output, response["response"])
        assertEquals("user", contents[2].jsonObject["role"]!!.jsonPrimitive.content)
    }

    @Test
    fun request_hasTheToolsAndTodaysDate() = runTest {
        model(ok("""{"role":"model","parts":[{"text":"Hi"}]}""")).reply(listOf(AgentTurn.User("hi")))

        val body = bodies.single()
        val declarations = body["tools"]!!.jsonArray.single().jsonObject["functionDeclarations"]!!.jsonArray
        assertEquals(6, declarations.size)
        val instruction = body["systemInstruction"]!!.jsonObject["parts"]!!.jsonArray.single().jsonObject["text"]!!.jsonPrimitive.content
        assertTrue(instruction.contains("Today is 2026-10-06"))
        assertTrue(instruction.contains("no receipts yet"))
    }

    @Test
    fun thoughtSummaries_areNotPartOfTheAnswer() = runTest {
        val reply = model(ok("""{"role":"model","parts":[{"text":"Planning…","thought":true},{"text":"12,34 €"}]}"""))
            .reply(listOf(AgentTurn.User("q")))

        assertEquals("12,34 €", assertIs<ModelReply.Answer>(reply).text)
    }

    @Test
    fun errors_mapToReasons() = runTest {
        val invalidKey = """{"error":{"code":400,"status":"INVALID_ARGUMENT","details":[{"reason":"API_KEY_INVALID"}]}}"""
        assertFailure(AskFailure.QUOTA, model(HttpStatusCode.TooManyRequests to "{}"))
        assertFailure(AskFailure.KEY_REJECTED, model(HttpStatusCode.BadRequest to invalidKey))
        assertFailure(AskFailure.KEY_REJECTED, model(HttpStatusCode.Forbidden to "{}"))
        assertFailure(AskFailure.FAILED, model(HttpStatusCode.BadRequest to "{}"))
        assertFailure(AskFailure.BUSY, model(HttpStatusCode.ServiceUnavailable to "{}", HttpStatusCode.ServiceUnavailable to "{}"))
        assertFailure(AskFailure.FAILED, model(ok("""{"role":"model","parts":[]}""")))
    }

    @Test
    fun overload_isRetriedOnce() = runTest {
        val reply = model(HttpStatusCode.ServiceUnavailable to "{}", ok("""{"role":"model","parts":[{"text":"ok"}]}"""))
            .reply(listOf(AgentTurn.User("q")))

        assertEquals("ok", assertIs<ModelReply.Answer>(reply).text)
    }

    @Test
    fun offline_isNoConnection() = runTest {
        val model = GeminiAgentModel(GeminiApi(createHttpClient(MockEngine { throw IOException("offline") })), keys, prompt)

        assertFailure(AskFailure.NO_CONNECTION, model)
    }

    @Test
    fun withoutKey_nothingIsSent() = runTest {
        keys.key.value = null

        assertFailure(AskFailure.NO_KEY, model())
        assertTrue(bodies.isEmpty())
    }

    @Test
    fun systemInstruction_describesTheData() {
        val text = systemInstruction(
            today = LocalDate(2026, 10, 6),
            coverage = DataCoverage(LocalDate(2026, 1, 3), LocalDate(2026, 10, 5), receipts = 59, receiptsWithItems = 40)
        )

        assertTrue(text.contains("from 2026-01-03 to 2026-10-05: 59 receipts, items read on 40"))
        assertTrue(text.contains("(tuesday)"))
    }

    private suspend fun assertFailure(expected: AskFailure, model: GeminiAgentModel) {
        val e = assertFailsWith<AgentUnavailableException> { model.reply(listOf(AgentTurn.User("q"))) }
        assertEquals(expected, e.reason)
    }
}
