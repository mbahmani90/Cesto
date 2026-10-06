package com.majidbahmani.cesto.feature.receipts.domain.usecase

import com.majidbahmani.cesto.feature.receipts.domain.model.SyncResult
import com.majidbahmani.cesto.feature.receipts.domain.repository.ReceiptRepository

/**
 * Gmail → database; the list updates through [ObserveReceiptsUseCase].
 * Owns the business rule of how far back Cesto looks; the data layer turns it into a Gmail query.
 */
class SyncReceiptsUseCase(private val repository: ReceiptRepository) {
    suspend operator fun invoke(): SyncResult = repository.sync(lookBackMonths = LOOK_BACK_MONTHS)

    companion object {
        /** Enough for "this month vs last month"; fewer PDFs to download (and to extract later). */
        const val LOOK_BACK_MONTHS = 3
    }
}
