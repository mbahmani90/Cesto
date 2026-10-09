package com.majidbahmani.cesto.gmailauth

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** Why no Google ID token was returned. */
enum class GoogleSignInError {
    /** The user closed Google's dialog. */
    CANCELLED,

    /** No Google account on the device, or the user has none they're willing to use. */
    NO_ACCOUNT,

    /** Anything else: no network, misconfigured OAuth client, Play services error. */
    FAILED
}

/**
 * "Sign in with Google": shows Google's account picker and returns a Google ID token, which `:account`
 * exchanges for a Cesto (Identity Platform) account. Separate from [GmailAuthorizer]: sign-in only proves
 * who the user is; Gmail access is asked for afterwards.
 *
 * Implemented by the platform apps (Android Credential Manager with the Web client ID as audience,
 * iOS GoogleSignIn) and passed to `initKoin`. Callback-based so Swift can implement it; Kotlin code uses
 * [requestIdToken]. Exactly one callback is called, once.
 */
interface GoogleIdTokenProvider {
    fun signIn(onSuccess: (idToken: String) -> Unit, onFailure: (GoogleSignInError) -> Unit)

    /** Forgets the Google sign-in state kept by the SDK, so the next [signIn] starts fresh. Never shows UI. */
    fun signOut()
}

sealed interface GoogleIdToken {
    data class Received(val idToken: String) : GoogleIdToken

    data class Failed(val error: GoogleSignInError) : GoogleIdToken
}

/** Suspending form of [GoogleIdTokenProvider.signIn]; a late callback after cancellation is ignored. */
suspend fun GoogleIdTokenProvider.requestIdToken(): GoogleIdToken = suspendCancellableCoroutine { continuation ->
    signIn(
        onSuccess = { token -> if (continuation.isActive) continuation.resume(GoogleIdToken.Received(token)) },
        onFailure = { error -> if (continuation.isActive) continuation.resume(GoogleIdToken.Failed(error)) }
    )
}
