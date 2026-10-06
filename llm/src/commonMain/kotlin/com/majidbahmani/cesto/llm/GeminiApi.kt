package com.majidbahmani.cesto.llm

import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException

/** Outcome of checking a key with one small Gemini request. */
enum class KeyCheck {
    VALID,

    /** HTTP 400/401: not a Gemini API key, or deleted. */
    INVALID_KEY,

    /** HTTP 403: the key's project hasn't enabled the Gemini API, or the key is restricted. */
    NOT_ALLOWED,

    /** No answer from Google (offline, timeout). */
    NO_CONNECTION,

    /** Any other answer (429 quota, 5xx). */
    FAILED,
}

/**
 * Gemini REST API (generativelanguage.googleapis.com) with the user's own key.
 * The key goes in the `x-goog-api-key` header, never in the URL (URLs end up in logs).
 */
class GeminiApi(private val client: HttpClient) {

    /** Lists one model: the cheapest call that proves the key works, without generating anything. */
    suspend fun checkKey(key: String): KeyCheck = try {
        client.get(BASE_URL + "models") {
            header(API_KEY_HEADER, key)
            parameter("pageSize", 1)
        }
        KeyCheck.VALID
    } catch (e: CancellationException) {
        throw e
    } catch (e: ClientRequestException) {
        when (e.response.status) {
            HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized -> KeyCheck.INVALID_KEY
            HttpStatusCode.Forbidden -> KeyCheck.NOT_ALLOWED
            else -> KeyCheck.FAILED
        }
    } catch (e: ResponseException) {
        KeyCheck.FAILED // 5xx
    } catch (e: Exception) {
        KeyCheck.NO_CONNECTION
    }

    companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/"
        const val API_KEY_HEADER = "x-goog-api-key"
    }
}
