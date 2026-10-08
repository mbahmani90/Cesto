package com.majidbahmani.cesto.feature.receipts.presentation.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Yesterday's spending, once a day (scheduled by `DailySpendingScheduler`). An entry point like a
 * ViewModel: it only calls use cases.
 *
 * Only a placeholder for now so the scheduler has a worker to run: sync, sum and notification come next.
 */
class DailySpendingWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = Result.success()
}
