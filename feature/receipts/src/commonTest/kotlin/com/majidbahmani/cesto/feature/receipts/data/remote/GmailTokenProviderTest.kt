package com.majidbahmani.cesto.feature.receipts.data.remote

import com.majidbahmani.cesto.gmailauth.GmailAuthError
import com.majidbahmani.cesto.gmailauth.GmailAuthorizer
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GmailTokenProviderTest {

    /** Answers asynchronously (like the Google SDKs) with token-1, token-2, …; counts calls. */
    private class CountingAuthorizer(private val scope: kotlinx.coroutines.CoroutineScope) : GmailAuthorizer {
        var calls = 0
        var error: GmailAuthError? = null

        override fun authorize(interactive: Boolean, onSuccess: (String) -> Unit, onFailure: (GmailAuthError) -> Unit) {
            val call = ++calls
            scope.launch {
                delay(10)
                error?.let(onFailure) ?: onSuccess("token-$call")
            }
        }
    }

    private var now = 0L

    @Test
    fun tokenIsReused_untilTheCacheExpires() = runTest {
        val authorizer = CountingAuthorizer(backgroundScope)
        val provider = GmailTokenProvider(authorizer, currentTimeMillis = { now }, cacheMillis = 1_000)

        assertEquals("token-1", provider.accessToken())
        now = 999
        assertEquals("token-1", provider.accessToken())
        now = 1_000
        assertEquals("token-2", provider.accessToken())
        assertEquals(2, authorizer.calls)
    }

    @Test
    fun parallelRequests_shareOneSdkCall() = runTest {
        val authorizer = CountingAuthorizer(backgroundScope)
        val provider = GmailTokenProvider(authorizer, currentTimeMillis = { now })

        val tokens = List(4) { async { provider.accessToken() } }.awaitAll()

        assertEquals(List(4) { "token-1" }, tokens)
        assertEquals(1, authorizer.calls)
    }

    @Test
    fun deniedAccess_throws_andIsAskedAgainNextTime() = runTest {
        val authorizer = CountingAuthorizer(backgroundScope).apply { error = GmailAuthError.NOT_GRANTED }
        val provider = GmailTokenProvider(authorizer, currentTimeMillis = { now })

        assertFailsWith<GmailNotAuthorizedException> { provider.accessToken() }
        authorizer.error = null
        assertEquals("token-2", provider.accessToken())
    }
}
