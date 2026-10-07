package com.majidbahmani.cesto.feature.receipts.domain.model

/** A receipt PDF found in Gmail. Items come with extraction later. */
data class Receipt(
    val id: Long,
    val fileName: String,
    /** When the email arrived (epoch millis); for Continente this is right after the purchase. */
    val receivedAtMillis: Long,
    val status: ReceiptStatus,
    /** Purchase date and time printed on the receipt; null until its text is read. */
    val purchasedAtMillis: Long? = null,
    /** "TOTAL A PAGAR" in cents; null until its text is read. */
    val totalCents: Long? = null
)
