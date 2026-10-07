package com.majidbahmani.cesto.feature.receipts.presentation.model

import com.majidbahmani.cesto.feature.receipts.domain.model.ReceiptStatus

/** One row of the list, with display-ready values. */
data class ReceiptItemUi(
    val id: Long,
    /** "05/10/2026 21:22": the purchase time on the receipt, or the email time until the text is read. */
    val dateText: String,
    val fileName: String,
    val status: ReceiptStatus,
    /** "4,52 €" once the text is read; the row shows the status until then. */
    val totalText: String? = null
)
