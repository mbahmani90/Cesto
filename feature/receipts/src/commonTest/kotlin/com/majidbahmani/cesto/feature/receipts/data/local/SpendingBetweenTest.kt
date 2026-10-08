package com.majidbahmani.cesto.feature.receipts.data.local

import com.majidbahmani.cesto.database.CestoDatabase
import com.majidbahmani.cesto.feature.receipts.data.parser.ParsedReceipt
import com.majidbahmani.cesto.feature.receipts.domain.model.DailySpending
import com.majidbahmani.cesto.feature.receipts.fake.createTestDriver
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class SpendingBetweenTest {

    private val driver = createTestDriver()
    private val database = CestoDatabase(driver)

    @AfterTest
    fun tearDown() = driver.close()

    private fun TestScope.local() = ReceiptLocalDataSource(database, StandardTestDispatcher(testScheduler), currentTimeMillis = { 0 })

    /** A receipt as the sync leaves it; [totalCents] null = its text isn't read yet. */
    private suspend fun TestScope.receipt(messageId: String, purchasedAt: Long, totalCents: Long?) {
        database.receiptQueries.insertIfNew(messageId, "1", "receipt.pdf", purchasedAt)
        if (totalCents == null) return
        val id = database.receiptQueries.selectByMessage(messageId).executeAsOne().id
        local().markTextExtracted(
            id = id,
            text = "",
            parsed = ParsedReceipt(purchasedAtMillis = purchasedAt, totalCents = totalCents, receiptNumber = null, atcud = null)
        )
    }

    @Test
    fun sumsTheReceiptsInTheRange_bothEndsIncluded() = runTest {
        receipt("before", purchasedAt = 999, totalCents = 10_000)
        receipt("first", purchasedAt = 1_000, totalCents = 452)
        receipt("middle", purchasedAt = 1_500, totalCents = 1_234)
        receipt("last", purchasedAt = 2_000, totalCents = 659)
        receipt("after", purchasedAt = 2_001, totalCents = 10_000)

        assertEquals(DailySpending(totalCents = 2_345, receiptCount = 3), local().spendingBetween(1_000, 2_000))
    }

    @Test
    fun receiptWithoutATotalYet_notCounted() = runTest {
        receipt("read", purchasedAt = 1_500, totalCents = 452)
        receipt("notRead", purchasedAt = 1_500, totalCents = null)

        assertEquals(DailySpending(totalCents = 452, receiptCount = 1), local().spendingBetween(1_000, 2_000))
    }

    @Test
    fun noReceipts_zero() = runTest {
        assertEquals(DailySpending(totalCents = 0, receiptCount = 0), local().spendingBetween(1_000, 2_000))
    }
}
