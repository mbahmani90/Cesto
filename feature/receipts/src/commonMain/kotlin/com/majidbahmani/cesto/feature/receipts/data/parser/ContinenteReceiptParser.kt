package com.majidbahmani.cesto.feature.receipts.data.parser

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Reads the fixed lines of a Continente "Fatura Simplificada". The extractors order the item lines
 * differently (PDFKit splits names and prices), but these lines come out the same on both:
 *
 *     Nro:FS ABC123/000001 05/10/2026 21:22 | NIF:...
 *     TOTAL A PAGAR 4,52
 *     ATCUD:ABCD1234-000001
 *
 * Items need the LLM later. Times on the receipt are Portuguese local time, wherever the phone is.
 */
class ContinenteReceiptParser(
    private val storeTimeZone: TimeZone = TimeZone.of("Europe/Lisbon"),
) {
    fun parse(text: String): ParsedReceipt {
        val header = HEADER.find(text)
        return ParsedReceipt(
            purchasedAtMillis = header?.let { purchaseTime(it.groupValues[2], it.groupValues[3]) },
            totalCents = TOTAL.find(text)?.groupValues?.get(1)?.let(::parseEuroCents),
            receiptNumber = header?.groupValues?.get(1)?.replace(WHITESPACE, " "),
            atcud = ATCUD.find(text)?.groupValues?.get(1),
        )
    }

    /** "05/10/2026" + "21:22" → epoch millis; null if it isn't a real date. */
    private fun purchaseTime(date: String, time: String): Long? {
        val (day, month, year) = date.split('/').map { it.toInt() }
        val (hour, minute) = time.split(':').map { it.toInt() }
        return runCatching { LocalDateTime(year, month, day, hour, minute).toInstant(storeTimeZone).toEpochMilliseconds() }
            .getOrNull()
    }

    private companion object {
        val WHITESPACE = Regex("""\s+""")

        /** Receipt number (series + number), then the date and time. */
        val HEADER = Regex("""Nro:\s*([A-Z]{2}\s+\S+)\s+(\d{2}/\d{2}/\d{4})\s+(\d{2}:\d{2})""")

        /** Portuguese amounts: thousands with ".", decimals with "," (1.234,56). */
        val TOTAL = Regex("""TOTAL A PAGAR\s+(\d{1,3}(?:\.\d{3})*,\d{2})""")

        val ATCUD = Regex("""ATCUD:\s*([A-Z0-9]+-\d+)""")

        fun parseEuroCents(amount: String): Long = amount.replace(".", "").replace(",", "").toLong()
    }
}
