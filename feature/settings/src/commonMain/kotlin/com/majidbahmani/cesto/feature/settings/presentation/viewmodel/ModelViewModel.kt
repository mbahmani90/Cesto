package com.majidbahmani.cesto.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.majidbahmani.cesto.feature.settings.domain.model.AiModel
import com.majidbahmani.cesto.feature.settings.domain.usecase.ObserveAiModelUseCase
import com.majidbahmani.cesto.feature.settings.domain.usecase.SelectAiModelUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ModelUiState(
    /** Null until the store has answered once. */
    val selected: AiModel? = null,
    val isPicking: Boolean = false
)

/** The Model card of Settings; separate from the Gemini key's ViewModel. */
class ModelViewModel(observeAiModel: ObserveAiModelUseCase, private val selectAiModel: SelectAiModelUseCase) : ViewModel() {

    private val isPicking = MutableStateFlow(false)

    val uiState: StateFlow<ModelUiState> = combine(observeAiModel(), isPicking) { selected, picking ->
        ModelUiState(selected = selected, isPicking = picking)
    }.stateIn(viewModelScope, SharingStarted.Lazily, ModelUiState())

    fun onOpenPicker() = isPicking.update { true }

    fun onDismissPicker() = isPicking.update { false }

    fun onSelect(model: AiModel) {
        isPicking.update { false }
        viewModelScope.launch { selectAiModel(model) }
    }
}
