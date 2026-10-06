package com.majidbahmani.cesto.feature.receipts.domain.repository

import com.majidbahmani.cesto.feature.receipts.domain.model.Receipt
import com.majidbahmani.cesto.feature.receipts.domain.model.SyncResult
import kotlinx.coroutines.flow.Flow

interface ReceiptRepository {
    /** Receipts on this phone, newest first; emits again whenever the sync changes them. */
    fun observeReceipts(): Flow<List<Receipt>>

    /** Finds new receipt emails from the last [lookBackMonths] months in Gmail and downloads their PDFs. */
    suspend fun sync(lookBackMonths: Int): SyncResult
}
