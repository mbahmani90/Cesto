package com.majidbahmani.cesto.feature.receipts.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class HeaderDto(val name: String, val value: String)
