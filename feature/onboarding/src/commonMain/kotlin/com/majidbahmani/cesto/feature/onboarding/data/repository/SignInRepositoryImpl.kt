package com.majidbahmani.cesto.feature.onboarding.data.repository

import com.majidbahmani.cesto.account.AccountRepository
import com.majidbahmani.cesto.account.IdentityPlatformException
import com.majidbahmani.cesto.core.logging.logWarning
import com.majidbahmani.cesto.feature.onboarding.domain.model.SignInResult
import com.majidbahmani.cesto.feature.onboarding.domain.repository.SignInRepository
import com.majidbahmani.cesto.gmailauth.GoogleIdToken
import com.majidbahmani.cesto.gmailauth.GoogleIdTokenProvider
import com.majidbahmani.cesto.gmailauth.GoogleSignInError
import com.majidbahmani.cesto.gmailauth.requestIdToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/** Google's SDK proves who the user is; `:account` turns the Google ID token into a Cesto account. */
class SignInRepositoryImpl(private val googleIdTokens: GoogleIdTokenProvider, private val accounts: AccountRepository) :
    SignInRepository {

    override suspend fun isSignedIn(): Boolean = accounts.activeAccount.first() != null

    override suspend fun signIn(): SignInResult = when (val token = googleIdTokens.requestIdToken()) {
        is GoogleIdToken.Failed -> when (token.error) {
            GoogleSignInError.CANCELLED -> SignInResult.CANCELLED
            GoogleSignInError.NO_ACCOUNT -> SignInResult.NO_GOOGLE_ACCOUNT
            GoogleSignInError.FAILED -> SignInResult.FAILED
        }

        is GoogleIdToken.Received -> try {
            accounts.signIn(token.idToken)
            SignInResult.SIGNED_IN
        } catch (e: CancellationException) {
            throw e
        } catch (e: IdentityPlatformException) {
            // The reason (e.g. API_KEY_REJECTED) points at the setup step that's missing.
            logWarning(TAG, "Identity Platform refused the sign-in: ${e.error}")
            SignInResult.REJECTED
        } catch (e: Exception) {
            logWarning(TAG, "sign-in failed", e)
            SignInResult.FAILED
        }
    }

    private companion object {
        const val TAG = "SignIn"
    }
}
