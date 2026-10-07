package com.majidbahmani.cesto.feature.receipts.data.remote.dto

import kotlinx.serialization.Serializable

/** `users.messages.list`. `messages` is missing (not empty) when nothing matches. */
@Serializable
data class MessageListResponseDto(
    val messages: List<MessageRefDto> = emptyList(),
    val nextPageToken: String? = null,
    val resultSizeEstimate: Int? = null
)
