package com.majidbahmani.cesto.gmailauth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class GoogleIdTokenProviderTest {

    private class FakeProvider(private val answer: (onSuccess: (String) -> Unit, onFailure: (GoogleSignInError) -> Unit) -> Unit) :
        GoogleIdTokenProvider {
        override fun signIn(onSuccess: (String) -> Unit, onFailure: (GoogleSignInError) -> Unit) = answer(onSuccess, onFailure)

        override fun signOut() = Unit
    }

    @Test
    fun success_returnsTheIdToken() = runTest {
        assertEquals(GoogleIdToken.Received("id-token"), FakeProvider { onSuccess, _ -> onSuccess("id-token") }.requestIdToken())
    }

    @Test
    fun failure_returnsTheReason() = runTest {
        val provider = FakeProvider { _, onFailure -> onFailure(GoogleSignInError.NO_ACCOUNT) }

        assertEquals(GoogleIdToken.Failed(GoogleSignInError.NO_ACCOUNT), provider.requestIdToken())
    }

    @Test
    fun secondCallback_isIgnored() = runTest {
        val provider = FakeProvider { onSuccess, onFailure ->
            onSuccess("id-token")
            onFailure(GoogleSignInError.FAILED)
        }

        assertEquals(GoogleIdToken.Received("id-token"), provider.requestIdToken())
    }
}
