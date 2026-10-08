package com.majidbahmani.cesto.feature.receipts.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

class DailyScheduleTest {

    private val lisbon = TimeZone.of("Europe/Lisbon")

    @Test
    fun beforeNine_today() {
        // 07/10/2026 08:30 in Lisbon (WEST, UTC+1).
        val now = Instant.parse("2026-10-07T07:30:00Z")

        assertEquals(Instant.parse("2026-10-07T08:00:00Z"), nextDailyRun(now, lisbon))
    }

    @Test
    fun afterNine_tomorrow() {
        val now = Instant.parse("2026-10-07T08:05:00Z")

        assertEquals(Instant.parse("2026-10-08T08:00:00Z"), nextDailyRun(now, lisbon))
    }

    @Test
    fun exactlyNine_tomorrow() {
        // The worker pins the next run while today's 09:00 run is going: never "now" again.
        val now = Instant.parse("2026-10-07T08:00:00Z")

        assertEquals(Instant.parse("2026-10-08T08:00:00Z"), nextDailyRun(now, lisbon))
    }

    @Test
    fun lateEvening_tomorrow() {
        val now = Instant.parse("2026-10-07T22:59:00Z")

        assertEquals(Instant.parse("2026-10-08T08:00:00Z"), nextDailyRun(now, lisbon))
    }

    @Test
    fun summerTimeEnds_stillNineLocal() {
        // 24/10/2026 10:00 WEST; on 25/10 clocks go back to WET (UTC+0): 25 h later, still 09:00.
        val now = Instant.parse("2026-10-24T09:00:00Z")

        assertEquals(Instant.parse("2026-10-25T09:00:00Z"), nextDailyRun(now, lisbon))
    }

    @Test
    fun summerTimeStarts_stillNineLocal() {
        // 28/03/2026 10:00 WET; on 29/03 clocks go forward to WEST (UTC+1): 23 h later, still 09:00.
        val now = Instant.parse("2026-03-28T10:00:00Z")

        assertEquals(Instant.parse("2026-03-29T08:00:00Z"), nextDailyRun(now, lisbon))
    }
}
