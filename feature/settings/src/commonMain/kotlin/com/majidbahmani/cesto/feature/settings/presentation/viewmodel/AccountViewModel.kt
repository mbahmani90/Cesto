package com.majidbahmani.cesto.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.majidbahmani.cesto.feature.settings.domain.usecase.ObserveSignedInAccountUseCase
import com.majidbahmani.cesto.feature.settings.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    /** True until the session store has answered once. */
    val isLoading: Boolean = true,
    /** Null when signed out: the route then leaves for onboarding. */
    val email: String? = null,
    val isConfirmingSignOut: Boolean = false,
    val isSigningOut: Boolean = false
) {
    val isSignedOut: Boolean get() = !isLoading && email == null
}

/** The Account card of Settings; separate from the Gemini key's ViewModel. */
class AccountViewModel(observeSignedInAccount: ObserveSignedInAccountUseCase, private val signOut: SignOutUseCase) : ViewModel() {

    private data class ScreenState(val isConfirming: Boolean = false, val isSigningOut: Boolean = false)

    private val screenState = MutableStateFlow(ScreenState())

    val uiState: StateFlow<AccountUiState> = combine(observeSignedInAccount(), screenState) { email, screen ->
        AccountUiState(isLoading = false, email = email, isConfirmingSignOut = screen.isConfirming, isSigningOut = screen.isSigningOut)
    }.stateIn(viewModelScope, SharingStarted.Lazily, AccountUiState())

    fun onSignOut() = screenState.update { it.copy(isConfirming = true) }

    fun onDismissSignOut() = screenState.update { it.copy(isConfirming = false) }

    fun onConfirmSignOut() {
        if (screenState.value.isSigningOut) return
        screenState.update { ScreenState(isSigningOut = true) }
        // When it's done the account flow emits null, and the route navigates away.
        viewModelScope.launch { signOut() }
    }
}
