package com.majidbahmani.cesto.core.network

import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable

class HttpClientFactoryTest {

    @Serializable
    private data class SampleDto(val id: String)

    private fun client(status: HttpStatusCode = HttpStatusCode.OK, body: String) =
        createHttpClient(MockEngine { respondJson(body, status) })

    private fun MockRequestHandleScope.respondJson(body: String, status: HttpStatusCode): HttpResponseData =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun decodesJson_ignoringUnknownKeys() = runTest {
        val dto: SampleDto = client(body = """{"id":"1","unknown":true}""").get("https://example.com").body()

        assertEquals(SampleDto(id = "1"), dto)
    }

    @Test
    fun serverError_throws() = runTest {
        assertFailsWith<ServerResponseException> {
            client(status = HttpStatusCode.InternalServerError, body = "{}").get("https://example.com")
        }
    }
}
