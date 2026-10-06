package com.majidbahmani.cesto.feature.onboarding.data.repository

import com.majidbahmani.cesto.feature.onboarding.domain.model.GmailConnectionResult
import com.majidbahmani.cesto.gmailauth.GmailAuthError
import com.majidbahmani.cesto.gmailauth.GmailAuthorizer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GmailConnectionRepositoryImplTest {

    /** Answers with a token, or with [error] if set; records the `interactive` flag of each call. */
    private class FakeAuthorizer(private val error: GmailAuthError? = null) : GmailAuthorizer {
        val requests = mutableListOf<Boolean>()

        override fun authorize(interactive: Boolean, onSuccess: (String) -> Unit, onFailure: (GmailAuthError) -> Unit) {
            requests += interactive
            if (error == null) onSuccess("token") else onFailure(error)
        }
    }

    @Test
    fun isConnected_neverShowsUi() = runTest {
        val authorizer = FakeAuthorizer()

        assertTrue(GmailConnectionRepositoryImpl(authorizer).isConnected())
        assertEquals(listOf(false), authorizer.requests)
    }

    @Test
    fun isConnected_falseWhenNotGranted() = runTest {
        assertFalse(GmailConnectionRepositoryImpl(FakeAuthorizer(GmailAuthError.NOT_GRANTED)).isConnected())
    }

    @Test
    fun connect_isInteractive() = runTest {
        val authorizer = FakeAuthorizer()

        assertEquals(GmailConnectionResult.CONNECTED, GmailConnectionRepositoryImpl(authorizer).connect())
        assertEquals(listOf(true), authorizer.requests)
    }

    @Test
    fun connect_mapsEveryError() = runTest {
        val expected = mapOf(
            GmailAuthError.CANCELLED to GmailConnectionResult.CANCELLED,
            GmailAuthError.NOT_GRANTED to GmailConnectionResult.PERMISSION_DENIED,
            GmailAuthError.FAILED to GmailConnectionResult.FAILED,
        )
        expected.forEach { (error, result) ->
            assertEquals(result, GmailConnectionRepositoryImpl(FakeAuthorizer(error)).connect(), "for $error")
        }
    }
}
