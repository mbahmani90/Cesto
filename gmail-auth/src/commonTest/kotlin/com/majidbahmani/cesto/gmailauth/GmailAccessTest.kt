package com.majidbahmani.cesto.gmailauth

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GmailAccessTest {

    private class FakeAuthorizer(private val answer: (onSuccess: (String) -> Unit, onFailure: (GmailAuthError) -> Unit) -> Unit) :
        GmailAuthorizer {
        val requests = mutableListOf<Boolean>()

        override fun authorize(interactive: Boolean, onSuccess: (String) -> Unit, onFailure: (GmailAuthError) -> Unit) {
            requests += interactive
            answer(onSuccess, onFailure)
        }
    }

    @Test
    fun success_returnsGrantedWithToken() = runTest {
        val authorizer = FakeAuthorizer { onSuccess, _ -> onSuccess("token") }

        assertEquals(GmailAccess.Granted("token"), authorizer.requestAccess(interactive = true))
        assertEquals(listOf(true), authorizer.requests)
    }

    @Test
    fun failure_returnsDeniedWithReason() = runTest {
        val authorizer = FakeAuthorizer { _, onFailure -> onFailure(GmailAuthError.CANCELLED) }

        assertEquals(GmailAccess.Denied(GmailAuthError.CANCELLED), authorizer.requestAccess(interactive = false))
    }

    @Test
    fun secondCallback_isIgnored() = runTest {
        val authorizer = FakeAuthorizer { onSuccess, onFailure ->
            onSuccess("token")
            onFailure(GmailAuthError.FAILED)
        }

        assertEquals(GmailAccess.Granted("token"), authorizer.requestAccess(interactive = true))
    }
}
