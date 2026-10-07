package com.majidbahmani.cesto.feature.receipts.data.remote

import com.majidbahmani.cesto.feature.receipts.data.remote.dto.MessageDto
import com.majidbahmani.cesto.feature.receipts.data.remote.dto.MessageListResponseDto
import com.majidbahmani.cesto.feature.receipts.data.remote.dto.MessagePartBodyDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.encodeURLPathPart

/**
 * Gmail REST with the shared [HttpClient] from :core (no base URL there: Gmail sets its own).
 * Every request carries a fresh access token from [tokens]; the token never leaves the phone
 * except to Google.
 */
class KtorGmailApi(private val client: HttpClient, private val tokens: GmailTokenProvider) : GmailApi {

    override suspend fun listMessages(query: String, pageToken: String?, maxResults: Int): MessageListResponseDto =
        client.get(GmailApi.BASE_URL + "messages") {
            authorize()
            parameter("q", query)
            parameter("pageToken", pageToken) // null is skipped
            parameter("maxResults", maxResults)
        }.body()

    override suspend fun getMessage(id: String): MessageDto = client.get(GmailApi.BASE_URL + "messages/${id.encodeURLPathPart()}") {
        authorize()
        parameter("format", "full")
    }.body()

    override suspend fun getAttachment(messageId: String, attachmentId: String): MessagePartBodyDto = client.get(
        GmailApi.BASE_URL + "messages/${messageId.encodeURLPathPart()}/attachments/${attachmentId.encodeURLPathPart()}"
    ) {
        authorize()
    }.body()

    private suspend fun HttpRequestBuilder.authorize() {
        bearerAuth(tokens.accessToken())
    }
}
