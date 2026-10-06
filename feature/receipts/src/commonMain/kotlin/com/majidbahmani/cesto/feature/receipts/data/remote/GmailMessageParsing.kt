package com.majidbahmani.cesto.feature.receipts.data.remote

import com.majidbahmani.cesto.feature.receipts.data.remote.dto.MessageDto
import com.majidbahmani.cesto.feature.receipts.data.remote.dto.MessagePartDto
import kotlin.io.encoding.Base64

/** A PDF attached to a message. [partId] is stable; the attachmentId is not (fetch it fresh). */
data class PdfAttachmentRef(
    val partId: String,
    val attachmentId: String,
    val fileName: String,
    val sizeBytes: Int,
)

/** Header value by name, case-insensitive like MIME (`From`, `Subject`, `Date`). */
fun MessageDto.header(name: String): String? =
    payload?.headers?.firstOrNull { it.name.equals(name, ignoreCase = true) }?.value

/** Gmail's `internalDate` (epoch millis as a string), or null if missing or malformed. */
fun MessageDto.receivedAtMillis(): Long? = internalDate?.toLongOrNull()

/**
 * Every PDF attachment, searched through nested multipart parts. Some mail clients send PDFs as
 * `application/octet-stream`, so the `.pdf` file name counts too.
 */
fun MessageDto.pdfAttachments(): List<PdfAttachmentRef> =
    payload?.allParts().orEmpty().mapNotNull { part ->
        val fileName = part.filename?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val partId = part.partId ?: return@mapNotNull null
        val attachmentId = part.body?.attachmentId ?: return@mapNotNull null
        val isPdf = part.mimeType.equals("application/pdf", ignoreCase = true) ||
            fileName.endsWith(".pdf", ignoreCase = true)
        if (isPdf) PdfAttachmentRef(partId, attachmentId, fileName, part.body?.size ?: 0) else null
    }

private fun MessagePartDto.allParts(): List<MessagePartDto> = listOf(this) + parts.flatMap { it.allParts() }

/** Gmail encodes bodies and attachments as base64url, with or without padding. */
private val gmailBase64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)

fun decodeGmailBase64(data: String): ByteArray = gmailBase64.decode(data)
