package com.majidbahmani.cesto.feature.receipts.presentation.mapper

import com.majidbahmani.cesto.feature.receipts.domain.model.Receipt
import com.majidbahmani.cesto.feature.receipts.domain.model.ReceiptStatus
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class ReceiptUiMapperTest {

    // 2026-10-05T20:22:00Z
    private val receipt = Receipt(id = 1, fileName = "f.pdf", receivedAtMillis = 1_791_231_720_000, status = ReceiptStatus.DOWNLOADED)

    @Test
    fun date_isDayFirst_inTheGivenTimeZone() {
        assertEquals("05/10/2026 21:22", receipt.toUi(TimeZone.of("Europe/Lisbon")).dateText) // WEST, UTC+1
        assertEquals("05/10/2026 20:22", receipt.toUi(TimeZone.UTC).dateText)
    }
}
