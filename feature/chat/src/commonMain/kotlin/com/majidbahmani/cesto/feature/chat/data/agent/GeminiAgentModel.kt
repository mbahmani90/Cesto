package com.majidbahmani.cesto.feature.chat.data.agent

import com.majidbahmani.cesto.core.logging.logWarning
import com.majidbahmani.cesto.feature.chat.data.tools.receiptToolDeclarations
import com.majidbahmani.cesto.feature.chat.domain.model.AgentModel
import com.majidbahmani.cesto.feature.chat.domain.model.AgentTurn
import com.majidbahmani.cesto.feature.chat.domain.model.AgentUnavailableException
import com.majidbahmani.cesto.feature.chat.domain.model.AskFailure
import com.majidbahmani.cesto.feature.chat.domain.model.ModelReply
import com.majidbahmani.cesto.feature.chat.domain.model.ToolCall
import com.majidbahmani.cesto.llm.GeminiApi
import com.majidbahmani.cesto.llm.GeminiEmptyResponseException
import com.majidbahmani.cesto.llm.GeminiKeyStore
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * The agent's model is Gemini with function calling. Each round sends the whole conversation (Gemini keeps
 * no state): the question, the model's earlier turns unchanged, and the tools' small results.
 */
class GeminiAgentModel(
    private val gemini: GeminiApi,
    private val keys: GeminiKeyStore,
    private val prompt: ReceiptAgentPrompt,
    private val retryWaitMillis: Long = RETRY_WAIT_MILLIS
) : AgentModel {

    override val isReady: Flow<Boolean> = keys.key.map { !it.isNullOrBlank() }.distinctUntilChanged()

    override suspend fun reply(conversation: List<AgentTurn>): ModelReply {
        val key = keys.key.first()?.takeIf { it.isNotBlank() } ?: throw AgentUnavailableException(AskFailure.NO_KEY)
        val content = generate(key, prompt.build(), conversation.map { it.toContent() })
        return content.toReply()
    }

    /** One retry after an overload (503); other errors end the question with a reason the screen can show. */
    private suspend fun generate(key: String, systemInstruction: String, contents: List<JsonObject>): JsonObject {
        repeat(2) { attempt ->
            try {
                return gemini.generateContent(key, systemInstruction, contents, receiptToolDeclarations)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ServerResponseException) {
                if (attempt == 0) delay(retryWaitMillis) else throw AgentUnavailableException(AskFailure.BUSY, e)
            } catch (e: ClientRequestException) {
                throw AgentUnavailableException(e.toFailure(), e)
            } catch (e: ResponseException) {
                throw AgentUnavailableException(AskFailure.FAILED, e)
            } catch (e: GeminiEmptyResponseException) {
                logWarning(TAG, "Gemini returned no content (${e.finishReason})")
                throw AgentUnavailableException(AskFailure.FAILED, e)
            } catch (e: Exception) {
                throw AgentUnavailableException(AskFailure.NO_CONNECTION, e) // offline, timeout
            }
        }
        error("unreachable")
    }

    private suspend fun ClientRequestException.toFailure(): AskFailure {
        val status = response.status
        // Status and Google's error code only: the body never contains the key, but stays out of logs anyway.
        logWarning(TAG, "Gemini answered ${status.value}")
        return when (status) {
            HttpStatusCode.TooManyRequests -> AskFailure.QUOTA

            HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> AskFailure.KEY_REJECTED

            HttpStatusCode.BadRequest ->
                if (runCatching { response.bodyAsText() }.getOrDefault("").contains("API_KEY_INVALID")) {
                    AskFailure.KEY_REJECTED
                } else {
                    AskFailure.FAILED
                }

            else -> AskFailure.FAILED
        }
    }

    private companion object {
        const val TAG = "GeminiAgentModel"
        const val RETRY_WAIT_MILLIS = 2_000L
    }
}

/** A turn as a Gemini `Content`: tool results go back as `functionResponse` parts in a user turn. */
internal fun AgentTurn.toContent(): JsonObject = when (this) {
    is AgentTurn.User -> buildJsonObject {
        put("role", "user")
        putJsonArray("parts") { addJsonObject { put("text", text) } }
    }

    is AgentTurn.Model -> content

    is AgentTurn.ToolResults -> buildJsonObject {
        put("role", "user")
        putJsonArray("parts") {
            results.forEach { result ->
                addJsonObject {
                    putJsonObject("functionResponse") {
                        result.call.id?.let { put("id", it) }
                        put("name", result.call.name)
                        put("response", result.output)
                    }
                }
            }
        }
    }
}

/** Function calls win over text: the model sometimes says "Let me check" next to its calls. */
internal fun JsonObject.toReply(): ModelReply {
    val turn = AgentTurn.Model(
        if ("role" in this) this else JsonObject(this + ("role" to JsonPrimitive("model")))
    )
    val parts = (this["parts"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
    val calls = parts.mapNotNull { part ->
        val call = part["functionCall"] as? JsonObject ?: return@mapNotNull null
        val name = (call["name"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
        ToolCall(
            name = name,
            arguments = call["args"] as? JsonObject ?: JsonObject(emptyMap()),
            id = (call["id"] as? JsonPrimitive)?.contentOrNull
        )
    }
    if (calls.isNotEmpty()) return ModelReply.UseTools(calls, turn)
    val text = parts
        .filter { (it["thought"] as? JsonPrimitive)?.booleanOrNull != true } // thought summaries aren't the answer
        .mapNotNull { (it["text"] as? JsonPrimitive)?.contentOrNull }
        .joinToString("")
        .trim()
    if (text.isEmpty()) throw AgentUnavailableException(AskFailure.FAILED)
    return ModelReply.Answer(text, turn)
}
