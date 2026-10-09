package com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.majidbahmani.cesto.feature.onboarding.domain.model.GmailConnectionResult
import com.majidbahmani.cesto.feature.onboarding.domain.model.SignInResult
import com.majidbahmani.cesto.feature.onboarding.domain.usecase.CheckGmailConnectionUseCase
import com.majidbahmani.cesto.feature.onboarding.domain.usecase.CheckSignInUseCase
import com.majidbahmani.cesto.feature.onboarding.domain.usecase.ConnectGmailUseCase
import com.majidbahmani.cesto.feature.onboarding.domain.usecase.SignInUseCase
import com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel.OnboardingUiState.ErrorReason
import com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel.OnboardingUiState.Status
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Two steps behind one button: sign in to Cesto with Google, then allow Gmail. */
class OnboardingViewModel(
    private val checkSignIn: CheckSignInUseCase,
    private val signIn: SignInUseCase,
    private val checkGmailConnection: CheckGmailConnectionUseCase,
    private val connectGmail: ConnectGmailUseCase
) : ViewModel() {

    // Two sources change the state (start check and button), so it's assigned, not derived (doc 16).
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val signedIn = checkSignIn()
            val connected = signedIn && checkGmailConnection()
            _uiState.update { it.copy(status = if (connected) Status.CONNECTED else Status.READY, signedIn = signedIn) }
        }
    }

    fun onContinue() {
        // Ignore double taps and taps during the start check.
        if (_uiState.value.status != Status.READY) return

        viewModelScope.launch {
            if (!_uiState.value.signedIn && !signInStep()) return@launch
            gmailStep()
        }
    }

    /** True when signed in; otherwise back to the intro with the reason. */
    private suspend fun signInStep(): Boolean {
        _uiState.update { it.copy(status = Status.SIGNING_IN, error = null) }
        val error = when (signIn()) {
            SignInResult.SIGNED_IN -> {
                _uiState.update { it.copy(signedIn = true) }
                return true
            }

            SignInResult.CANCELLED -> null

            SignInResult.NO_GOOGLE_ACCOUNT -> ErrorReason.NO_GOOGLE_ACCOUNT

            SignInResult.REJECTED -> ErrorReason.SIGN_IN_REJECTED

            SignInResult.FAILED -> ErrorReason.FAILED
        }
        _uiState.update { it.copy(status = Status.READY, error = error) }
        return false
    }

    private suspend fun gmailStep() {
        _uiState.update { it.copy(status = Status.CONNECTING, error = null) }
        val result = connectGmail()
        _uiState.update {
            when (result) {
                GmailConnectionResult.CONNECTED -> it.copy(status = Status.CONNECTED)
                GmailConnectionResult.CANCELLED -> it.copy(status = Status.READY)
                GmailConnectionResult.PERMISSION_DENIED -> it.copy(status = Status.READY, error = ErrorReason.PERMISSION_DENIED)
                GmailConnectionResult.FAILED -> it.copy(status = Status.READY, error = ErrorReason.FAILED)
            }
        }
    }
}
