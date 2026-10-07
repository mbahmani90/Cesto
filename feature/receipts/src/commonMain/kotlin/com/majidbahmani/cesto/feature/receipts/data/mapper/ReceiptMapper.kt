package com.majidbahmani.cesto.feature.receipts.data.mapper

import com.majidbahmani.cesto.database.Receipt as ReceiptRow
import com.majidbahmani.cesto.feature.receipts.domain.model.Receipt
import com.majidbahmani.cesto.feature.receipts.domain.model.ReceiptStatus

fun ReceiptRow.toDomain(): Receipt = Receipt(
    id = id,
    fileName = file_name,
    receivedAtMillis = received_at,
    // Unknown values (an older or newer app version) show as failed instead of crashing.
    status = ReceiptStatus.entries.firstOrNull { it.name == status } ?: ReceiptStatus.FAILED,
    purchasedAtMillis = purchased_at,
    totalCents = total_cents
)
