package com.majidbahmani.cesto.feature.receipts.data.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.TimeZone

class ContinenteReceiptParserTest {

    private val parser = ContinenteReceiptParser()

    /** 05/10/2026 21:22 in Lisbon (WEST, UTC+1) = 20:22 UTC. */
    private val expected = ParsedReceipt(
        purchasedAtMillis = 1_791_231_720_000,
        totalCents = 188,
        receiptNumber = "FS ABC123/000001",
        atcud = "ABCD1234-000001"
    )

    @Test
    fun pdfBoxText_allFields() {
        assertEquals(expected, parser.parse(PDFBOX_RECEIPT))
    }

    @Test
    fun pdfKitText_sameFields() {
        assertEquals(expected, parser.parse(PDFKIT_RECEIPT))
    }

    @Test
    fun purchaseTime_isPortugueseLocalTime_notThePhones() {
        assertEquals(
            ContinenteReceiptParser(TimeZone.of("Europe/Lisbon")).parse(PDFBOX_RECEIPT),
            ContinenteReceiptParser().parse(PDFBOX_RECEIPT)
        )
        // In winter Portugal is UTC+0: 15/01/2026 10:00 = 10:00 UTC.
        val winter = parser.parse("Nro:FS X1/2 15/01/2026 10:00 |")
        assertEquals(1_768_471_200_000, winter.purchasedAtMillis)
    }

    @Test
    fun total_withThousandsSeparator_andNotSubtotalOrCardLine() {
        val text = "SUBTOTAL 1.300,00\nDesconto 65,44\nTOTAL A PAGAR 1.234,56\nCartao Cliente 1.234,56"
        assertEquals(123_456, parser.parse(text).totalCents)
    }

    @Test
    fun totalOnTheNextLine_stillFound() {
        assertEquals(452, parser.parse("TOTAL A PAGAR\n4,52").totalCents)
    }

    @Test
    fun missingFields_areNull_notErrors() {
        assertEquals(ParsedReceipt(null, null, null, null), parser.parse("not a receipt"))
        assertNull(parser.parse("Nro:FS X1/2 31/02/2026 10:00 |").purchasedAtMillis) // no 31 February
    }
}
