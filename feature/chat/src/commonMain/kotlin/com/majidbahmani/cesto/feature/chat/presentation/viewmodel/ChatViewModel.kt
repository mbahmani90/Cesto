package com.majidbahmani.cesto.feature.chat.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.majidbahmani.cesto.feature.chat.domain.model.AgentTurn
import com.majidbahmani.cesto.feature.chat.domain.model.AskResult
import com.majidbahmani.cesto.feature.chat.domain.usecase.AskQuestionUseCase
import com.majidbahmani.cesto.feature.chat.domain.usecase.ObserveAssistantReadyUseCase
import com.majidbahmani.cesto.feature.chat.presentation.mapper.toThinkingStep
import com.majidbahmani.cesto.feature.chat.presentation.model.ChatMessage
import com.majidbahmani.cesto.feature.chat.presentation.model.ThinkingStep
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(observeAssistantReady: ObserveAssistantReadyUseCase, private val askQuestion: AskQuestionUseCase) : ViewModel() {

    private data class ScreenState(
        val messages: List<ChatMessage> = emptyList(),
        val input: String = "",
        val thinking: ThinkingStep? = null
    )

    private val screenState = MutableStateFlow(ScreenState())

    /** What the model has seen so far (incl. its own turns), for follow-up questions. Not UI state. */
    private var conversation: List<AgentTurn> = emptyList()
    private var nextId = 0L
    private var askJob: Job? = null

    val uiState: StateFlow<ChatUiState> = combine(observeAssistantReady(), screenState) { ready, screen ->
        ChatUiState(isReady = ready, messages = screen.messages, input = screen.input, thinking = screen.thinking)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

    fun onInputChange(text: String) = screenState.update { it.copy(input = text) }

    fun onSend() = ask(screenState.value.input)

    fun onSuggestion(question: String) = ask(question)

    /** Starts over: the model forgets earlier questions too. */
    fun onNewChat() {
        askJob?.cancel()
        conversation = emptyList()
        screenState.update { ScreenState(input = it.input) }
    }

    private fun ask(text: String) {
        val question = text.trim()
        if (question.isEmpty() || screenState.value.thinking != null || uiState.value.isReady == false) return
        screenState.update {
            it.copy(
                messages = it.messages + ChatMessage.Question(nextId++, question),
                input = "",
                thinking = ThinkingStep.THINKING
            )
        }
        askJob = viewModelScope.launch {
            val result = askQuestion(conversation, question) { call ->
                screenState.update { it.copy(thinking = call.toThinkingStep()) }
            }
            val message = when (result) {
                is AskResult.Answered -> {
                    conversation = result.conversation
                    ChatMessage.Answer(nextId++, result.text, result.receiptIds.size)
                }

                // The failed question stays out of the conversation: asking again starts it cleanly.
                is AskResult.Failed -> ChatMessage.Failure(nextId++, result.reason)
            }
            screenState.update { it.copy(messages = it.messages + message, thinking = null) }
        }
    }
}
