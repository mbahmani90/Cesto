package com.majidbahmani.cesto.feature.receipts.domain.usecase

import com.majidbahmani.cesto.feature.receipts.domain.model.Receipt
import com.majidbahmani.cesto.feature.receipts.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow

/** The receipt list comes from the local database only (single source of truth), never from Gmail directly. */
class ObserveReceiptsUseCase(private val repository: ReceiptRepository) {
    operator fun invoke(): Flow<List<Receipt>> = repository.observeReceipts()
}
