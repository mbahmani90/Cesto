package com.majidbahmani.cesto.feature.receipts.data.repository

import com.majidbahmani.cesto.database.CestoDatabase
import com.majidbahmani.cesto.feature.receipts.data.local.ReceiptLocalDataSource
import com.majidbahmani.cesto.feature.receipts.data.parser.ContinenteReceiptParser
import com.majidbahmani.cesto.feature.receipts.data.parser.PDFBOX_RECEIPT
import com.majidbahmani.cesto.feature.receipts.data.remote.ContinenteReceipts
import com.majidbahmani.cesto.feature.receipts.data.remote.GmailNotAuthorizedException
import com.majidbahmani.cesto.feature.receipts.domain.model.ReceiptStatus
import com.majidbahmani.cesto.feature.receipts.domain.model.SyncFailure
import com.majidbahmani.cesto.feature.receipts.domain.model.SyncResult
import com.majidbahmani.cesto.feature.receipts.fake.FakeGmailApi
import com.majidbahmani.cesto.feature.receipts.fake.FakeGmailApi.Mail
import com.majidbahmani.cesto.feature.receipts.fake.FakePdfTextExtractor
import com.majidbahmani.cesto.feature.receipts.fake.FakeReceiptFileStore
import com.majidbahmani.cesto.feature.receipts.fake.createTestDriver
import com.majidbahmani.cesto.gmailauth.GmailAuthError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReceiptRepositoryImplTest {

    private val driver = createTestDriver()
    private val database = CestoDatabase(driver)
    private var gmail = FakeGmailApi(pageSize = 2)
    private val files = FakeReceiptFileStore()
    private val extractor = FakePdfTextExtractor()

    @AfterTest
    fun tearDown() = driver.close()

    private fun TestScope.repository(maxParallelEmails: Int = 4) = ReceiptRepositoryImpl(
        gmail = gmail,
        local = ReceiptLocalDataSource(database, StandardTestDispatcher(testScheduler), currentTimeMillis = { NOW }),
        files = files,
        textExtractor = extractor,
        parser = ContinenteReceiptParser(),
        currentTimeMillis = { NOW },
        maxParallelEmails = maxParallelEmails,
    )

    private fun pdf(text: String) = Base64.UrlSafe.encode(text.encodeToByteArray())

    private fun rows() = database.receiptQueries.selectAll().executeAsList()

    @Test
    fun firstSync_savesAndDownloadsEveryPdf_readingEachEmailOnce() = runTest {
        gmail.mailbox += Mail("m3", receivedAt = 3_000, pdfs = mapOf("1" to pdf("%PDF-3")))
        gmail.mailbox += Mail("m2", receivedAt = 2_000, pdfs = mapOf("1" to pdf("%PDF-2a"), "2" to pdf("%PDF-2b")))
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = emptyMap()) // matched the query, no PDF

        val result = repository().sync(lookBackMonths = 3)

        assertEquals(SyncResult.Success(newReceipts = 3, downloaded = 3), result)
        assertEquals(listOf("m1", "m2", "m3"), gmail.fetchedMessages.sorted()) // each email read once
        assertTrue(rows().all { it.status == "TEXT_EXTRACTED" && it.pdf_path == "receipts/receipt-${it.id}.pdf" })
        assertEquals(setOf("%PDF-3", "%PDF-2a", "%PDF-2b"), files.files.values.map { it.decodeToString() }.toSet())
    }

    @Test
    fun query_usesTheLookBackPeriod() = runTest {
        repository().sync(lookBackMonths = 3)

        assertEquals(ContinenteReceipts.gmailQuery(3), gmail.queries.single())
        assertEquals("from:noreply@cartaocontinente.pt has:attachment filename:pdf newer_than:3m", gmail.queries.single())
    }

    @Test
    fun allResultPages_areRead() = runTest {
        repeat(5) { i -> gmail.mailbox += Mail("m$i", receivedAt = 10L - i, pdfs = mapOf("1" to pdf("%PDF-$i"))) }

        val result = repository().sync(lookBackMonths = 3)

        assertEquals(SyncResult.Success(newReceipts = 5, downloaded = 5), result)
        assertEquals(3, gmail.queries.size) // pages of 2: 2 + 2 + 1
    }

    @Test
    fun atMostMaxParallelEmails_areHandledAtOnce() = runTest {
        gmail = FakeGmailApi(pageSize = 100, requestMillis = 100)
        repeat(10) { i -> gmail.mailbox += Mail("m$i", receivedAt = 100L - i, pdfs = mapOf("1" to pdf("%PDF-$i"))) }

        val result = repository(maxParallelEmails = 3).sync(lookBackMonths = 3)

        assertEquals(SyncResult.Success(newReceipts = 10, downloaded = 10), result)
        assertEquals(3, gmail.maxInFlight)
        // 10 emails × 2 requests × 100 ms in parallels of 3 instead of 2 000 ms one by one
        assertTrue(testScheduler.currentTime <= 800, "took ${testScheduler.currentTime} ms")
    }

    @Test
    fun secondSync_onlyReadsNewEmails() = runTest {
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to pdf("%PDF-1")))
        val repository = repository()
        repository.sync(lookBackMonths = 3)
        gmail.fetchedMessages.clear()

        gmail.mailbox.add(0, Mail("m2", receivedAt = 2_000, pdfs = mapOf("1" to pdf("%PDF-2"))))
        val result = repository.sync(lookBackMonths = 3)

        assertEquals(SyncResult.Success(newReceipts = 1, downloaded = 1), result)
        assertEquals(listOf("m2"), gmail.fetchedMessages)
        assertEquals(2, rows().size)
    }

    @Test
    fun unreadablePdf_isFailed_andNotRetried() = runTest {
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to null))
        val repository = repository()

        assertEquals(SyncResult.Success(newReceipts = 1, downloaded = 0), repository.sync(lookBackMonths = 3))
        assertEquals("FAILED", rows().single().status)

        gmail.fetchedMessages.clear()
        repository.sync(lookBackMonths = 3)
        assertTrue(gmail.fetchedMessages.isEmpty())
    }

    @Test
    fun unreadableEmail_doesNotStopTheOthers_andIsReadNextTime() = runTest {
        gmail.mailbox += Mail("m3", receivedAt = 3_000, pdfs = mapOf("1" to pdf("%PDF-3")))
        gmail.mailbox += Mail("m2", receivedAt = 2_000, pdfs = mapOf("1" to pdf("%PDF-2")))
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to pdf("%PDF-1")))
        val repository = repository()

        gmail.failGetMessage = "m2"
        assertEquals(SyncResult.Success(newReceipts = 2, downloaded = 2, incomplete = 1), repository.sync(lookBackMonths = 3))
        assertEquals(setOf("m1", "m3"), rows().map { it.gmail_message_id }.toSet())

        gmail.failGetMessage = null
        gmail.fetchedMessages.clear()
        assertEquals(SyncResult.Success(newReceipts = 1, downloaded = 1), repository.sync(lookBackMonths = 3))
        assertEquals(listOf("m2"), gmail.fetchedMessages)
    }

    @Test
    fun failedDownload_keepsTheReceipt_andIsRetriedNextSync() = runTest {
        gmail.mailbox += Mail("m2", receivedAt = 2_000, pdfs = mapOf("1" to pdf("%PDF-2")))
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to pdf("%PDF-1"), "2" to pdf("%PDF-1b")))
        val repository = repository()

        gmail.failAttachmentOf = "m1"
        assertEquals(SyncResult.Success(newReceipts = 3, downloaded = 1, incomplete = 2), repository.sync(lookBackMonths = 3))
        assertEquals(listOf("FOUND", "FOUND"), rows().filter { it.gmail_message_id == "m1" }.map { it.status })

        gmail.failAttachmentOf = null
        gmail.fetchedMessages.clear()
        assertEquals(SyncResult.Success(newReceipts = 0, downloaded = 2), repository.sync(lookBackMonths = 3))
        assertEquals(listOf("m1"), gmail.fetchedMessages) // read again only for fresh attachment ids
        assertTrue(rows().all { it.status == "TEXT_EXTRACTED" })
    }

    @Test
    fun failedSearch_failsTheSync_andKeepsSavedReceipts() = runTest {
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to pdf("%PDF-1")))
        val repository = repository()
        repository.sync(lookBackMonths = 3)

        gmail.failList = IllegalStateException("offline")
        assertEquals(SyncResult.Failure(SyncFailure.FAILED), repository.sync(lookBackMonths = 3))
        assertEquals(1, rows().size)
    }

    @Test
    fun revokedAccess_isNotAuthorized() = runTest {
        gmail.failList = GmailNotAuthorizedException(GmailAuthError.NOT_GRANTED)

        assertEquals(SyncResult.Failure(SyncFailure.NOT_AUTHORIZED), repository().sync(lookBackMonths = 3))
    }

    @Test
    fun observeReceipts_newestFirst_asDomainModels() = runTest {
        gmail.mailbox += Mail("m2", receivedAt = 2_000, pdfs = mapOf("1" to pdf("%PDF-2")))
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to null))
        val repository = repository()
        repository.sync(lookBackMonths = 3)

        val receipts = repository.observeReceipts().first()

        assertEquals(listOf(2_000L, 1_000L), receipts.map { it.receivedAtMillis })
        assertEquals(listOf(ReceiptStatus.TEXT_EXTRACTED, ReceiptStatus.FAILED), receipts.map { it.status })
        assertEquals("Fatura_m2-1.pdf", receipts.first().fileName)
    }

    @Test
    fun downloadedPdf_isReadAndParsed_inTheSameSync() = runTest {
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to pdf(PDFBOX_RECEIPT)))

        repository().sync(lookBackMonths = 3)

        val row = rows().single()
        assertEquals("TEXT_EXTRACTED", row.status)
        assertEquals(PDFBOX_RECEIPT, row.text)
        assertEquals(188, row.total_cents)
        assertEquals(1_791_231_720_000, row.purchased_at)
        assertEquals("FS ABC123/000001", row.receipt_number)
        assertEquals("ABCD1234-000001", row.atcud)

        val receipt = repository().observeReceipts().first().single()
        assertEquals(188, receipt.totalCents)
        assertEquals(1_791_231_720_000, receipt.purchasedAtMillis)
    }

    @Test
    fun receiptsDownloadedBeforeThisVersion_areReadFromTheirFile_withoutGmail() = runTest {
        // As left by the previous app version: downloaded, file on the phone, no text.
        database.receiptQueries.insertIfNew("m1", "1", "f.pdf", received_at = 1_000)
        val id = rows().single().id
        database.receiptQueries.markDownloaded("receipts/receipt-$id.pdf", id)
        database.gmailMessageQueries.insert("m1", received_at = 1_000, checked_at = 1_000)
        files.save("receipt-$id.pdf", PDFBOX_RECEIPT.encodeToByteArray())
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to pdf("unused")))

        repository().sync(lookBackMonths = 3)

        assertEquals("TEXT_EXTRACTED", rows().single().status)
        assertEquals(188, rows().single().total_cents)
        assertTrue(gmail.fetchedMessages.isEmpty())
    }

    @Test
    fun pdfWithoutText_orBroken_isFailed_butKeepsItsFile() = runTest {
        gmail.mailbox += Mail("m2", receivedAt = 2_000, pdfs = mapOf("1" to pdf("   "))) // scanned: no text layer
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to pdf("%BROKEN")))

        val result = repository().sync(lookBackMonths = 3)

        assertEquals(SyncResult.Success(newReceipts = 2, downloaded = 2), result)
        assertTrue(rows().all { it.status == "FAILED" && it.pdf_path != null })
    }

    @Test
    fun textWithoutTheFixedLines_isStillExtracted_withEmptyFields() = runTest {
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to pdf("Some other PDF")))

        repository().sync(lookBackMonths = 3)

        val row = rows().single()
        assertEquals("TEXT_EXTRACTED", row.status)
        assertEquals(null, row.total_cents)
    }

    private companion object {
        const val NOW = 9_000L
    }
}
