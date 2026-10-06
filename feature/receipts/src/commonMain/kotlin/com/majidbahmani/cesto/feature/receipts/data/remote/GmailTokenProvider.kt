package com.majidbahmani.cesto.feature.receipts.data.remote

import com.majidbahmani.cesto.gmailauth.GmailAccess
import com.majidbahmani.cesto.gmailauth.GmailAuthError
import com.majidbahmani.cesto.gmailauth.GmailAuthorizer
import com.majidbahmani.cesto.gmailauth.requestAccess

/** Thrown before any request when Gmail access isn't (or no longer) granted. */
class GmailNotAuthorizedException(val reason: GmailAuthError) : Exception("Gmail access not granted: $reason")

/**
 * Access token for each Gmail request. Never shows UI: the sync runs in the background, and only
 * onboarding asks the user. The platform SDKs cache the token and refresh it when it expires.
 */
class GmailTokenProvider(private val authorizer: GmailAuthorizer) {
    suspend fun accessToken(): String =
        when (val access = authorizer.requestAccess(interactive = false)) {
            is GmailAccess.Granted -> access.accessToken
            is GmailAccess.Denied -> throw GmailNotAuthorizedException(access.error)
        }
}
