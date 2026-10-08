package com.majidbahmani.cesto.di

import com.majidbahmani.cesto.feature.receipts.domain.repository.DailySpendingScheduler
import org.koin.core.KoinApplication

/**
 * Schedules the background jobs, once per process right after [initKoin] (Koin only creates the schedulers).
 * Safe on every start: an already scheduled job isn't changed, and after a force-stop it's scheduled again.
 */
fun KoinApplication.startBackgroundWork() {
    koin.get<DailySpendingScheduler>().schedule()
}
