package com.majidbahmani.cesto.feature.chat.fake

import com.majidbahmani.cesto.feature.chat.domain.model.AgentModel
import com.majidbahmani.cesto.feature.chat.domain.model.AgentTurn
import com.majidbahmani.cesto.feature.chat.domain.model.ModelReply
import com.majidbahmani.cesto.feature.chat.domain.model.ReceiptTools
import com.majidbahmani.cesto.feature.chat.domain.model.ToolCall
import com.majidbahmani.cesto.feature.chat.domain.model.ToolResult
import com.majidbahmani.cesto.llm.GeminiKeyStore
import com.majidbahmani.cesto.llm.embedding.EmbeddingProvider
import com.majidbahmani.cesto.llm.embedding.EmbeddingUnavailableException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Plays a script of replies (or throws), and records what the conversation looked like each round. */
class ScriptedAgentModel(vararg replies: suspend () -> ModelReply) : AgentModel {
    private val script = ArrayDeque(replies.toList())
    val seen = mutableListOf<List<AgentTurn>>()
    override val isReady = MutableStateFlow(true)

    override suspend fun reply(conversation: List<AgentTurn>): ModelReply {
        seen += conversation.toList()
        return script.removeFirst()()
    }
}

fun answer(text: String) = ModelReply.Answer(text, modelTurn(text))

fun useTools(vararg names: String) =
    ModelReply.UseTools(names.map { ToolCall(it, JsonObject(emptyMap())) }, modelTurn(names.joinToString()))

fun modelTurn(marker: String) = AgentTurn.Model(JsonObject(mapOf("role" to JsonPrimitive("model"), "marker" to JsonPrimitive(marker))))

/** Every tool returns {"ok": name} and the receipt ids given for it. */
class FakeReceiptTools(private val receiptIds: Map<String, List<Long>> = emptyMap()) : ReceiptTools {
    val calls = mutableListOf<ToolCall>()

    override suspend fun run(call: ToolCall): ToolResult {
        calls += call
        return ToolResult(call, buildJsonObject { put("ok", call.name) }, receiptIds[call.name].orEmpty())
    }
}

class FakeGeminiKeyStore(initial: String? = "AIza-test") : GeminiKeyStore {
    override val key = MutableStateFlow(initial)
    override suspend fun save(key: String) {
        this.key.value = key
    }
    override suspend fun clear() {
        key.value = null
    }
}

/** Search words → the vectors given in [queries]; records what was asked. */
class FakeEmbeddingProvider(private val queries: Map<String, FloatArray> = emptyMap()) : EmbeddingProvider {
    override val modelId = "fake@2"
    val asked = mutableListOf<String>()
    var unavailable: EmbeddingUnavailableException.Reason? = null

    override suspend fun embedDocuments(texts: List<String>): List<FloatArray> = error("not used by the tools")

    override suspend fun embedQuery(text: String): FloatArray {
        unavailable?.let { throw EmbeddingUnavailableException(it) }
        asked += text
        return queries.getValue(text)
    }
}
