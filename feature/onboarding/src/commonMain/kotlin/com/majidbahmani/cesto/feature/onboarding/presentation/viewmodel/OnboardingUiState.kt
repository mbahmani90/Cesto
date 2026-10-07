package com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel

data class OnboardingUiState(
    val status: Status = Status.CHECKING,
    /** Shown under the buttons while [Status.READY]; null after a cancel or a new attempt. */
    val error: ErrorReason? = null
) {
    enum class Status {
        /** Silent check at start: was Gmail connected before? Shows a spinner, no intro yet. */
        CHECKING,

        /** Intro with "Connect Gmail" and "Try demo". */
        READY,

        /** Google's dialog is open or being prepared. */
        CONNECTING,

        /** Access granted: the route navigates on. */
        CONNECTED
    }

    /** A reason, not text: the UI picks the string. */
    enum class ErrorReason { PERMISSION_DENIED, FAILED }
}
