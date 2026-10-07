package com.majidbahmani.cesto.gmailauth

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

sealed interface GmailAccess {
    data class Granted(val accessToken: String) : GmailAccess

    data class Denied(val error: GmailAuthError) : GmailAccess
}

/** Suspending form of [GmailAuthorizer.authorize]; a late callback after cancellation is ignored. */
suspend fun GmailAuthorizer.requestAccess(interactive: Boolean): GmailAccess = suspendCancellableCoroutine { continuation ->
    authorize(
        interactive = interactive,
        onSuccess = { token -> if (continuation.isActive) continuation.resume(GmailAccess.Granted(token)) },
        onFailure = { error -> if (continuation.isActive) continuation.resume(GmailAccess.Denied(error)) }
    )
}
