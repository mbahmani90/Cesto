package com.majidbahmani.cesto.feature.receipts.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.majidbahmani.cesto.database.CestoDatabase
import com.majidbahmani.cesto.database.Receipt as ReceiptRow
import com.majidbahmani.cesto.feature.receipts.data.remote.PdfAttachmentRef
import com.majidbahmani.cesto.feature.receipts.domain.model.ReceiptStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** SQLite calls are blocking: every call here switches to [ioDispatcher] (doc 14). */
class ReceiptLocalDataSource(
    private val database: CestoDatabase,
    private val ioDispatcher: CoroutineDispatcher,
    private val currentTimeMillis: () -> Long,
) {
    private val receipts = database.receiptQueries
    private val messages = database.gmailMessageQueries

    fun observeAll(): Flow<List<ReceiptRow>> = receipts.selectAll().asFlow().mapToList(ioDispatcher)

    suspend fun isChecked(messageId: String): Boolean = withContext(ioDispatcher) {
        messages.isChecked(messageId).executeAsOne()
    }

    /**
     * Saves the message's PDFs as receipts and marks the message as checked, in one transaction:
     * an interrupted sync never leaves a message "checked" without its receipts.
     * @return how many receipts are new.
     */
    suspend fun saveCheckedMessage(messageId: String, receivedAtMillis: Long, pdfs: List<PdfAttachmentRef>): Int =
        withContext(ioDispatcher) {
            database.transactionWithResult {
                pdfs.sumOf { pdf ->
                    receipts.insertIfNew(messageId, pdf.partId, pdf.fileName, receivedAtMillis)
                    receipts.changes().executeAsOne().toInt()
                }.also { messages.insert(messageId, receivedAtMillis, currentTimeMillis()) }
            }
        }

    suspend fun receiptsOfMessage(messageId: String): List<ReceiptRow> = withContext(ioDispatcher) {
        receipts.selectByMessage(messageId).executeAsList()
    }

    suspend fun receiptsWithStatus(status: ReceiptStatus): List<ReceiptRow> = withContext(ioDispatcher) {
        receipts.selectByStatus(status.name).executeAsList()
    }

    suspend fun markDownloaded(id: Long, pdfPath: String) = withContext(ioDispatcher) {
        receipts.markDownloaded(pdfPath, id)
    }

    suspend fun markFailed(id: Long) = withContext(ioDispatcher) {
        receipts.updateStatus(ReceiptStatus.FAILED.name, id)
    }
}
