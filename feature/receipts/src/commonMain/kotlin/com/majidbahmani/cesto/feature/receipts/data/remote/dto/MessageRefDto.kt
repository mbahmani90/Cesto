package com.majidbahmani.cesto.feature.receipts.data.remote.dto

import kotlinx.serialization.Serializable

/** A search hit: only the ids; details come from `messages.get`. */
@Serializable
data class MessageRefDto(val id: String, val threadId: String? = null)
