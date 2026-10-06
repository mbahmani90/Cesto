package com.majidbahmani.cesto.feature.receipts.presentation.model

import com.majidbahmani.cesto.feature.receipts.domain.model.ReceiptStatus

/** One row of the list, with display-ready values. */
data class ReceiptItemUi(
    val id: Long,
    /** "05/10/2026 21:22" in the phone's time zone. */
    val dateText: String,
    val fileName: String,
    val status: ReceiptStatus,
)
