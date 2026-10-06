package com.majidbahmani.cesto.llm

import com.majidbahmani.cesto.llm.dto.ContentDto
import com.majidbahmani.cesto.llm.dto.GenerateContentRequestDto
import com.majidbahmani.cesto.llm.dto.GenerateContentResponseDto
import com.majidbahmani.cesto.llm.dto.GenerationConfigDto
import com.majidbahmani.cesto.llm.dto.PartDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
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

    /**
     * One prompt → JSON matching [responseSchema] (Gemini's structured output), as text.
     * Throws ClientRequestException (4xx: e.g. 400 invalid key, 403 not allowed, 429 quota) or
     * ResponseException (5xx); [GeminiEmptyResponseException] if Gemini returned no text.
     */
    suspend fun generateJson(
        key: String,
        systemInstruction: String,
        prompt: String,
        responseSchema: JsonObject,
        model: String = EXTRACTION_MODEL,
    ): String {
        val response: GenerateContentResponseDto = client.post(BASE_URL + "models/$model:generateContent") {
            header(API_KEY_HEADER, key)
            contentType(ContentType.Application.Json)
            // Generating takes longer than the client's defaults.
            timeout {
                requestTimeoutMillis = 120_000
                socketTimeoutMillis = 120_000
            }
            setBody(
                GenerateContentRequestDto(
                    contents = listOf(ContentDto(role = "user", parts = listOf(PartDto(text = prompt)))),
                    systemInstruction = ContentDto(parts = listOf(PartDto(text = systemInstruction))),
                    generationConfig = GenerationConfigDto(
                        temperature = 0.0, // extraction: the same receipt should give the same items
                        responseMimeType = "application/json",
                        responseSchema = responseSchema,
                    ),
                ),
            )
        }.body()
        val candidate = response.candidates.firstOrNull()
        return candidate?.content?.parts?.mapNotNull { it.text }?.joinToString("")?.takeIf { it.isNotBlank() }
            ?: throw GeminiEmptyResponseException(candidate?.finishReason)
    }

    /**
     * One round of a conversation with tools (function calling). [contents] are Gemini `Content` objects
     * as JSON; the returned model content must be sent back **unchanged** in the next round (Gemini 3
     * models put a `thoughtSignature` in it). Same exceptions as [generateJson].
     */
    suspend fun generateContent(
        key: String,
        systemInstruction: String,
        contents: List<JsonObject>,
        functionDeclarations: JsonArray,
        model: String = CHAT_MODEL,
    ): JsonObject {
        val body = buildJsonObject {
            putJsonArray("contents") { contents.forEach { add(it) } }
            putJsonObject("systemInstruction") {
                putJsonArray("parts") { add(buildJsonObject { put("text", systemInstruction) }) }
            }
            putJsonArray("tools") { add(buildJsonObject { put("functionDeclarations", functionDeclarations) }) }
            putJsonObject("generationConfig") { put("temperature", 0.2) }
        }
        val response: JsonObject = client.post(BASE_URL + "models/$model:generateContent") {
            header(API_KEY_HEADER, key)
            contentType(ContentType.Application.Json)
            timeout {
                requestTimeoutMillis = 120_000
                socketTimeoutMillis = 120_000
            }
            setBody(body)
        }.body()
        val candidate = response["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
        return candidate?.get("content")?.jsonObject
            ?: throw GeminiEmptyResponseException(candidate?.get("finishReason")?.toString())
    }

    companion object {
        /** Current Flash model (checked in Google's model list, Oct 2026); one place to switch. */
        const val EXTRACTION_MODEL = "gemini-3.8-flash"

        /** The agent loop: tool choice and the answer text. */
        const val CHAT_MODEL = "gemini-3.8-flash"

        /** Cheaper, separate capacity: used when the main model is overloaded (503). */
        const val FALLBACK_MODEL = "gemini-3.5-flash-lite"
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/"
        const val API_KEY_HEADER = "x-goog-api-key"
    }
}

/** Gemini answered without text (e.g. finishReason SAFETY or MAX_TOKENS). */
class GeminiEmptyResponseException(val finishReason: String?) : Exception("Gemini returned no text ($finishReason)")
