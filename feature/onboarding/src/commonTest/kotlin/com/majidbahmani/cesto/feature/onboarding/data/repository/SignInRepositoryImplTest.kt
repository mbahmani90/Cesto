package com.majidbahmani.cesto.feature.onboarding.data.repository

import com.majidbahmani.cesto.account.Account
import com.majidbahmani.cesto.account.AccountRepository
import com.majidbahmani.cesto.account.IdentityPlatformError
import com.majidbahmani.cesto.account.IdentityPlatformException
import com.majidbahmani.cesto.feature.onboarding.domain.model.SignInResult
import com.majidbahmani.cesto.gmailauth.GoogleIdTokenProvider
import com.majidbahmani.cesto.gmailauth.GoogleSignInError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

class SignInRepositoryImplTest {

    /** Answers with "google-token", or with [error] if set. */
    private class FakeProvider(private val error: GoogleSignInError? = null) : GoogleIdTokenProvider {
        override fun signIn(onSuccess: (String) -> Unit, onFailure: (GoogleSignInError) -> Unit) =
            if (error == null) onSuccess("google-token") else onFailure(error)
    }

    private class FakeAccounts(private val failure: Exception? = null) : AccountRepository {
        val signedInWith = mutableListOf<String>()
        override val activeAccount = MutableStateFlow<Account?>(null)
        override val accounts = MutableStateFlow(emptyList<Account>())

        override suspend fun signIn(googleIdToken: String): Account {
            failure?.let { throw it }
            signedInWith += googleIdToken
            return Account("user-1", "ana@example.com").also { activeAccount.value = it }
        }

        override suspend fun switchTo(localId: String) = error("not used")
        override suspend fun validIdToken() = error("not used")
        override suspend fun signOut(localId: String) = error("not used")
    }

    @Test
    fun isSignedIn_followsTheActiveAccount() = runTest {
        val accounts = FakeAccounts()
        val repository = SignInRepositoryImpl(FakeProvider(), accounts)
        assertFalse(repository.isSignedIn())

        accounts.activeAccount.value = Account("user-1", "ana@example.com")
        assertTrue(repository.isSignedIn())
    }

    @Test
    fun signIn_passesTheGoogleTokenToTheAccount() = runTest {
        val accounts = FakeAccounts()

        assertEquals(SignInResult.SIGNED_IN, SignInRepositoryImpl(FakeProvider(), accounts).signIn())
        assertEquals(listOf("google-token"), accounts.signedInWith)
    }

    @Test
    fun googleErrors_mapToResults() = runTest {
        suspend fun resultFor(error: GoogleSignInError) = SignInRepositoryImpl(FakeProvider(error), FakeAccounts()).signIn()

        assertEquals(SignInResult.CANCELLED, resultFor(GoogleSignInError.CANCELLED))
        assertEquals(SignInResult.NO_GOOGLE_ACCOUNT, resultFor(GoogleSignInError.NO_ACCOUNT))
        assertEquals(SignInResult.FAILED, resultFor(GoogleSignInError.FAILED))
    }

    @Test
    fun identityPlatformRefusal_isRejected_otherErrorsFailed() = runTest {
        val refused = FakeAccounts(IdentityPlatformException(IdentityPlatformError.API_KEY_REJECTED, 400))
        assertEquals(SignInResult.REJECTED, SignInRepositoryImpl(FakeProvider(), refused).signIn())

        val offline = FakeAccounts(IllegalStateException("offline"))
        assertEquals(SignInResult.FAILED, SignInRepositoryImpl(FakeProvider(), offline).signIn())
    }
}
