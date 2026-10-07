package com.majidbahmani.cesto.feature.receipts.presentation.mapper

import com.majidbahmani.cesto.feature.receipts.domain.model.Receipt
import com.majidbahmani.cesto.feature.receipts.presentation.model.ReceiptItemUi
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

/** Day first, as on Portuguese receipts. */
private val dateFormat = LocalDateTime.Format {
    day()
    char('/')
    monthNumber()
    char('/')
    year()
    char(' ')
    hour()
    char(':')
    minute()
}

fun Receipt.toUi(timeZone: TimeZone): ReceiptItemUi = ReceiptItemUi(
    id = id,
    dateText = dateFormat.format(Instant.fromEpochMilliseconds(purchasedAtMillis ?: receivedAtMillis).toLocalDateTime(timeZone)),
    fileName = fileName,
    status = status,
    totalText = totalCents?.let(::formatEuros)
)

/** Portuguese style, as on the receipt: 4,52 € · 1.234,56 €. */
fun formatEuros(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val absolute = kotlin.math.abs(cents)
    val euros = (absolute / 100).toString().reversed().chunked(3).joinToString(".").reversed()
    val rest = (absolute % 100).toString().padStart(2, '0')
    return "$sign$euros,$rest €"
}
