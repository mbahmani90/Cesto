package com.majidbahmani.cesto.feature.receipts.fake

import com.majidbahmani.cesto.feature.receipts.data.remote.GmailApi
import com.majidbahmani.cesto.feature.receipts.data.remote.dto.MessageDto
import com.majidbahmani.cesto.feature.receipts.data.remote.dto.MessageListResponseDto
import com.majidbahmani.cesto.feature.receipts.data.remote.dto.MessagePartBodyDto
import com.majidbahmani.cesto.feature.receipts.data.remote.dto.MessagePartDto
import com.majidbahmani.cesto.feature.receipts.data.remote.dto.MessageRefDto
import kotlinx.coroutines.delay

/**
 * An in-memory mailbox. Messages are listed newest first in pages of [pageSize], like Gmail.
 * Like real Gmail, every getMessage returns a new attachmentId for the same part.
 */
class FakeGmailApi(private val pageSize: Int = 2, private val requestMillis: Long = 0) : GmailApi {

    /** One email: its PDFs as partId → base64url content (null = unreadable data). */
    data class Mail(val id: String, val receivedAt: Long, val pdfs: Map<String, String?>)

    /** Newest first, as Gmail lists them. */
    val mailbox = mutableListOf<Mail>()
    val queries = mutableListOf<String>()
    val fetchedMessages = mutableListOf<String>()
    var failGetMessage: String? = null
    var failList: Exception? = null
    var failAttachmentOf: String? = null

    /** Requests running at the same time (each takes [requestMillis] of virtual time). */
    var maxInFlight = 0
        private set
    private var inFlight = 0

    private suspend fun <T> request(block: () -> T): T {
        inFlight++
        maxInFlight = maxOf(maxInFlight, inFlight)
        try {
            if (requestMillis > 0) delay(requestMillis)
            return block()
        } finally {
            inFlight--
        }
    }

    private var attachmentRequests = 0

    override suspend fun listMessages(query: String, pageToken: String?, maxResults: Int): MessageListResponseDto {
        failList?.let { throw it }
        queries += query
        val start = pageToken?.toInt() ?: 0
        val page = mailbox.drop(start).take(pageSize)
        val next = (start + pageSize).takeIf { it < mailbox.size }?.toString()
        return MessageListResponseDto(messages = page.map { MessageRefDto(it.id) }, nextPageToken = next)
    }

    override suspend fun getMessage(id: String): MessageDto = request {
        if (id == failGetMessage) throw IllegalStateException("network down")
        fetchedMessages += id
        val mail = mailbox.first { it.id == id }
        attachmentRequests++
        MessageDto(
            id = id,
            internalDate = mail.receivedAt.toString(),
            payload = MessagePartDto(
                partId = "",
                mimeType = "multipart/mixed",
                parts = mail.pdfs.keys.map { partId ->
                    MessagePartDto(
                        partId = partId,
                        mimeType = "application/pdf",
                        filename = "Fatura_$id-$partId.pdf",
                        body = MessagePartBodyDto(attachmentId = "att-$id-$partId-$attachmentRequests", size = 10),
                    )
                },
            ),
        )
    }

    override suspend fun getAttachment(messageId: String, attachmentId: String): MessagePartBodyDto = request {
        if (messageId == failAttachmentOf) throw IllegalStateException("connection reset")
        val mail = mailbox.first { it.id == messageId }
        val partId = attachmentId.removePrefix("att-$messageId-").substringBefore('-')
        MessagePartBodyDto(attachmentId = attachmentId, data = mail.pdfs.getValue(partId))
    }
}
