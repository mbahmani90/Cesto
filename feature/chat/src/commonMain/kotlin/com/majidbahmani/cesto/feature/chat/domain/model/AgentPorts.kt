package com.majidbahmani.cesto.feature.chat.domain.model

import kotlinx.coroutines.flow.Flow

/** The LLM, behind an interface: the loop doesn't know it's Gemini. */
interface AgentModel {
    /** False without a Gemini key. */
    val isReady: Flow<Boolean>

    /** @throws AgentUnavailableException when it can't answer now (no key, quota, offline). */
    suspend fun reply(conversation: List<AgentTurn>): ModelReply
}

/** The tools, run on the phone (SQL on the local database). Never throws: errors become a result the model can read. */
interface ReceiptTools {
    suspend fun run(call: ToolCall): ToolResult
}
