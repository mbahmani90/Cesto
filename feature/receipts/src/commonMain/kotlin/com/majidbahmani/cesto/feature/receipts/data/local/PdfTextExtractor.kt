package com.majidbahmani.cesto.feature.receipts.data.local

/** PDF → plain text on the phone (PdfBox-Android / PDFKit). CPU work: implementations switch to Default. */
interface PdfTextExtractor {
    /** The text layer of every page; empty for a scanned PDF without text (needs OCR, not supported yet). */
    suspend fun extractText(pdf: ByteArray): String
}
