package com.majidbahmani.cesto.feature.chat.domain.model

import kotlinx.serialization.json.JsonObject

/** A tool the model wants the app to run, e.g. findProducts(keywords = ["iogurte"]); [id] is echoed in the result. */
data class ToolCall(val name: String, val arguments: JsonObject, val id: String? = null)

/** What a tool returned (small JSON for the model) and the receipts it was based on (links for the user). */
data class ToolResult(val call: ToolCall, val output: JsonObject, val receiptIds: List<Long> = emptyList())

/** One entry of the conversation the model sees; the LLM is stateless, so all of it is sent every round. */
sealed interface AgentTurn {
    data class User(val text: String) : AgentTurn

    /** The model's own message, kept as it came and sent back unchanged (it may carry a thought signature). */
    data class Model(val content: JsonObject) : AgentTurn

    data class ToolResults(val results: List<ToolResult>) : AgentTurn
}

/** The model's next step: answer, or ask for tools first. */
sealed interface ModelReply {
    val turn: AgentTurn.Model

    data class Answer(val text: String, override val turn: AgentTurn.Model) : ModelReply

    data class UseTools(val calls: List<ToolCall>, override val turn: AgentTurn.Model) : ModelReply
}

enum class AskFailure {
    /** No Gemini key saved: set it up in Settings. */
    NO_KEY,
    KEY_REJECTED,

    /** Rate limit or quota (e.g. the free tier's 5 requests per minute). */
    QUOTA,

    /** Gemini overloaded (5xx): try again in a moment. */
    BUSY,
    NO_CONNECTION,

    /** The model kept calling tools past the safety limit. */
    TOO_MANY_STEPS,
    FAILED
}

sealed interface AskResult {
    /** [conversation] includes this question and answer: pass it back for follow-up questions. */
    data class Answered(val text: String, val receiptIds: List<Long>, val conversation: List<AgentTurn>) : AskResult

    data class Failed(val reason: AskFailure) : AskResult
}

/** Thrown by the model when no answer is possible now; the loop turns it into [AskResult.Failed]. */
class AgentUnavailableException(val reason: AskFailure, cause: Throwable? = null) : Exception(reason.name, cause)
