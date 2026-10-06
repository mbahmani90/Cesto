package com.majidbahmani.cesto.feature.receipts.data.remote

import com.majidbahmani.cesto.feature.receipts.data.remote.dto.MessageDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GmailMessageParsingTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val message = json.decodeFromString<MessageDto>(MESSAGE_WITH_PDFS_DOCUMENTED_SHAPE)

    @Test
    fun pdfAttachments_findsNestedAndOctetStreamPdfs_skipsImagesAndBody() {
        assertEquals(
            listOf(
                PdfAttachmentRef(partId = "0.1", attachmentId = "ANGjdJ_b", fileName = "talao-2.PDF", sizeBytes = 2048),
                PdfAttachmentRef(partId = "1", attachmentId = "ANGjdJ_a", fileName = "fatura.pdf", sizeBytes = 40123),
            ),
            message.pdfAttachments(),
        )
    }

    @Test
    fun pdfAttachments_noPayload_isEmpty() {
        assertTrue(MessageDto(id = "x").pdfAttachments().isEmpty())
    }

    @Test
    fun header_isCaseInsensitive() {
        assertEquals("A sua fatura", message.header("subject"))
        assertEquals("Continente <noreply@example.pt>", message.header("FROM"))
        assertNull(message.header("Date"))
    }

    @Test
    fun receivedAtMillis_parsesInt64String() {
        assertEquals(1_788_432_000_000, message.receivedAtMillis())
        assertNull(MessageDto(id = "x", internalDate = "not a number").receivedAtMillis())
    }

    @Test
    fun decodeGmailBase64_acceptsUrlSafeCharsWithAndWithoutPadding() {
        assertEquals("<p>Olá</p>", decodeGmailBase64("PHA-T2zDoTwvcD4").decodeToString())
        assertEquals("%PDF-1.7", decodeGmailBase64("JVBERi0xLjc=").decodeToString())
        assertEquals("%PDF-1.7", decodeGmailBase64("JVBERi0xLjc").decodeToString())
    }
}
