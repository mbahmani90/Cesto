package com.majidbahmani.cesto.llm.embedding

import com.majidbahmani.cesto.core.network.createHttpClient
import com.majidbahmani.cesto.llm.GeminiApi
import com.majidbahmani.cesto.llm.GeminiKeyStore
import com.majidbahmani.cesto.llm.embedding.EmbeddingUnavailableException.Reason
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeminiEmbeddingProviderTest {

    private val requests = mutableListOf<HttpRequestData>()
    private val keys = object : GeminiKeyStore {
        override val key = MutableStateFlow<String?>("AIza-test")
        override suspend fun save(key: String) { this.key.value = key }
        override suspend fun clear() { key.value = null }
    }

    /** Answers each request with one [3, 4] vector per text, unless [statuses] says otherwise. */
    private fun provider(vararg statuses: HttpStatusCode): GeminiEmbeddingProvider {
        val queue = ArrayDeque(statuses.toList())
        val engine = MockEngine { request ->
            requests += request
            val status = queue.removeFirstOrNull() ?: HttpStatusCode.OK
            val count = request.texts().size
            val body = if (status == HttpStatusCode.OK) {
                """{"embeddings":[${List(count) { """{"values":[3.0, 4.0]}""" }.joinToString(",")}]}"""
            } else {
                "{}"
            }
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return GeminiEmbeddingProvider(GeminiApi(createHttpClient(engine)), keys, retryWaitMillis = 0)
    }

    private fun HttpRequestData.json() = Json.parseToJsonElement((body as TextContent).text).jsonObject

    private fun HttpRequestData.texts(): List<String> = json()["requests"]!!.jsonArray.map {
        it.jsonObject["content"]!!.jsonObject["parts"]!!.jsonArray.single().jsonObject["text"]!!.jsonPrimitive.content
    }

    @Test
    fun documents_areBatched_withTheDocumentPrefix_keyInHeader() = runTest {
        val vectors = provider().embedDocuments(List(250) { "Produto $it. Categoria: Laticinios" })

        assertEquals(250, vectors.size)
        assertEquals(listOf(100, 100, 50), requests.map { it.texts().size })
        val first = requests.first()
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-2:batchEmbedContents", first.url.toString())
        assertEquals("AIza-test", first.headers[GeminiApi.API_KEY_HEADER])
        assertFalse(first.url.toString().contains("AIza"))
        assertEquals("title: none | text: Produto 0. Categoria: Laticinios", first.texts().first())
        val request = first.json()["requests"]!!.jsonArray.first().jsonObject
        assertEquals("models/gemini-embedding-2", request["model"]!!.jsonPrimitive.content)
        assertEquals(768, request["outputDimensionality"]!!.jsonPrimitive.int)
    }

    @Test
    fun query_hasTheSearchPrefix_andIsNormalized() = runTest {
        val vector = provider().embedQuery("dairy")

        assertEquals("task: search result | query: dairy", requests.single().texts().single())
        assertTrue(abs(vector[0] - 0.6f) < 1e-6 && abs(vector[1] - 0.8f) < 1e-6) // [3, 4] / 5
    }

    @Test
    fun modelId_namesModelAndSize() {
        assertEquals("gemini-embedding-2@768", provider().modelId)
    }

    @Test
    fun errors_becomeReasons() = runTest {
        assertReason(Reason.QUOTA) { provider(HttpStatusCode.TooManyRequests).embedQuery("x") }
        assertReason(Reason.KEY_REJECTED) { provider(HttpStatusCode.BadRequest).embedQuery("x") }
        assertReason(Reason.BUSY) { provider(HttpStatusCode.ServiceUnavailable, HttpStatusCode.ServiceUnavailable).embedQuery("x") }
        val offline = GeminiEmbeddingProvider(GeminiApi(createHttpClient(MockEngine { throw IOException("offline") })), keys)
        assertReason(Reason.NO_CONNECTION) { offline.embedQuery("x") }
    }

    @Test
    fun overload_isRetriedOnce() = runTest {
        provider(HttpStatusCode.ServiceUnavailable).embedQuery("x")

        assertEquals(2, requests.size)
    }

    @Test
    fun withoutKey_nothingIsSent() = runTest {
        keys.key.value = null

        assertReason(Reason.NO_KEY) { provider().embedDocuments(listOf("Banana")) }
        assertTrue(requests.isEmpty())
    }

    private suspend fun assertReason(expected: Reason, block: suspend () -> Unit) {
        assertEquals(expected, assertFailsWith<EmbeddingUnavailableException> { block() }.reason)
    }
}
