package com.majidbahmani.cesto.feature.receipts.domain.repository

/**
 * Runs the daily spending summary once a day around [DAILY_SPENDING_TIME][com.majidbahmani.cesto.feature.receipts.domain.model.DAILY_SPENDING_TIME].
 *
 * Koin only creates it; the callers decide when: the app calls [schedule] on every start, the worker calls
 * [pinNextRun] right before it succeeds, and "Disconnect Gmail" will call [cancel].
 */
interface DailySpendingScheduler {

    /** First run at the next 09:00, then every 24 h. Already scheduled: nothing changes, so safe on every start. */
    fun schedule()

    /** Moves the next run of the running job [workId] back to tomorrow 09:00, so delays don't add up. */
    fun pinNextRun(workId: String)

    /** No more runs. */
    fun cancel()
}
