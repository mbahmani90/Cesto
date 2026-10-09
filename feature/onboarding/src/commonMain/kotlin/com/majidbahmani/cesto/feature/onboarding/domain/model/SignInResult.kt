package com.majidbahmani.cesto.feature.onboarding.domain.model

/** Outcome of "Sign in with Google" plus creating or finding the Cesto account. */
enum class SignInResult {
    SIGNED_IN,

    /** The user closed Google's dialog: not an error, they can simply try again. */
    CANCELLED,

    /** No Google account on this phone to sign in with. */
    NO_GOOGLE_ACCOUNT,

    /** Google gave a token, but Cesto's sign-in service refused it (setup, disabled sign-ups). */
    REJECTED,

    /** No connection, or Google's SDK failed. */
    FAILED
}
