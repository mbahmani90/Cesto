package com.majidbahmani.cesto.feature.chat.domain.usecase

import com.majidbahmani.cesto.feature.chat.domain.model.AgentModel
import com.majidbahmani.cesto.feature.chat.domain.model.AgentTurn
import com.majidbahmani.cesto.feature.chat.domain.model.AgentUnavailableException
import com.majidbahmani.cesto.feature.chat.domain.model.AskFailure
import com.majidbahmani.cesto.feature.chat.domain.model.AskResult
import com.majidbahmani.cesto.feature.chat.domain.model.ModelReply
import com.majidbahmani.cesto.feature.chat.domain.model.ReceiptTools
import com.majidbahmani.cesto.feature.chat.domain.model.ToolCall
import kotlinx.coroutines.CancellationException

/**
 * The agent loop: the model picks tools, the app runs them on the phone and sends back only their small
 * results, until the model answers with text. Numbers come from SQL, never from the model's arithmetic.
 */
class AskQuestionUseCase(
    private val model: AgentModel,
    private val tools: ReceiptTools,
    private val maxRounds: Int = MAX_ROUNDS,
) {
    /**
     * @param previous the conversation so far (follow-up questions keep their context).
     * @param onToolCall progress for the UI ("Searching your products…").
     */
    suspend operator fun invoke(
        previous: List<AgentTurn>,
        question: String,
        onToolCall: (ToolCall) -> Unit = {},
    ): AskResult {
        val conversation = (previous.lastQuestions(MAX_PREVIOUS_QUESTIONS) + AgentTurn.User(question.trim())).toMutableList()
        val receiptIds = linkedSetOf<Long>()
        return try {
            repeat(maxRounds) {
                when (val reply = model.reply(conversation)) {
                    is ModelReply.Answer -> {
                        conversation += reply.turn
                        return AskResult.Answered(reply.text, receiptIds.toList(), conversation)
                    }
                    is ModelReply.UseTools -> {
                        conversation += reply.turn
                        val results = reply.calls.map { call ->
                            onToolCall(call)
                            tools.run(call)
                        }
                        results.forEach { receiptIds += it.receiptIds }
                        conversation += AgentTurn.ToolResults(results)
                    }
                }
            }
            AskResult.Failed(AskFailure.TOO_MANY_STEPS)
        } catch (e: CancellationException) {
            throw e
        } catch (e: AgentUnavailableException) {
            AskResult.Failed(e.reason)
        } catch (e: Exception) {
            AskResult.Failed(AskFailure.FAILED)
        }
    }

    /** Follow-ups need some context, but every round resends it all: keep only the last questions and their steps. */
    private fun List<AgentTurn>.lastQuestions(count: Int): List<AgentTurn> {
        val starts = indices.filter { this[it] is AgentTurn.User }
        return if (starts.size <= count) this else subList(starts[starts.size - count], size)
    }

    companion object {
        /** Safety limit: a normal question needs 2–3 rounds. */
        const val MAX_ROUNDS = 5

        /** Earlier questions (with their answers) sent along with a new one. */
        const val MAX_PREVIOUS_QUESTIONS = 3
    }
}
