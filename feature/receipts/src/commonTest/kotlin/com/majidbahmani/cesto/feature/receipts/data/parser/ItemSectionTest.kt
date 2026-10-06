package com.majidbahmani.cesto.feature.receipts.data.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ItemSectionTest {

    @Test
    fun onlyTheLinesBetweenHeaderAndSubtotal() {
        val section = itemSection(PDFBOX_RECEIPT)!!

        assertTrue(section.startsWith("Laticinios/Beb. Veg.:"))
        assertTrue(section.endsWith("0,760 X 1,19 0,90"))
        // Nothing personal from outside the section.
        listOf("NIF", "Cartao cliente", "ATCUD", "TOTAL A PAGAR", "SUBTOTAL").forEach { assertFalse(section.contains(it), it) }
    }

    @Test
    fun withoutDiscount_theSectionEndsAtTotalAPagar() {
        // Most receipts: no SUBTOTAL line, the items go straight to the total.
        val text = "Nro:FS X1/2 14/03/2026 10:15 | NIF:PT123456789\nIVA DESCRICAO VALOR\n(A) PAO 0,35\nTOTAL A PAGAR 0,35\nCartao Cliente 0,35"

        assertEquals("(A) PAO 0,35", itemSection(text))
        assertNull(subtotalCents(text))
    }

    @Test
    fun longNumbersInsideTheSection_areMasked() {
        val text = "IVA DESCRICAO VALOR\n(A) ITEM 123456789012 1,00\nSUBTOTAL 1,00"

        assertEquals("(A) ITEM ######### 1,00", itemSection(text))
    }

    @Test
    fun withoutMarkers_nothingIsSent() {
        assertNull(itemSection("TOTAL A PAGAR 4,52"))
        assertNull(itemSection("IVA DESCRICAO VALOR\nSUBTOTAL 1,00")) // empty section
    }

    @Test
    fun subtotal_inCents() {
        assertEquals(209, subtotalCents(PDFBOX_RECEIPT))
        assertEquals(123_456, subtotalCents("SUBTOTAL 1.234,56"))
        assertNull(subtotalCents("no subtotal"))
    }
}
