package com.majidbahmani.cesto.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.majidbahmani.cesto.feature.settings.domain.model.SaveKeyResult
import com.majidbahmani.cesto.feature.settings.domain.usecase.ObserveGeminiKeyUseCase
import com.majidbahmani.cesto.feature.settings.domain.usecase.RemoveGeminiKeyUseCase
import com.majidbahmani.cesto.feature.settings.domain.usecase.SaveGeminiKeyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    observeGeminiKey: ObserveGeminiKeyUseCase,
    private val saveGeminiKey: SaveGeminiKeyUseCase,
    private val removeGeminiKey: RemoveGeminiKeyUseCase
) : ViewModel() {

    /** Screen-only state (doc 29), grouped: one flow, one update per event. */
    private data class ScreenState(
        val isChanging: Boolean = false,
        val accountEmail: String = "",
        val keyInput: String = "",
        val isKeyVisible: Boolean = false,
        val isTesting: Boolean = false,
        val feedback: SaveKeyResult? = null
    )

    private val screenState = MutableStateFlow(ScreenState())

    val uiState: StateFlow<SettingsUiState> = combine(observeGeminiKey(), screenState) { masked, screen ->
        SettingsUiState(
            isLoading = false,
            savedKeyMasked = masked,
            isEditing = masked == null || screen.isChanging,
            accountEmail = screen.accountEmail,
            keyInput = screen.keyInput,
            isKeyVisible = screen.isKeyVisible,
            isTesting = screen.isTesting,
            feedback = screen.feedback
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, SettingsUiState())

    fun onAccountEmailChange(email: String) = screenState.update { it.copy(accountEmail = email) }

    fun onKeyInputChange(input: String) = screenState.update { it.copy(keyInput = input, feedback = null) }

    fun onToggleKeyVisibility() = screenState.update { it.copy(isKeyVisible = !it.isKeyVisible) }

    fun onChangeKey() = screenState.update { it.copy(isChanging = true, keyInput = "", feedback = null) }

    fun onCancelChange() = screenState.update { ScreenState() }

    fun onTestAndSave() {
        if (screenState.value.isTesting) return
        val input = screenState.value.keyInput
        screenState.update { it.copy(isTesting = true, feedback = null) }
        viewModelScope.launch {
            val result = saveGeminiKey(input)
            screenState.update {
                if (result == SaveKeyResult.SAVED) {
                    ScreenState(feedback = SaveKeyResult.SAVED) // key out of memory, editor closed
                } else {
                    it.copy(isTesting = false, feedback = result)
                }
            }
        }
    }

    fun onRemoveKey() {
        viewModelScope.launch {
            removeGeminiKey()
            screenState.update { ScreenState() }
        }
    }
}
