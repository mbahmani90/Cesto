package com.majidbahmani.cesto.feature.receipts.fake

import com.majidbahmani.cesto.feature.receipts.data.local.PdfTextExtractor

/** "PDF" bytes are plain text in tests: the text is returned as is; "%BROKEN" throws like a corrupt file. */
class FakePdfTextExtractor : PdfTextExtractor {
    val extracted = mutableListOf<String>()

    override suspend fun extractText(pdf: ByteArray): String {
        val content = pdf.decodeToString()
        if (content.startsWith("%BROKEN")) throw IllegalArgumentException("not a PDF")
        extracted += content
        return content
    }
}
