package com.majidbahmani.cesto.database

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CestoDatabaseTest {

    private val driver = createTestDriver()
    private val database = CestoDatabase(driver)
    private val receipts = database.receiptQueries
    private val messages = database.gmailMessageQueries

    @AfterTest
    fun tearDown() = driver.close()

    @Test
    fun newReceipt_isFoundWithoutFile() {
        receipts.insertIfNew("msg-1", "2", "talao.pdf", received_at = 1_000)

        val receipt = receipts.selectAll().executeAsOne()
        assertEquals("FOUND", receipt.status)
        assertNull(receipt.pdf_path)
    }

    @Test
    fun sameMessageAndPart_isInsertedOnce_andKeepsItsStatus() {
        receipts.insertIfNew("msg-1", "2", "talao.pdf", received_at = 1_000)
        val id = receipts.selectAll().executeAsOne().id
        receipts.markDownloaded("receipts/1.pdf", id)

        receipts.insertIfNew("msg-1", "2", "talao.pdf", received_at = 1_000) // second sync

        val receipt = receipts.selectAll().executeAsOne()
        assertEquals("DOWNLOADED", receipt.status)
        assertEquals("receipts/1.pdf", receipt.pdf_path)
    }

    @Test
    fun twoPdfsInOneMessage_areTwoReceipts() {
        receipts.insertIfNew("msg-1", "2", "a.pdf", received_at = 1_000)
        receipts.insertIfNew("msg-1", "3", "b.pdf", received_at = 1_000)

        assertEquals(2, receipts.selectAll().executeAsList().size)
    }

    @Test
    fun selectAll_newestFirst_andSelectByStatus() {
        receipts.insertIfNew("old", "1", "old.pdf", received_at = 1_000)
        receipts.insertIfNew("new", "1", "new.pdf", received_at = 2_000)
        val newId = receipts.selectAll().executeAsList().first().id
        receipts.updateStatus("FAILED", newId)

        assertEquals(listOf("new", "old"), receipts.selectAll().executeAsList().map { it.gmail_message_id })
        assertEquals(listOf("new"), receipts.selectByStatus("FAILED").executeAsList().map { it.gmail_message_id })
    }

    @Test
    fun checkedMessages_rememberIdsAndNewestDate() {
        assertNull(messages.latestReceivedAt().executeAsOne().MAX)

        messages.insert("msg-1", received_at = 1_000, checked_at = 5_000)
        messages.insert("msg-2", received_at = 3_000, checked_at = 5_000)
        messages.insert("msg-1", received_at = 1_000, checked_at = 9_000) // ignored

        assertTrue(messages.isChecked("msg-1").executeAsOne())
        assertFalse(messages.isChecked("msg-3").executeAsOne())
        assertEquals(3_000, messages.latestReceivedAt().executeAsOne().MAX)
    }

    @Test
    fun deleteAll_removesEverything() {
        receipts.insertIfNew("msg-1", "2", "talao.pdf", received_at = 1_000)
        messages.insert("msg-1", received_at = 1_000, checked_at = 5_000)

        receipts.deleteAll()
        messages.deleteAll()

        assertTrue(receipts.selectAll().executeAsList().isEmpty())
        assertFalse(messages.isChecked("msg-1").executeAsOne())
    }
}
