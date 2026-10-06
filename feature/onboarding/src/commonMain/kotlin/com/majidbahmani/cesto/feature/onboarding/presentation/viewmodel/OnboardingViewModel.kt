package com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.majidbahmani.cesto.feature.onboarding.domain.model.GmailConnectionResult
import com.majidbahmani.cesto.feature.onboarding.domain.usecase.CheckGmailConnectionUseCase
import com.majidbahmani.cesto.feature.onboarding.domain.usecase.ConnectGmailUseCase
import com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel.OnboardingUiState.ErrorReason
import com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel.OnboardingUiState.Status
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val checkGmailConnection: CheckGmailConnectionUseCase,
    private val connectGmail: ConnectGmailUseCase,
) : ViewModel() {

    // Two sources change the state (start check and button), so it's assigned, not derived (doc 16).
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val connected = checkGmailConnection()
            _uiState.update { it.copy(status = if (connected) Status.CONNECTED else Status.READY) }
        }
    }

    fun onConnectGmail() {
        // Ignore double taps and taps during the start check.
        if (_uiState.value.status != Status.READY) return
        _uiState.update { it.copy(status = Status.CONNECTING, error = null) }

        viewModelScope.launch {
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
}
