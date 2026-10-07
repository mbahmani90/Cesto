package com.majidbahmani.cesto.feature.chat.domain.usecase

import com.majidbahmani.cesto.feature.chat.domain.model.AgentTurn
import com.majidbahmani.cesto.feature.chat.domain.model.AgentUnavailableException
import com.majidbahmani.cesto.feature.chat.domain.model.AskFailure
import com.majidbahmani.cesto.feature.chat.domain.model.AskResult
import com.majidbahmani.cesto.feature.chat.fake.FakeReceiptTools
import com.majidbahmani.cesto.feature.chat.fake.ScriptedAgentModel
import com.majidbahmani.cesto.feature.chat.fake.answer
import com.majidbahmani.cesto.feature.chat.fake.useTools
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class AskQuestionUseCaseTest {

    private val tools = FakeReceiptTools(receiptIds = mapOf("sumQuantity" to listOf(3L, 1L), "listReceipts" to listOf(1L, 7L)))

    @Test
    fun runsTheToolsTheModelAsksFor_untilItAnswers() = runTest {
        val model = ScriptedAgentModel({ useTools("findProducts") }, { useTools("sumQuantity") }, { answer("24 yogurts") })
        val steps = mutableListOf<String>()

        val result = AskQuestionUseCase(model, tools)(emptyList(), "  How many yogurts?  ") { steps += it.name }

        val answered = assertIs<AskResult.Answered>(result)
        assertEquals("24 yogurts", answered.text)
        assertEquals(listOf("findProducts", "sumQuantity"), tools.calls.map { it.name })
        assertEquals(steps, tools.calls.map { it.name })
        // Round 3 saw: question, model turn, results, model turn, results.
        val last = model.seen.last()
        assertEquals(AgentTurn.User("How many yogurts?"), last.first())
        assertEquals(5, last.size)
        assertIs<AgentTurn.ToolResults>(last[2])
        assertEquals(6, answered.conversation.size) // + the answer, for follow-ups
    }

    @Test
    fun citesEachReceiptOnce_inTheOrderFound() = runTest {
        val model = ScriptedAgentModel({ useTools("sumQuantity", "listReceipts") }, { answer("done") })

        val result = assertIs<AskResult.Answered>(AskQuestionUseCase(model, tools)(emptyList(), "q"))

        assertEquals(listOf(3L, 1L, 7L), result.receiptIds)
    }

    @Test
    fun stopsAfterMaxRounds() = runTest {
        val model = ScriptedAgentModel(*Array(5) { { useTools("findProducts") } })

        val result = AskQuestionUseCase(model, tools, maxRounds = 5)(emptyList(), "q")

        assertEquals(AskResult.Failed(AskFailure.TOO_MANY_STEPS), result)
        assertEquals(5, tools.calls.size)
    }

    @Test
    fun modelUnavailable_becomesItsReason() = runTest {
        val model = ScriptedAgentModel({ throw AgentUnavailableException(AskFailure.QUOTA) })

        assertEquals(AskResult.Failed(AskFailure.QUOTA), AskQuestionUseCase(model, tools)(emptyList(), "q"))
    }

    @Test
    fun unexpectedError_becomesFailed() = runTest {
        val model = ScriptedAgentModel({ throw IllegalStateException("bug") })

        assertEquals(AskResult.Failed(AskFailure.FAILED), AskQuestionUseCase(model, tools)(emptyList(), "q"))
    }

    @Test
    fun followUp_sendsOnlyTheLastQuestionsAsContext() = runTest {
        val history = (1..5).flatMap { listOf(AgentTurn.User("q$it"), answer("a$it").turn) }
        val model = ScriptedAgentModel({ answer("a6") })

        AskQuestionUseCase(model, tools)(history, "q6")

        val sent = model.seen.single().filterIsInstance<AgentTurn.User>().map { it.text }
        assertEquals(listOf("q3", "q4", "q5", "q6"), sent)
    }
}
