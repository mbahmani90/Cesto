package com.majidbahmani.cesto.feature.onboarding.domain.model

/** Outcome of asking the user to connect Gmail. */
enum class GmailConnectionResult {
    CONNECTED,

    /** The user closed Google's dialog: not an error, they can simply try again. */
    CANCELLED,

    /** The dialog was confirmed but the Gmail permission was left unticked. */
    PERMISSION_DENIED,
    FAILED
}
