package com.majidbahmani.cesto.feature.receipts.domain.usecase

import com.majidbahmani.cesto.feature.receipts.domain.model.DailySpending
import com.majidbahmani.cesto.feature.receipts.domain.model.Receipt
import com.majidbahmani.cesto.feature.receipts.domain.model.SyncResult
import com.majidbahmani.cesto.feature.receipts.domain.repository.ReceiptRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone

class GetYesterdaySpendingUseCaseTest {

    /** Remembers the asked range and answers with [answer]. */
    private class FakeReceiptRepository(private val answer: DailySpending) : ReceiptRepository {
        var range: Pair<Long, Long>? = null

        override fun observeReceipts(): Flow<List<Receipt>> = emptyFlow()

        override suspend fun sync(lookBackMonths: Int): SyncResult = error("not used")

        override suspend fun spendingBetween(fromMillis: Long, toMillis: Long): DailySpending {
            range = fromMillis to toMillis
            return answer
        }
    }

    private val lisbon = TimeZone.of("Europe/Lisbon")
    private val repository = FakeReceiptRepository(DailySpending(totalCents = 2_345, receiptCount = 3))

    private fun useCase(now: String) = GetYesterdaySpendingUseCase(
        repository = repository,
        clock = object : Clock {
            override fun now() = Instant.parse(now)
        },
        timeZone = { lisbon }
    )

    private fun millis(instant: String) = Instant.parse(instant).toEpochMilliseconds()

    @Test
    fun morning_wholeOfYesterday() = runTest {
        // 08/10/2026 08:30 in Lisbon (WEST, UTC+1): yesterday is 07/10 00:00 to 23:59:59.999.
        useCase(now = "2026-10-08T07:30:00Z")()

        assertEquals(millis("2026-10-06T23:00:00Z") to millis("2026-10-07T23:00:00Z") - 1, repository.range)
    }

    @Test
    fun justAfterMidnight_stillTheDayBefore() = runTest {
        // 08/10/2026 00:05 in Lisbon: "yesterday" is 07/10, not 06/10.
        useCase(now = "2026-10-07T23:05:00Z")()

        assertEquals(millis("2026-10-06T23:00:00Z") to millis("2026-10-07T23:00:00Z") - 1, repository.range)
    }

    @Test
    fun summerTimeEnds_yesterdayHas25Hours() = runTest {
        // 26/10/2026 08:00 WET; 25/10 started in WEST (UTC+1) and ended in WET (UTC+0).
        useCase(now = "2026-10-26T08:00:00Z")()

        assertEquals(millis("2026-10-24T23:00:00Z") to millis("2026-10-26T00:00:00Z") - 1, repository.range)
    }

    @Test
    fun summerTimeStarts_yesterdayHas23Hours() = runTest {
        // 30/03/2026 09:00 WEST; 29/03 started in WET (UTC+0) and ended in WEST (UTC+1).
        useCase(now = "2026-03-30T08:00:00Z")()

        assertEquals(millis("2026-03-29T00:00:00Z") to millis("2026-03-29T23:00:00Z") - 1, repository.range)
    }

    @Test
    fun returnsTheRepositorySum() = runTest {
        assertEquals(DailySpending(totalCents = 2_345, receiptCount = 3), useCase(now = "2026-10-08T07:30:00Z")())
    }
}
