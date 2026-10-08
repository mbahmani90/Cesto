package com.majidbahmani.cesto.feature.receipts.presentation.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.majidbahmani.cesto.core.logging.logWarning
import com.majidbahmani.cesto.feature.receipts.domain.repository.DailySpendingScheduler
import com.majidbahmani.cesto.feature.receipts.domain.usecase.GetYesterdaySpendingUseCase
import com.majidbahmani.cesto.feature.receipts.domain.usecase.SyncReceiptsUseCase
import com.majidbahmani.cesto.feature.receipts.presentation.notification.DailySpendingNotifier
import kotlin.coroutines.cancellation.CancellationException

/**
 * Yesterday's spending, once a day around 09:00 (scheduled by [DailySpendingScheduler]). An entry point like a
 * ViewModel: it only calls use cases and the notifier. Created by Koin's WorkerFactory (see CestoApp).
 */
class DailySpendingWorker(
    context: Context,
    params: WorkerParameters,
    private val syncReceipts: SyncReceiptsUseCase,
    private val getYesterdaySpending: GetYesterdaySpendingUseCase,
    private val notifier: DailySpendingNotifier,
    private val scheduler: DailySpendingScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            // Never throws: on failure (offline, Gmail access revoked) the receipts already saved are summed.
            syncReceipts()
            val spending = getYesterdaySpending()
            if (spending.receiptCount > 0) notifier.show(spending)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // No retry(): a later attempt would arrive late; tomorrow's run still happens.
            logWarning(TAG, "daily spending failed", e)
            return Result.failure()
        }
        // Right before success(): the next run goes back to tomorrow 09:00, so delays don't add up.
        runCatching { scheduler.pinNextRun(id.toString()) }.onFailure { logWarning(TAG, "pinNextRun failed", it) }
        return Result.success()
    }

    private companion object {
        const val TAG = "DailySpendingWorker"
    }
}
