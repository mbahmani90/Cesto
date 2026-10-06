package com.majidbahmani.cesto.feature.chat.presentation.viewmodel

import com.majidbahmani.cesto.feature.chat.presentation.model.ChatMessage
import com.majidbahmani.cesto.feature.chat.presentation.model.ThinkingStep

data class ChatUiState(
    /** Null until the key store has answered; false: no Gemini key, the screen points to Settings. */
    val isReady: Boolean? = null,
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    /** Non-null while a question is being answered. */
    val thinking: ThinkingStep? = null,
) {
    val canSend: Boolean get() = isReady == true && thinking == null && input.isNotBlank()
}
