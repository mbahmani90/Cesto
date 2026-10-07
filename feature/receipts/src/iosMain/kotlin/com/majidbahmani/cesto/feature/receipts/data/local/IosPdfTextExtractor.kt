package com.majidbahmani.cesto.feature.receipts.data.local

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.create
import platform.PDFKit.PDFDocument

/** PDFKit (built into iOS), called from Kotlin/Native: no Swift needed. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosPdfTextExtractor(private val defaultDispatcher: CoroutineDispatcher) : PdfTextExtractor {

    override suspend fun extractText(pdf: ByteArray): String = withContext(defaultDispatcher) {
        require(pdf.isNotEmpty()) { "empty PDF" }
        val data = pdf.usePinned { NSData.create(bytes = it.addressOf(0), length = pdf.size.toULong()) }
        // A nil from PDFDocument's initializer surfaces as a NullPointerException in Kotlin/Native.
        val document = try {
            PDFDocument(data = data)
        } catch (e: NullPointerException) {
            null
        }
        checkNotNull(document) { "not a PDF" }
        document.string.orEmpty()
    }
}
