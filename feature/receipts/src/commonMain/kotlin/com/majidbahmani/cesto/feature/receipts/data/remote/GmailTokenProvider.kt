package com.majidbahmani.cesto.feature.receipts.data.remote

import com.majidbahmani.cesto.gmailauth.GmailAccess
import com.majidbahmani.cesto.gmailauth.GmailAuthError
import com.majidbahmani.cesto.gmailauth.GmailAuthorizer
import com.majidbahmani.cesto.gmailauth.requestAccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Thrown before any request when Gmail access isn't (or no longer) granted. */
class GmailNotAuthorizedException(val reason: GmailAuthError) : Exception("Gmail access not granted: $reason")

/**
 * Access token for each Gmail request. Never shows UI: the sync runs in the background, and only
 * onboarding asks the user.
 *
 * Parallel requests share one token: it's kept for [cacheMillis] (Google tokens live 60 minutes;
 * the platform SDKs refresh them) and the [Mutex] makes sure only one SDK call runs at a time.
 */
class GmailTokenProvider(
    private val authorizer: GmailAuthorizer,
    private val currentTimeMillis: () -> Long,
    private val cacheMillis: Long = 5 * 60 * 1000L,
) {
    private val mutex = Mutex()
    private var cached: String? = null
    private var cachedAtMillis = 0L

    suspend fun accessToken(): String = mutex.withLock {
        val now = currentTimeMillis()
        cached?.takeIf { now - cachedAtMillis < cacheMillis }?.let { return it }

        when (val access = authorizer.requestAccess(interactive = false)) {
            is GmailAccess.Granted -> access.accessToken.also {
                cached = it
                cachedAtMillis = now
            }
            is GmailAccess.Denied -> {
                cached = null
                throw GmailNotAuthorizedException(access.error)
            }
        }
    }
}
