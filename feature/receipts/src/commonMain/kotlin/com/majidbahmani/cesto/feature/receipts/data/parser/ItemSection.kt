package com.majidbahmani.cesto.feature.receipts.data.parser

/**
 * Only the lines between the column header ("IVA DESCRICAO VALOR") and the first total go to Gemini:
 * "SUBTOTAL" (receipts with a discount) or else "TOTAL A PAGAR". That's
 * products and prices. The NIF, card number, card balance and coupons are outside this section.
 * Null when the markers aren't there: then nothing is sent.
 */
fun itemSection(text: String): String? {
    val lines = text.lines()
    val start = lines.indexOfFirst { it.contains("DESCRICAO") }
    val end = lines.indexOfFirst { line -> TOTAL_LINES.any { line.trimStart().startsWith(it) } }
    if (start < 0 || end <= start + 1) return null
    return lines.subList(start + 1, end)
        .joinToString("\n")
        .replace(LONG_NUMBER, "#########") // second safeguard: no card or ID numbers, even if the layout changes
        .trim()
        .takeIf { it.isNotEmpty() }
}

/** "SUBTOTAL 5,36" in cents (only on receipts with a discount): the item lines must add up to it. */
fun subtotalCents(text: String): Long? =
    SUBTOTAL.find(text)?.groupValues?.get(1)?.replace(".", "")?.replace(",", "")?.toLongOrNull()

private val TOTAL_LINES = listOf("SUBTOTAL", "TOTAL A PAGAR")
private val LONG_NUMBER = Regex("""\d{9,}""")
private val SUBTOTAL = Regex("""SUBTOTAL\s+(\d{1,3}(?:\.\d{3})*,\d{2})""")
