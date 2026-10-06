package com.majidbahmani.cesto.feature.chat.presentation.model

import com.majidbahmani.cesto.feature.chat.domain.model.AskFailure

/** One bubble on the screen; [id] keeps list items stable. */
sealed interface ChatMessage {
    val id: Long

    data class Question(override val id: Long, val text: String) : ChatMessage

    /** [receiptCount]: how many receipts the numbers came from (0 when not counted). */
    data class Answer(override val id: Long, val text: String, val receiptCount: Int) : ChatMessage

    data class Failure(override val id: Long, val reason: AskFailure) : ChatMessage
}

/** What the agent is doing, shown while waiting. */
enum class ThinkingStep { THINKING, FINDING_PRODUCTS, COUNTING, ADDING_SPENDING, RANKING, LISTING_RECEIPTS }
