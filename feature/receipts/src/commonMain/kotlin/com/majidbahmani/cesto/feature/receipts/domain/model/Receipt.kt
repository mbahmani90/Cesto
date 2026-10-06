package com.majidbahmani.cesto.feature.receipts.domain.model

/** A receipt PDF found in Gmail. Purchase date, total and items come with extraction later. */
data class Receipt(
    val id: Long,
    val fileName: String,
    /** When the email arrived (epoch millis); for Continente this is right after the purchase. */
    val receivedAtMillis: Long,
    val status: ReceiptStatus,
)
