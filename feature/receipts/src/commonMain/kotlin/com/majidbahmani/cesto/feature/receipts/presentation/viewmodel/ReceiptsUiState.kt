package com.majidbahmani.cesto.feature.receipts.presentation.viewmodel

import com.majidbahmani.cesto.feature.receipts.presentation.model.ReceiptItemUi

data class ReceiptsUiState(
    /** True until the database has answered once: avoids flashing "no receipts". */
    val isLoading: Boolean = true,
    val receipts: List<ReceiptItemUi> = emptyList(),
    /** Gmail sync running: pull-to-refresh indicator. The list stays visible meanwhile. */
    val isSyncing: Boolean = false,
    /** Result of the last sync if something wasn't done; null when everything worked. */
    val syncProblem: SyncProblem? = null,
) {
    sealed interface SyncProblem {
        /** Access revoked: connect Gmail again. */
        data object NotAuthorized : SyncProblem

        /** Gmail couldn't be searched (offline, Gmail error). */
        data object Failed : SyncProblem

        /** Search worked, but [count] emails or PDFs couldn't be loaded this time. */
        data class Incomplete(val count: Int) : SyncProblem
    }
}
