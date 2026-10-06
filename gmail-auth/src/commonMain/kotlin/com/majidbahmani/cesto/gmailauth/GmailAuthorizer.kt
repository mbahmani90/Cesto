package com.majidbahmani.cesto.gmailauth

/** OAuth scopes the app requests. Read-only: Cesto never sends, deletes or changes emails. */
object GmailScopes {
    const val GMAIL_READONLY = "https://www.googleapis.com/auth/gmail.readonly"
}

/** Why no access token was returned. */
enum class GmailAuthError {
    /** The user closed Google's dialog. */
    CANCELLED,

    /** Access isn't granted: not yet (silent check) or the user unticked the Gmail permission. */
    NOT_GRANTED,

    /** Anything else: no network, misconfigured OAuth client, Play services error. */
    FAILED,
}

/**
 * Gets a Gmail access token from Google's SDK on each platform; the token never leaves the phone.
 *
 * Implemented by the platform apps (Android `AuthorizationClient`, iOS `GoogleSignIn`) and passed
 * to `initKoin`. Callback-based so Swift can implement it; Kotlin code uses [requestAccess].
 * Exactly one callback is called, once.
 */
interface GmailAuthorizer {
    /**
     * @param interactive true: may show Google's account picker and consent dialog.
     *   false: only returns a token if access is already granted, never shows UI.
     */
    fun authorize(
        interactive: Boolean,
        onSuccess: (accessToken: String) -> Unit,
        onFailure: (GmailAuthError) -> Unit,
    )
}
