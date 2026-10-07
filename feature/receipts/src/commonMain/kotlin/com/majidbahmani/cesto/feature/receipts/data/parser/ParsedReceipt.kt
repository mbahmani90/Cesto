package com.majidbahmani.cesto.feature.receipts.data.parser

/** The fixed fields of a receipt's text; a field the text doesn't have stays null. */
data class ParsedReceipt(val purchasedAtMillis: Long?, val totalCents: Long?, val receiptNumber: String?, val atcud: String?)
