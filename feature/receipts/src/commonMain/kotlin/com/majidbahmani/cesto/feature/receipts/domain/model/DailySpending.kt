package com.majidbahmani.cesto.feature.receipts.domain.model

/** What was spent on one day: the sum of the receipts' totals, in cents (4,52 € = 452). */
data class DailySpending(val totalCents: Long, val receiptCount: Int)
