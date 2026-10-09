package com.majidbahmani.cesto.account

import com.majidbahmani.cesto.core.network.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class AccountRepositoryTest {

    private class FakeSessionStore : SessionStore {
        override val sessions = MutableStateFlow(Sessions())

        override suspend fun save(sessions: Sessions) {
            this.sessions.value = sessions
        }
    }

    private val store = FakeSessionStore()
    private var now = 0L
    private var refreshCalls = 0

    /** signInWithIdp: the Google token is the user id. securetoken: answers [refreshAnswer]. */
    private var refreshAnswer: Pair<HttpStatusCode, String> =
        HttpStatusCode.OK to """{"id_token":"id-new","refresh_token":"refresh-new","expires_in":"3600","user_id":"ana"}"""

    private val repository = AccountRepositoryImpl(
        api = IdentityPlatformApi(
            createHttpClient(
                MockEngine { request ->
                    val json = headersOf(HttpHeaders.ContentType, "application/json")
                    if (request.url.host == "securetoken.googleapis.com") {
                        refreshCalls++
                        respond(refreshAnswer.second, refreshAnswer.first, json)
                    } else {
                        val body = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
                        val user = Regex("id_token=(\\w+)").find(body)!!.groupValues[1]
                        respond(
                            """{"localId":"$user","email":"$user@example.com","idToken":"id-$user","refreshToken":"refresh-$user","expiresIn":"3600"}""",
                            HttpStatusCode.OK,
                            json
                        )
                    }
                }
            ),
            apiKey = "AIza-test"
        ),
        store = store,
        currentTimeMillis = { now }
    )

    @Test
    fun signIn_addsTheAccountAndMakesItActive() = runTest {
        repository.signIn("ana")
        val bruno = repository.signIn("bruno")

        assertEquals(listOf("ana", "bruno"), repository.accounts.first().map { it.localId })
        assertEquals(bruno, repository.activeAccount.first())
        assertEquals("refresh-bruno", store.sessions.value.accounts.last().refreshToken)
    }

    @Test
    fun signInAgain_updatesTheSameAccount() = runTest {
        repository.signIn("ana")
        repository.signIn("ana")

        assertEquals(1, repository.accounts.first().size)
    }

    @Test
    fun switchTo_changesTheActiveAccount() = runTest {
        repository.signIn("ana")
        repository.signIn("bruno")

        repository.switchTo("ana")

        assertEquals("ana", repository.activeAccount.first()?.localId)
        assertFailsWith<IllegalArgumentException> { repository.switchTo("nobody") }
    }

    @Test
    fun validIdToken_usesTheCachedTokenUntil5MinutesBeforeExpiry() = runTest {
        repository.signIn("ana")

        now = 54 * 60 * 1000L
        assertEquals("id-ana", repository.validIdToken())
        assertEquals(0, refreshCalls)

        now = 56 * 60 * 1000L
        assertEquals("id-new", repository.validIdToken())
        assertEquals(1, refreshCalls)
        assertEquals("refresh-new", store.sessions.value.accounts.single().refreshToken)
    }

    @Test
    fun validIdToken_afterRestart_refreshes() = runTest {
        store.sessions.value = Sessions(
            listOf(Sessions.StoredAccount(Account("ana", "ana@example.com"), "refresh-ana")),
            activeLocalId = "ana"
        )

        assertEquals("id-new", repository.validIdToken())
    }

    @Test
    fun validIdToken_sessionEnded_removesTheAccount() = runTest {
        repository.signIn("ana")
        now = 2 * 60 * 60 * 1000L
        refreshAnswer = HttpStatusCode.BadRequest to """{"error":{"code":400,"message":"TOKEN_EXPIRED"}}"""

        assertFailsWith<NotSignedInException> { repository.validIdToken() }
        assertNull(repository.activeAccount.first())
    }

    @Test
    fun validIdToken_otherErrors_keepTheAccount() = runTest {
        repository.signIn("ana")
        now = 2 * 60 * 60 * 1000L
        refreshAnswer = HttpStatusCode.TooManyRequests to """{"error":{"code":429,"message":"QUOTA_EXCEEDED"}}"""

        assertFailsWith<IdentityPlatformException> { repository.validIdToken() }
        assertEquals("ana", repository.activeAccount.first()?.localId)
    }

    @Test
    fun noAccount_throwsNotSignedIn() = runTest {
        assertFailsWith<NotSignedInException> { repository.validIdToken() }
    }

    @Test
    fun signOut_activatesAnotherAccount() = runTest {
        repository.signIn("ana")
        repository.signIn("bruno")

        repository.signOut("bruno")

        assertEquals("ana", repository.activeAccount.first()?.localId)
        repository.signOut("ana")
        assertNull(repository.activeAccount.first())
    }
}
