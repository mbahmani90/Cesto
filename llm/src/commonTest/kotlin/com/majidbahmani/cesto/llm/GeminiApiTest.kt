package com.majidbahmani.cesto.llm

import com.majidbahmani.cesto.core.network.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GeminiApiTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun api(status: HttpStatusCode = HttpStatusCode.OK, body: String = """{"models":[]}""") = GeminiApi(
        createHttpClient(
            MockEngine { request ->
                requests += request
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ),
    )

    /** Shape of Gemini's real answer for an invalid key (checked with curl and an invented key). */
    private val invalidKeyBody = """{"error":{"code":400,"message":"API key not valid. Please pass a valid API key.",
        |"status":"INVALID_ARGUMENT","details":[{"reason":"API_KEY_INVALID"}]}}""".trimMargin()

    @Test
    fun validKey_keyInHeaderNotInUrl() = runTest {
        assertEquals(KeyCheck.VALID, api().checkKey("AIza-test"))

        val request = requests.single()
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models", request.url.toString().substringBefore('?'))
        assertEquals("AIza-test", request.headers[GeminiApi.API_KEY_HEADER])
        assertFalse(request.url.toString().contains("AIza-test"))
    }

    @Test
    fun answers_mapToTheReason() = runTest {
        assertEquals(KeyCheck.INVALID_KEY, api(HttpStatusCode.BadRequest, invalidKeyBody).checkKey("x"))
        assertEquals(KeyCheck.INVALID_KEY, api(HttpStatusCode.Unauthorized).checkKey("x"))
        assertEquals(KeyCheck.NOT_ALLOWED, api(HttpStatusCode.Forbidden).checkKey("x"))
        assertEquals(KeyCheck.FAILED, api(HttpStatusCode.TooManyRequests).checkKey("x"))
        assertEquals(KeyCheck.FAILED, api(HttpStatusCode.ServiceUnavailable).checkKey("x"))
    }

    @Test
    fun noAnswer_isNoConnection() = runTest {
        val api = GeminiApi(createHttpClient(MockEngine { throw IOException("offline") }))

        assertEquals(KeyCheck.NO_CONNECTION, api.checkKey("x"))
    }
}
