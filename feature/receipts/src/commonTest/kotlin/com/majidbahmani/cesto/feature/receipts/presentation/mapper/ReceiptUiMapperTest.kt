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
    fun purchaseTime_isShownInsteadOfTheEmailTime_andTheTotalInEuros() {
        val read = receipt.copy(purchasedAtMillis = receipt.receivedAtMillis - 60 * 60 * 1000, totalCents = 452)

        val ui = read.toUi(TimeZone.UTC)

        assertEquals("05/10/2026 19:22", ui.dateText)
        assertEquals("4,52 €", ui.totalText)
        assertEquals(null, receipt.toUi(TimeZone.UTC).totalText)
    }

    @Test
    fun euros_portugueseStyle() {
        assertEquals("0,05 €", formatEuros(5))
        assertEquals("4,52 €", formatEuros(452))
        assertEquals("1.234,56 €", formatEuros(123_456))
        assertEquals("1.000.000,00 €", formatEuros(100_000_000))
        assertEquals("-0,84 €", formatEuros(-84))
    }

    @Test
    fun date_isDayFirst_inTheGivenTimeZone() {
        assertEquals("05/10/2026 21:22", receipt.toUi(TimeZone.of("Europe/Lisbon")).dateText) // WEST, UTC+1
        assertEquals("05/10/2026 20:22", receipt.toUi(TimeZone.UTC).dateText)
    }
}
