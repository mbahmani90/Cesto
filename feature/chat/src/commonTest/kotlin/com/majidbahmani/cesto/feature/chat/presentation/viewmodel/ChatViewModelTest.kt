package com.majidbahmani.cesto.feature.chat.presentation.viewmodel

import com.majidbahmani.cesto.feature.chat.domain.model.AgentUnavailableException
import com.majidbahmani.cesto.feature.chat.domain.model.AskFailure
import com.majidbahmani.cesto.feature.chat.domain.usecase.AskQuestionUseCase
import com.majidbahmani.cesto.feature.chat.domain.usecase.ObserveAssistantReadyUseCase
import com.majidbahmani.cesto.feature.chat.fake.FakeReceiptTools
import com.majidbahmani.cesto.feature.chat.fake.ScriptedAgentModel
import com.majidbahmani.cesto.feature.chat.fake.answer
import com.majidbahmani.cesto.feature.chat.fake.useTools
import com.majidbahmani.cesto.feature.chat.presentation.model.ChatMessage
import com.majidbahmani.cesto.feature.chat.presentation.model.ThinkingStep
import com.majidbahmani.cesto.feature.chat.domain.model.AgentTurn
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val tools = FakeReceiptTools(receiptIds = mapOf("sumQuantity" to listOf(1L, 2L)))

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(model: ScriptedAgentModel): ChatViewModel =
        ChatViewModel(ObserveAssistantReadyUseCase(model), AskQuestionUseCase(model, tools)).also {
            it.uiState.launchIn(backgroundScope) // WhileSubscribed: someone has to collect
            runCurrent()
        }

    @Test
    fun send_showsTheQuestion_thenTheAnswerWithItsReceipts() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val model = ScriptedAgentModel({ gate.await(); useTools("sumQuantity") }, { answer("24 yogurts") })
        val vm = viewModel(model)

        vm.onInputChange("How many yogurts?")
        vm.onSend()
        runCurrent() // waits at the gate: Gemini hasn't answered yet

        val asking = vm.uiState.value
        assertEquals(listOf("How many yogurts?"), asking.messages.map { (it as ChatMessage.Question).text })
        assertEquals("", asking.input)
        assertEquals(ThinkingStep.THINKING, asking.thinking)

        gate.complete(Unit)
        runCurrent()
        val done = vm.uiState.value
        val reply = assertIs<ChatMessage.Answer>(done.messages.last())
        assertEquals("24 yogurts", reply.text)
        assertEquals(2, reply.receiptCount)
        assertNull(done.thinking)
    }

    @Test
    fun followUp_keepsTheConversation_newChatForgetsIt() = runTest(dispatcher) {
        val model = ScriptedAgentModel({ answer("a1") }, { answer("a2") }, { answer("a3") })
        val vm = viewModel(model)

        vm.onSuggestion("q1"); runCurrent()
        vm.onSuggestion("q2"); runCurrent()
        assertEquals(listOf("q1", "q2"), model.seen[1].filterIsInstance<AgentTurn.User>().map { it.text })

        vm.onNewChat()
        runCurrent()
        assertTrue(vm.uiState.value.messages.isEmpty())
        vm.onSuggestion("q3"); runCurrent()
        assertEquals(listOf("q3"), model.seen[2].filterIsInstance<AgentTurn.User>().map { it.text })
    }

    @Test
    fun failure_isShown_andTheQuestionIsNotSentAgainAsContext() = runTest(dispatcher) {
        val model = ScriptedAgentModel({ throw AgentUnavailableException(AskFailure.QUOTA) }, { answer("ok") })
        val vm = viewModel(model)

        vm.onSuggestion("q1"); runCurrent()
        assertEquals(AskFailure.QUOTA, assertIs<ChatMessage.Failure>(vm.uiState.value.messages.last()).reason)

        vm.onSuggestion("q2"); runCurrent()
        assertEquals(listOf("q2"), model.seen[1].filterIsInstance<AgentTurn.User>().map { it.text })
    }

    @Test
    fun blankInput_orWithoutKey_sendsNothing() = runTest(dispatcher) {
        val model = ScriptedAgentModel()
        val vm = viewModel(model)

        vm.onInputChange("   ")
        vm.onSend()
        runCurrent()
        assertTrue(vm.uiState.value.messages.isEmpty())

        model.isReady.value = false
        runCurrent()
        vm.onSuggestion("q")
        runCurrent()
        assertTrue(vm.uiState.value.messages.isEmpty())
        assertTrue(model.seen.isEmpty())
    }
}
