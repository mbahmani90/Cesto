package com.majidbahmani.cesto.feature.receipts.data.remote.dto

import kotlinx.serialization.Serializable

/** `users.messages.get` with `format=full`. */
@Serializable
data class MessageDto(
    val id: String,
    val threadId: String? = null,
    /** Epoch millis as a string (Google sends int64 values as JSON strings). */
    val internalDate: String? = null,
    val snippet: String? = null,
    val payload: MessagePartDto? = null,
)
