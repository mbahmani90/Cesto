package com.majidbahmani.cesto.feature.receipts.data.remote.dto

import kotlinx.serialization.Serializable

/** A MIME part; multipart messages nest their parts. Attachments have a filename and `body.attachmentId`. */
@Serializable
data class MessagePartDto(
    val partId: String? = null,
    val mimeType: String? = null,
    val filename: String? = null,
    val headers: List<HeaderDto> = emptyList(),
    val body: MessagePartBodyDto? = null,
    val parts: List<MessagePartDto> = emptyList(),
)
