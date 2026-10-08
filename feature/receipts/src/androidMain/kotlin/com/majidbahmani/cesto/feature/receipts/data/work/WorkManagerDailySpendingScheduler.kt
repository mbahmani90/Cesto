package com.majidbahmani.cesto.feature.receipts.data.work

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.majidbahmani.cesto.feature.receipts.domain.model.nextDailyRun
import com.majidbahmani.cesto.feature.receipts.domain.repository.DailySpendingScheduler
import com.majidbahmani.cesto.feature.receipts.presentation.work.DailySpendingWorker
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlinx.datetime.TimeZone

/** [DailySpendingScheduler] with WorkManager: one unique 24 h periodic job running [DailySpendingWorker]. */
class WorkManagerDailySpendingScheduler(
    private val workManager: WorkManager,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }
) : DailySpendingScheduler {

    // KEEP: if the job is already scheduled nothing changes.
    override fun schedule() {
        val now = clock.now()
        val delayMillis = (nextDailyRun(now, timeZone()) - now).inWholeMilliseconds
        val request = periodicRequest()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /**
     * A periodic job counts the next 24 h from when the last run finished, so Doze delays and summer-time
     * changes would make it drift. Called while the worker is running: WorkManager applies the change to the
     * next run, not the current one. Never before `Result.retry()`: the override also replaces the backoff.
     */
    override fun pinNextRun(workId: String) {
        val nextMillis = nextDailyRun(clock.now(), timeZone()).toEpochMilliseconds()
        val request = periodicRequest()
            .setId(UUID.fromString(workId))
            .setNextScheduleTimeOverride(nextMillis)
            .build()
        workManager.updateWork(request)
    }

    override fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    // Same worker, period and constraints for schedule() and pinNextRun(): updateWork() replaces them all.
    private fun periodicRequest(): PeriodicWorkRequest.Builder = PeriodicWorkRequestBuilder<DailySpendingWorker>(24, TimeUnit.HOURS)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())

    private companion object {
        const val WORK_NAME = "daily-spending"
    }
}
