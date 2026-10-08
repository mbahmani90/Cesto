package com.majidbahmani.cesto.feature.receipts.domain.usecase

import com.majidbahmani.cesto.feature.receipts.domain.model.DailySpending
import com.majidbahmani.cesto.feature.receipts.domain.repository.ReceiptRepository
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/**
 * Yesterday's spending for the daily summary: from yesterday 00:00 to just before today 00:00, in the phone's
 * time zone. Counted by calendar day, so a summer-time day of 23 or 25 hours is still one whole day.
 */
class GetYesterdaySpendingUseCase(
    private val repository: ReceiptRepository,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }
) {
    suspend operator fun invoke(): DailySpending {
        val zone = timeZone()
        val today = clock.now().toLocalDateTime(zone).date
        val fromMillis = today.minus(1, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds()
        val toMillis = today.atStartOfDayIn(zone).toEpochMilliseconds() - 1 // the query includes both ends
        return repository.spendingBetween(fromMillis, toMillis)
    }
}
