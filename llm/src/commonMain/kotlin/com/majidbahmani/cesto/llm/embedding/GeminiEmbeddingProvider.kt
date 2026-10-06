package com.majidbahmani.cesto.llm.embedding

import com.majidbahmani.cesto.llm.GeminiApi
import com.majidbahmani.cesto.llm.GeminiKeyStore
import com.majidbahmani.cesto.llm.embedding.EmbeddingUnavailableException.Reason
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.sqrt

/**
 * Gemini embeddings with the user's own key. Only the given texts are sent (product name + category, or the
 * search words); the vectors are stored and compared on the phone.
 */
class GeminiEmbeddingProvider(
    private val gemini: GeminiApi,
    private val keys: GeminiKeyStore,
    private val retryWaitMillis: Long = RETRY_WAIT_MILLIS,
) : EmbeddingProvider {

    override val modelId: String = "${GeminiApi.EMBEDDING_MODEL}@${GeminiApi.EMBEDDING_DIMENSIONS}"

    override suspend fun embedDocuments(texts: List<String>): List<FloatArray> {
        val key = key()
        // gemini-embedding-2 takes its task as a text prefix; no title for a product.
        return texts.chunked(MAX_BATCH).flatMap { batch -> request(key, batch.map { "title: none | text: $it" }) }
    }

    override suspend fun embedQuery(text: String): FloatArray =
        request(key(), listOf("task: search result | query: $text")).single()

    private suspend fun key(): String =
        keys.key.first()?.takeIf { it.isNotBlank() } ?: throw EmbeddingUnavailableException(Reason.NO_KEY)

    /** One retry after an overload (5xx); other errors become a [Reason]. */
    private suspend fun request(key: String, texts: List<String>): List<FloatArray> {
        repeat(2) { attempt ->
            try {
                return gemini.embed(key, texts).map { it.normalized() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ServerResponseException) {
                if (attempt == 0) delay(retryWaitMillis) else throw EmbeddingUnavailableException(Reason.BUSY, e)
            } catch (e: ClientRequestException) {
                val reason = when (e.response.status) {
                    HttpStatusCode.TooManyRequests -> Reason.QUOTA
                    HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> Reason.KEY_REJECTED
                    else -> Reason.FAILED
                }
                throw EmbeddingUnavailableException(reason, e)
            } catch (e: ResponseException) {
                throw EmbeddingUnavailableException(Reason.FAILED, e)
            } catch (e: Exception) {
                throw EmbeddingUnavailableException(Reason.NO_CONNECTION, e) // offline, timeout, bad answer
            }
        }
        error("unreachable")
    }

    private companion object {
        /** Texts per batchEmbedContents request. */
        const val MAX_BATCH = 100
        const val RETRY_WAIT_MILLIS = 2_000L
    }
}

/** Unit length, so cosine similarity is a plain dot product (Google normalizes 768 already; this makes it certain). */
internal fun FloatArray.normalized(): FloatArray {
    val norm = sqrt(sumOf { (it * it).toDouble() }).toFloat()
    return if (norm == 0f) this else FloatArray(size) { this[it] / norm }
}
