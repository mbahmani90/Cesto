package com.majidbahmani.cesto.feature.receipts.data.local

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

class IosPdfTextExtractorTest {

    /** A minimal one-page PDF with [lines] as real text (Helvetica), xref offsets computed. */
    private fun pdfWithText(vararg lines: String): ByteArray {
        val content = buildString {
            append("BT /F1 12 Tf 20 180 Td 14 TL\n")
            lines.forEach { append("(").append(it).append(") '\n") }
            append("ET")
        }
        val objects = listOf(
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 200] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>",
            "<< /Length ${content.length} >>\nstream\n$content\nendstream",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
        )
        val pdf = StringBuilder("%PDF-1.4\n")
        val offsets = objects.mapIndexed { index, body ->
            pdf.length.also { pdf.append("${index + 1} 0 obj\n$body\nendobj\n") }
        }
        val xref = pdf.length
        pdf.append("xref\n0 ${objects.size + 1}\n0000000000 65535 f \n")
        offsets.forEach { pdf.append(it.toString().padStart(10, '0')).append(" 00000 n \n") }
        pdf.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return pdf.toString().encodeToByteArray()
    }

    @Test
    fun extractsTheTextLayer() = runTest {
        val extractor = IosPdfTextExtractor(StandardTestDispatcher(testScheduler))

        val text = extractor.extractText(pdfWithText("Nro:FS ABC123/000001 05/10/2026 21:22", "TOTAL A PAGAR 4,52"))

        assertContains(text, "TOTAL A PAGAR 4,52")
        assertContains(text, "Nro:FS ABC123/000001 05/10/2026 21:22")
    }

    @Test
    fun notAPdf_throws() = runTest {
        val extractor = IosPdfTextExtractor(StandardTestDispatcher(testScheduler))

        assertFailsWith<IllegalStateException> { extractor.extractText("hello".encodeToByteArray()) }
    }
}
