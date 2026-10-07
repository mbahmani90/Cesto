package com.majidbahmani.cesto.feature.receipts.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Part content: small bodies come inline in [data]; attachments only have an [attachmentId] and are
 * fetched with `users.messages.attachments.get`, which returns this type with [data] set.
 * [data] is base64url encoded.
 */
@Serializable
data class MessagePartBodyDto(val attachmentId: String? = null, val size: Int = 0, val data: String? = null)
