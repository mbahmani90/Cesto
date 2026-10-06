package com.majidbahmani.cesto.feature.receipts.data.remote

import com.majidbahmani.cesto.core.network.createHttpClient
import com.majidbahmani.cesto.feature.receipts.data.remote.dto.MessageRefDto
import com.majidbahmani.cesto.gmailauth.GmailAuthError
import com.majidbahmani.cesto.gmailauth.GmailAuthorizer
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KtorGmailApiTest {

    private class FakeAuthorizer(private val error: GmailAuthError? = null) : GmailAuthorizer {
        val interactiveFlags = mutableListOf<Boolean>()

        override fun authorize(interactive: Boolean, onSuccess: (String) -> Unit, onFailure: (GmailAuthError) -> Unit) {
            interactiveFlags += interactive
            if (error == null) onSuccess("test-token") else onFailure(error)
        }
    }

    private val requests = mutableListOf<HttpRequestData>()

    private fun api(
        authorizer: GmailAuthorizer = FakeAuthorizer(),
        handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = KtorGmailApi(
        client = createHttpClient(MockEngine { request -> requests += request; handler(request) }),
        tokens = GmailTokenProvider(authorizer, currentTimeMillis = { 0L }),
    )

    private fun MockRequestHandleScope.respondJson(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun listMessages_sendsQueryPagingAndToken() = runTest {
        api { respondJson(MESSAGE_LIST_DOCUMENTED_SHAPE) }.listMessages("from:continente has:attachment", pageToken = "page-2", maxResults = 50)

        val request = requests.single()
        assertEquals("https://gmail.googleapis.com/gmail/v1/users/me/messages", request.url.toString().substringBefore('?'))
        assertEquals("from:continente has:attachment", request.url.parameters["q"])
        assertEquals("page-2", request.url.parameters["pageToken"])
        assertEquals("50", request.url.parameters["maxResults"])
        assertEquals("Bearer test-token", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun listMessages_firstPage_hasNoPageToken() = runTest {
        api { respondJson(MESSAGE_LIST_EMPTY) }.listMessages("q")

        assertNull(requests.single().url.parameters["pageToken"])
    }

    @Test
    fun listMessages_parsesIdsAndNextPage() = runTest {
        val response = api { respondJson(MESSAGE_LIST_DOCUMENTED_SHAPE) }.listMessages("q")

        assertEquals(listOf(MessageRefDto("m2", "t2"), MessageRefDto("m1", "t1")), response.messages)
        assertEquals("page-2", response.nextPageToken)
    }

    @Test
    fun listMessages_noMatches_isEmptyList() = runTest {
        val response = api { respondJson(MESSAGE_LIST_EMPTY) }.listMessages("q")

        assertTrue(response.messages.isEmpty())
        assertNull(response.nextPageToken)
    }

    @Test
    fun getMessage_requestsFullFormat_andParsesNestedParts() = runTest {
        val message = api { respondJson(MESSAGE_WITH_PDFS_DOCUMENTED_SHAPE) }.getMessage("18c2a0f1")

        val request = requests.single()
        assertEquals("https://gmail.googleapis.com/gmail/v1/users/me/messages/18c2a0f1", request.url.toString().substringBefore('?'))
        assertEquals("full", request.url.parameters["format"])
        assertEquals("1788432000000", message.internalDate)
        assertEquals("multipart/alternative", message.payload?.parts?.first()?.mimeType)
    }

    @Test
    fun getAttachment_usesMessageAndAttachmentIds() = runTest {
        val body = api { respondJson(ATTACHMENT_DOCUMENTED_SHAPE) }.getAttachment("18c2a0f1", "ANGjdJ_a")

        assertEquals(
            "https://gmail.googleapis.com/gmail/v1/users/me/messages/18c2a0f1/attachments/ANGjdJ_a",
            requests.single().url.toString(),
        )
        assertEquals("%PDF-1.7", decodeGmailBase64(body.data!!).decodeToString())
    }

    @Test
    fun notAuthorized_failsWithoutRequest_andNeverShowsUi() = runTest {
        val authorizer = FakeAuthorizer(GmailAuthError.NOT_GRANTED)

        val error = assertFailsWith<GmailNotAuthorizedException> {
            api(authorizer) { respondJson(MESSAGE_LIST_EMPTY) }.listMessages("q")
        }
        assertEquals(GmailAuthError.NOT_GRANTED, error.reason)
        assertTrue(requests.isEmpty())
        assertEquals(listOf(false), authorizer.interactiveFlags)
    }

    @Test
    fun expiredOrRevokedToken_throws401() = runTest {
        assertFailsWith<ClientRequestException> {
            api { respondJson("""{"error":{"code":401}}""", HttpStatusCode.Unauthorized) }.listMessages("q")
        }
    }
}
