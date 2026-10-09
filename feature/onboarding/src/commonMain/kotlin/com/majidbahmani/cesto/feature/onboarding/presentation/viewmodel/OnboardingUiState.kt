package com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel

data class OnboardingUiState(
    val status: Status = Status.CHECKING,
    /** Signed in to Cesto: the button only asks for Gmail then. */
    val signedIn: Boolean = false,
    /** Shown under the buttons while [Status.READY]; null after a cancel or a new attempt. */
    val error: ErrorReason? = null
) {
    enum class Status {
        /** Silent check at start: signed in and Gmail connected before? Shows a spinner, no intro yet. */
        CHECKING,

        /** Intro with "Continue with Google" (or "Connect Gmail" once signed in) and "Try demo". */
        READY,

        /** Google's account picker is open, or the Cesto account is being created. */
        SIGNING_IN,

        /** Google's Gmail consent dialog is open or being prepared. */
        CONNECTING,

        /** Signed in and Gmail access granted: the route navigates on. */
        CONNECTED
    }

    /** A reason, not text: the UI picks the string. */
    enum class ErrorReason { NO_GOOGLE_ACCOUNT, SIGN_IN_REJECTED, PERMISSION_DENIED, FAILED }
}
