package com.majidbahmani.cesto.auth

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.majidbahmani.cesto.gmailauth.GoogleIdTokenProvider
import com.majidbahmani.cesto.gmailauth.GoogleSignInError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * "Sign in with Google" via Credential Manager. The ID token's audience is the **Web** OAuth client
 * ([webClientId]), which Identity Platform's Google provider is configured with.
 *
 * Application-scoped (created before Koin). Credential Manager shows its UI from an Activity, so
 * MainActivity [attach]es itself while it exists.
 */
class AndroidGoogleIdTokenProvider(private val webClientId: String) : GoogleIdTokenProvider {

    private var activity: ComponentActivity? = null

    fun attach(activity: ComponentActivity) {
        this.activity = activity
    }

    fun detach(activity: ComponentActivity) {
        if (this.activity === activity) this.activity = null
    }

    override fun signIn(onSuccess: (idToken: String) -> Unit, onFailure: (GoogleSignInError) -> Unit) {
        val activity = activity
        if (activity == null || webClientId.isEmpty()) {
            Log.w(TAG, if (activity == null) "no Activity attached" else "GOOGLE_WEB_CLIENT_ID missing in local.properties")
            onFailure(GoogleSignInError.FAILED)
            return
        }
        // Always the full "Sign in with Google" flow: the user can pick any account or add one (several accounts per phone).
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId = webClientId).build())
            .build()
        activity.lifecycleScope.launch {
            try {
                val credential = CredentialManager.create(activity).getCredential(activity, request).credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    onSuccess(GoogleIdTokenCredential.createFrom(credential.data).idToken)
                } else {
                    Log.w(TAG, "unexpected credential type ${credential.type}")
                    onFailure(GoogleSignInError.FAILED)
                }
            } catch (e: CancellationException) {
                onFailure(GoogleSignInError.CANCELLED) // the Activity was destroyed
                throw e
            } catch (e: GetCredentialCancellationException) {
                onFailure(GoogleSignInError.CANCELLED)
            } catch (e: NoCredentialException) {
                onFailure(GoogleSignInError.NO_ACCOUNT)
            } catch (e: GetCredentialException) {
                // e.g. a wrong Web client ID, or the Android client's package / SHA-1 not registered.
                Log.w(TAG, "sign-in failed", e)
                onFailure(GoogleSignInError.FAILED)
            } catch (e: Exception) {
                Log.w(TAG, "Google ID token unreadable", e)
                onFailure(GoogleSignInError.FAILED)
            }
        }
    }

    private companion object {
        const val TAG = "GoogleIdTokenProvider"
    }
}
