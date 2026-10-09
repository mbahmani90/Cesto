package com.majidbahmani.cesto.feature.settings.data.repository

import com.majidbahmani.cesto.account.Account
import com.majidbahmani.cesto.account.AccountRepository
import com.majidbahmani.cesto.gmailauth.GoogleIdTokenProvider
import com.majidbahmani.cesto.gmailauth.GoogleSignInError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class AccountSessionRepositoryImplTest {

    private class FakeAccounts(active: Account?) : AccountRepository {
        val signedOut = mutableListOf<String>()
        override val activeAccount = MutableStateFlow(active)
        override val accounts = MutableStateFlow(listOfNotNull(active))

        override suspend fun signOut(localId: String) {
            signedOut += localId
            activeAccount.value = null
        }

        override suspend fun signIn(googleIdToken: String) = error("not used")
        override suspend fun switchTo(localId: String) = error("not used")
        override suspend fun validIdToken() = error("not used")
    }

    private class FakeGoogle : GoogleIdTokenProvider {
        var signOutCalls = 0
        override fun signIn(onSuccess: (String) -> Unit, onFailure: (GoogleSignInError) -> Unit) = error("not used")

        override fun signOut() {
            signOutCalls++
        }
    }

    @Test
    fun email_orAnotherLabelWithoutOne() = runTest {
        assertEquals(
            "ana@example.com",
            AccountSessionRepositoryImpl(FakeAccounts(Account("u1", "ana@example.com")), FakeGoogle()).observeSignedInEmail().first()
        )
        assertEquals(
            "Ana",
            AccountSessionRepositoryImpl(FakeAccounts(Account("u1", null, "Ana")), FakeGoogle()).observeSignedInEmail().first()
        )
        assertNull(AccountSessionRepositoryImpl(FakeAccounts(null), FakeGoogle()).observeSignedInEmail().first())
    }

    @Test
    fun signOut_removesTheActiveAccount_andGoogleSignIn() = runTest {
        val accounts = FakeAccounts(Account("u1", "ana@example.com"))
        val google = FakeGoogle()

        AccountSessionRepositoryImpl(accounts, google).signOut()

        assertEquals(listOf("u1"), accounts.signedOut)
        assertEquals(1, google.signOutCalls)
    }

    @Test
    fun signOut_withoutAccount_doesNothing() = runTest {
        val google = FakeGoogle()

        AccountSessionRepositoryImpl(FakeAccounts(null), google).signOut()

        assertEquals(0, google.signOutCalls)
    }
}
