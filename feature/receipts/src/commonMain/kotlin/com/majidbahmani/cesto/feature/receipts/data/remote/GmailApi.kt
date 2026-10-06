package com.majidbahmani.cesto.feature.receipts.data.remote

import com.majidbahmani.cesto.feature.receipts.data.remote.dto.MessageDto
import com.majidbahmani.cesto.feature.receipts.data.remote.dto.MessageListResponseDto
import com.majidbahmani.cesto.feature.receipts.data.remote.dto.MessagePartBodyDto

/** The three Gmail REST calls Cesto needs; read-only (`gmail.readonly`). */
interface GmailApi {
    /**
     * Ids of messages matching a Gmail search [query] (same syntax as the Gmail search box),
     * newest first. Pass the previous response's `nextPageToken` as [pageToken] for the next page.
     */
    suspend fun listMessages(query: String, pageToken: String? = null, maxResults: Int = PAGE_SIZE): MessageListResponseDto

    /** Headers, MIME structure and attachment ids of one message (`format=full`). */
    suspend fun getMessage(id: String): MessageDto

    /** Attachment content; `attachmentId` must come from a fresh [getMessage] (it changes per request). */
    suspend fun getAttachment(messageId: String, attachmentId: String): MessagePartBodyDto

    companion object {
        const val BASE_URL = "https://gmail.googleapis.com/gmail/v1/users/me/"
        const val PAGE_SIZE = 100
    }
}
