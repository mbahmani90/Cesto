package com.majidbahmani.cesto.feature.receipts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.majidbahmani.cesto.feature.receipts.domain.model.SyncFailure
import com.majidbahmani.cesto.feature.receipts.domain.model.SyncResult
import com.majidbahmani.cesto.feature.receipts.domain.usecase.ObserveReceiptsUseCase
import com.majidbahmani.cesto.feature.receipts.domain.usecase.SyncReceiptsUseCase
import com.majidbahmani.cesto.feature.receipts.presentation.mapper.toUi
import com.majidbahmani.cesto.feature.receipts.presentation.viewmodel.ReceiptsUiState.SyncProblem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

class ReceiptsViewModel(
    observeReceipts: ObserveReceiptsUseCase,
    private val syncReceipts: SyncReceiptsUseCase,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    private data class SyncState(val isSyncing: Boolean = false, val problem: SyncProblem? = null)

    private val syncState = MutableStateFlow(SyncState())

    // The list comes from the database; the sync only writes there (single source of truth).
    val uiState: StateFlow<ReceiptsUiState> = combine(observeReceipts(), syncState) { receipts, sync ->
        ReceiptsUiState(
            isLoading = false,
            receipts = receipts.map { it.toUi(timeZone) },
            isSyncing = sync.isSyncing,
            syncProblem = sync.problem,
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, ReceiptsUiState())

    init {
        sync() // new receipts every time the screen opens
    }

    /** Pull to refresh and the error's retry button. */
    fun onRefresh() = sync()

    private fun sync() {
        if (syncState.value.isSyncing) return
        syncState.update { SyncState(isSyncing = true) }
        viewModelScope.launch {
            val problem = when (val result = syncReceipts()) {
                is SyncResult.Success -> result.incomplete.takeIf { it > 0 }?.let { SyncProblem.Incomplete(it) }
                is SyncResult.Failure -> when (result.reason) {
                    SyncFailure.NOT_AUTHORIZED -> SyncProblem.NotAuthorized
                    SyncFailure.FAILED -> SyncProblem.Failed
                }
            }
            syncState.update { SyncState(isSyncing = false, problem = problem) }
        }
    }
}
