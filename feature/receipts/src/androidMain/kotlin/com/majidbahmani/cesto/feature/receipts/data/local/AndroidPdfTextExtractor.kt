package com.majidbahmani.cesto.feature.receipts.data.local

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** PdfBox-Android: lines in reading order, each price on the same line as its item. */
class AndroidPdfTextExtractor(
    context: Context,
    private val defaultDispatcher: CoroutineDispatcher,
) : PdfTextExtractor {

    init {
        PDFBoxResourceLoader.init(context.applicationContext) // fonts and glyph lists; cheap after the first call
    }

    override suspend fun extractText(pdf: ByteArray): String = withContext(defaultDispatcher) {
        PDDocument.load(pdf).use { document -> PDFTextStripper().getText(document) }
    }
}
