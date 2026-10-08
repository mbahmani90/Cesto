package com.majidbahmani.cesto.feature.receipts.domain.model

import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** When the daily spending summary runs, in the phone's time zone. */
val DAILY_SPENDING_TIME = LocalTime(hour = 9, minute = 0)

/**
 * The next [at] strictly after [now]: today if it's still before [at], otherwise tomorrow.
 *
 * Counted as a time of day, not as "now + 24 h", so a summer-time change keeps it at 09:00. Shared, so
 * iOS can schedule its local notification with the same rule later.
 */
fun nextDailyRun(now: Instant, timeZone: TimeZone, at: LocalTime = DAILY_SPENDING_TIME): Instant {
    val today = now.toLocalDateTime(timeZone).date
    val todayRun = today.atTime(at).toInstant(timeZone)
    return if (todayRun > now) todayRun else today.plus(1, DateTimeUnit.DAY).atTime(at).toInstant(timeZone)
}
