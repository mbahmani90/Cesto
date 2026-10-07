package com.majidbahmani.cesto.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.majidbahmani.cesto.gmailauth.GmailAuthError
import com.majidbahmani.cesto.gmailauth.GmailAuthorizer
import com.majidbahmani.cesto.gmailauth.GmailScopes

/**
 * Gmail permission via Google Identity's AuthorizationClient: no separate sign-in step, Google shows
 * the account picker and consent screen itself, and returns a fresh token silently once granted.
 *
 * Application-scoped (created before Koin). The consent screen needs an Activity to start it, so
 * MainActivity [attach]es its result launcher while it exists.
 */
class AndroidGmailAuthorizer(context: Context) : GmailAuthorizer {

    private val client = Identity.getAuthorizationClient(context.applicationContext)
    private val request = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(GmailScopes.GMAIL_READONLY)))
        .build()

    private var launcher: ActivityResultLauncher<IntentSenderRequest>? = null

    /** Callbacks of the request waiting for the consent screen; survives Activity recreation. */
    private var pending: Pending? = null

    private class Pending(val onSuccess: (String) -> Unit, val onFailure: (GmailAuthError) -> Unit)

    fun attach(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        this.launcher = launcher
    }

    fun detach(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        if (this.launcher === launcher) this.launcher = null
    }

    override fun authorize(interactive: Boolean, onSuccess: (accessToken: String) -> Unit, onFailure: (GmailAuthError) -> Unit) {
        client.authorize(request)
            .addOnSuccessListener { result ->
                when {
                    !result.hasResolution() -> deliver(result, onSuccess, onFailure)
                    !interactive -> onFailure(GmailAuthError.NOT_GRANTED)
                    else -> showConsent(result, Pending(onSuccess, onFailure))
                }
            }
            .addOnFailureListener { e ->
                // e.g. ApiException 10 (DEVELOPER_ERROR): package or SHA-1 not in the OAuth client.
                Log.w(TAG, "authorize failed", e)
                onFailure(GmailAuthError.FAILED)
            }
    }

    /** Result of the consent screen, forwarded by MainActivity's launcher. */
    fun onConsentResult(activityResult: ActivityResult) {
        val callbacks = pending ?: return
        pending = null

        if (activityResult.resultCode != Activity.RESULT_OK) {
            callbacks.onFailure(GmailAuthError.CANCELLED)
            return
        }
        try {
            deliver(client.getAuthorizationResultFromIntent(activityResult.data), callbacks.onSuccess, callbacks.onFailure)
        } catch (e: Exception) {
            Log.w(TAG, "consent result unreadable", e)
            callbacks.onFailure(GmailAuthError.FAILED)
        }
    }

    private fun showConsent(result: AuthorizationResult, callbacks: Pending) {
        val intentSender = result.pendingIntent?.intentSender
        val launcher = launcher
        if (intentSender == null || launcher == null) {
            Log.w(TAG, "consent screen can't be shown (no Activity attached)")
            callbacks.onFailure(GmailAuthError.FAILED)
            return
        }
        pending?.onFailure?.invoke(GmailAuthError.CANCELLED) // a newer request replaces an unfinished one
        pending = callbacks
        launcher.launch(IntentSenderRequest.Builder(intentSender).build())
    }

    private fun deliver(result: AuthorizationResult, onSuccess: (String) -> Unit, onFailure: (GmailAuthError) -> Unit) {
        val token = result.accessToken
        val granted = result.grantedScopes.any { it == GmailScopes.GMAIL_READONLY }
        if (token != null && granted) onSuccess(token) else onFailure(GmailAuthError.NOT_GRANTED)
    }

    private companion object {
        const val TAG = "GmailAuthorizer"
    }
}
