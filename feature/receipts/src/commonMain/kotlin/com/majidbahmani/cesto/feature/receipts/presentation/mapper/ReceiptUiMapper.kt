package com.majidbahmani.cesto.feature.receipts.presentation.mapper

import com.majidbahmani.cesto.feature.receipts.domain.model.Receipt
import com.majidbahmani.cesto.feature.receipts.presentation.model.ReceiptItemUi
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Day first, as on Portuguese receipts. */
private val dateFormat = LocalDateTime.Format {
    day(); char('/'); monthNumber(); char('/'); year()
    char(' ')
    hour(); char(':'); minute()
}

fun Receipt.toUi(timeZone: TimeZone): ReceiptItemUi = ReceiptItemUi(
    id = id,
    dateText = dateFormat.format(Instant.fromEpochMilliseconds(receivedAtMillis).toLocalDateTime(timeZone)),
    fileName = fileName,
    status = status,
)
