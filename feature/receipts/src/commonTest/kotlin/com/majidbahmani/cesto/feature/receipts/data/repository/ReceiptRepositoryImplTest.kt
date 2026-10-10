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
import com.majidbahmani.cesto.feature.receipts.fake.FakeEmbeddingProvider
import com.majidbahmani.cesto.feature.receipts.fake.FakeGeminiKeyStore
import com.majidbahmani.cesto.feature.receipts.fake.FakeGmailApi
import com.majidbahmani.cesto.feature.receipts.fake.FakeGmailApi.Mail
import com.majidbahmani.cesto.feature.receipts.fake.FakePdfTextExtractor
import com.majidbahmani.cesto.feature.receipts.fake.FakeReceiptFileStore
import com.majidbahmani.cesto.feature.receipts.fake.FakeReceiptItemExtractor
import com.majidbahmani.cesto.feature.receipts.fake.createTestDriver
import com.majidbahmani.cesto.gmailauth.GmailAuthError
import com.majidbahmani.cesto.llm.embedding.EmbeddingUnavailableException
import kotlin.io.encoding.Base64
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class ReceiptRepositoryImplTest {

    private val driver = createTestDriver()
    private val database = CestoDatabase(driver)
    private var gmail = FakeGmailApi(pageSize = 2)
    private val files = FakeReceiptFileStore()
    private val extractor = FakePdfTextExtractor()
    private val itemExtractor = FakeReceiptItemExtractor()
    private val embeddings = FakeEmbeddingProvider()
    private val geminiKeys = FakeGeminiKeyStore(initial = null) // no key: item extraction skipped unless a test sets one

    @AfterTest
    fun tearDown() = driver.close()

    private fun TestScope.repository(batchSize: Int = 4) = ReceiptRepositoryImpl(
        gmail = gmail,
        local = ReceiptLocalDataSource(database, StandardTestDispatcher(testScheduler), currentTimeMillis = { NOW }),
        files = files,
        textExtractor = extractor,
        parser = ContinenteReceiptParser(),
        itemExtractor = itemExtractor,
        embeddings = embeddings,
        geminiKeys = geminiKeys,
        currentTimeMillis = { NOW },
        batchSize = batchSize
    )

    private fun pdf(text: String) = Base64.UrlSafe.encode(text.encodeToByteArray())

    private fun rows() = database.receiptQueries.selectAll().executeAsList()

    @Test
    fun firstSync_savesAndDownloadsEveryPdf_readingEachEmailOnce() = runTest {
        gmail.mailbox += Mail("m3", receivedAt = 3_000, pdfs = mapOf("1" to pdf("%PDF-3")))
        gmail.mailbox += Mail("m2", receivedAt = 2_000, pdfs = mapOf("1" to pdf("%PDF-2a"), "2" to pdf("%PDF-2b")))
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = emptyMap()) // matched the query, no PDF

        val result = repository().sync(lookBackMonths = 3)

        assertEquals(SyncResult.Success(downloaded = 3), result)
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

        assertEquals(SyncResult.Success(downloaded = 5), result)
        assertEquals(3, gmail.queries.size) // pages of 2: 2 + 2 + 1
    }

    @Test
    fun emails_areHandledInBatches() = runTest {
        gmail = FakeGmailApi(pageSize = 100, requestMillis = 100)
        repeat(10) { i -> gmail.mailbox += Mail("m$i", receivedAt = 100L - i, pdfs = mapOf("1" to pdf("%PDF-$i"))) }

        val result = repository(batchSize = 3).sync(lookBackMonths = 3)

        assertEquals(SyncResult.Success(downloaded = 10), result)
        assertEquals(3, gmail.maxInFlight)
        // 10 emails × 2 requests × 100 ms in batches of 3, 3, 3, 1 instead of 2 000 ms one by one
        assertEquals(800, testScheduler.currentTime)
    }

    @Test
    fun secondSync_onlyReadsNewEmails() = runTest {
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to pdf("%PDF-1")))
        val repository = repository()
        repository.sync(lookBackMonths = 3)
        gmail.fetchedMessages.clear()

        gmail.mailbox.add(0, Mail("m2", receivedAt = 2_000, pdfs = mapOf("1" to pdf("%PDF-2"))))
        val result = repository.sync(lookBackMonths = 3)

        assertEquals(SyncResult.Success(downloaded = 1), result)
        assertEquals(listOf("m2"), gmail.fetchedMessages)
        assertEquals(2, rows().size)
    }

    @Test
    fun unreadablePdf_isFailed_andNotRetried() = runTest {
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to null))
        val repository = repository()

        assertEquals(SyncResult.Success(downloaded = 0), repository.sync(lookBackMonths = 3))
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
        assertEquals(SyncResult.Success(downloaded = 2, incomplete = 1), repository.sync(lookBackMonths = 3))
        assertEquals(setOf("m1", "m3"), rows().map { it.gmail_message_id }.toSet())

        gmail.failGetMessage = null
        gmail.fetchedMessages.clear()
        assertEquals(SyncResult.Success(downloaded = 1), repository.sync(lookBackMonths = 3))
        assertEquals(listOf("m2"), gmail.fetchedMessages)
    }

    @Test
    fun failedDownload_keepsTheReceipt_andIsRetriedNextSync() = runTest {
        gmail.mailbox += Mail("m2", receivedAt = 2_000, pdfs = mapOf("1" to pdf("%PDF-2")))
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to pdf("%PDF-1"), "2" to pdf("%PDF-1b")))
        val repository = repository()

        gmail.failAttachmentOf = "m1"
        assertEquals(SyncResult.Success(downloaded = 1, incomplete = 2), repository.sync(lookBackMonths = 3))
        assertEquals(listOf("FOUND", "FOUND"), rows().filter { it.gmail_message_id == "m1" }.map { it.status })

        gmail.failAttachmentOf = null
        gmail.fetchedMessages.clear()
        assertEquals(SyncResult.Success(downloaded = 2), repository.sync(lookBackMonths = 3))
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

        assertEquals(SyncResult.Success(downloaded = 2), result)
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

    @Test
    fun withoutGeminiKey_receiptsStayAtText_andNothingIsSent() = runTest {
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to pdf(PDFBOX_RECEIPT)))

        repository().sync(lookBackMonths = 3)

        assertEquals("TEXT_EXTRACTED", rows().single().status)
        assertTrue(itemExtractor.sentSections.isEmpty())
    }

    @Test
    fun withGeminiKey_itemsAreSaved_onlyTheSectionIsSent_productsShared() = runTest {
        geminiKeys.key.value = "AIza-test"
        gmail.mailbox += Mail("m2", receivedAt = 2_000, pdfs = mapOf("1" to pdf(PDFBOX_RECEIPT)))
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to pdf(PDFBOX_RECEIPT)))

        repository().sync(lookBackMonths = 3)

        assertTrue(rows().all { it.status == "READY" })
        assertEquals(2, itemExtractor.sentSections.size)
        itemExtractor.sentSections.forEach { section ->
            listOf("NIF", "Cartao cliente", "ATCUD", "TOTAL A PAGAR").forEach { assertTrue(it !in section, it) }
        }
        assertEquals(2, database.productQueries.count().executeAsOne()) // milk + banana, once each
        assertEquals(4, database.receiptItemQueries.count().executeAsOne()) // 2 lines × 2 receipts
        val banana = database.receiptItemQueries.selectByReceipt(rows().first().id).executeAsList()[1]
        assertEquals("KG", banana.unit)
        assertEquals(90, banana.line_total_cents)
    }

    @Test
    fun keyAddedLater_extractsReceiptsReadBefore_withoutGmail() = runTest {
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to pdf(PDFBOX_RECEIPT)))
        val repository = repository()
        repository.sync(lookBackMonths = 3)
        gmail.fetchedMessages.clear()

        geminiKeys.key.value = "AIza-test"
        repository.sync(lookBackMonths = 3)

        assertEquals("READY", rows().single().status)
        assertTrue(gmail.fetchedMessages.isEmpty())
    }

    @Test
    fun geminiLoop_goesThroughEveryBatch_skippingFailuresUntilTheNextSync() = runTest {
        geminiKeys.key.value = "AIza-test"
        itemExtractor.failFor = "BROKEN LINE"
        repeat(5) { i ->
            val text = if (i == 1) PDFBOX_RECEIPT.replace("BANANA", "BROKEN LINE") else PDFBOX_RECEIPT
            gmail.mailbox += Mail("m$i", receivedAt = 10L - i, pdfs = mapOf("1" to pdf(text)))
        }

        repository(batchSize = 2).sync(lookBackMonths = 3)

        assertEquals(4, rows().count { it.status == "READY" })
        assertEquals("TEXT_EXTRACTED", rows().single { it.gmail_message_id == "m1" }.status)
    }

    @Test
    fun quotaOrRejectedKey_keepsReceiptsAtText_forTheNextSync() = runTest {
        geminiKeys.key.value = "AIza-test"
        itemExtractor.unavailable = true
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to pdf(PDFBOX_RECEIPT)))

        val result = repository().sync(lookBackMonths = 3)

        assertEquals(SyncResult.Success(downloaded = 1), result) // the sync itself worked
        assertEquals("TEXT_EXTRACTED", rows().single().status)
    }

    @Test
    fun oneBadExtraction_doesNotStopTheOthers() = runTest {
        geminiKeys.key.value = "AIza-test"
        itemExtractor.failFor = "BROKEN LINE"
        gmail.mailbox += Mail("m2", receivedAt = 2_000, pdfs = mapOf("1" to pdf(PDFBOX_RECEIPT)))
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to pdf(PDFBOX_RECEIPT.replace("BANANA", "BROKEN LINE"))))

        repository().sync(lookBackMonths = 3)

        assertEquals(listOf("READY", "TEXT_EXTRACTED"), rows().map { it.status }) // newest first: m2, m1
    }

    @Test
    fun noItemSection_nothingIsSent() = runTest {
        geminiKeys.key.value = "AIza-test"
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to pdf("Some other PDF")))

        repository().sync(lookBackMonths = 3)

        assertTrue(itemExtractor.sentSections.isEmpty())
        assertEquals("TEXT_EXTRACTED", rows().single().status)
    }

    @Test
    fun newProducts_getAVector_onlyNameAndCategoryAreSent() = runTest {
        geminiKeys.key.value = "AIza-test"
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to pdf(PDFBOX_RECEIPT)))

        repository().sync(lookBackMonths = 3)

        assertEquals(
            listOf("Leite pasteurizado gordo 1 L. Categoria: Laticinios", "Banana. Categoria: Frutas e Legumes"),
            embeddings.sentTexts
        )
        assertEquals(2, database.productEmbeddingQueries.count().executeAsOne())
    }

    @Test
    fun productsBoughtAgain_areNotEmbeddedAgain() = runTest {
        geminiKeys.key.value = "AIza-test"
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to pdf(PDFBOX_RECEIPT)))
        val repository = repository()
        repository.sync(lookBackMonths = 3)
        embeddings.sentTexts.clear()

        gmail.mailbox += Mail("m2", receivedAt = 2_000, pdfs = mapOf("1" to pdf(PDFBOX_RECEIPT))) // same milk and banana
        repository.sync(lookBackMonths = 3)

        assertEquals(2, rows().count { it.status == "READY" })
        assertTrue(embeddings.sentTexts.isEmpty())
    }

    @Test
    fun newEmbeddingModel_embedsEverythingAgain_andDropsTheOldVectors() = runTest {
        geminiKeys.key.value = "AIza-test"
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to pdf(PDFBOX_RECEIPT)))
        val repository = repository()
        repository.sync(lookBackMonths = 3)

        embeddings.modelId = "other@2"
        repository.sync(lookBackMonths = 3)

        assertEquals(4, embeddings.sentTexts.size) // 2 products × 2 models
        assertEquals(2, database.productEmbeddingQueries.count().executeAsOne())
        assertEquals(2, database.productEmbeddingQueries.vectorsForModel("other@2").executeAsList().size)
    }

    @Test
    fun quotaForVectors_keepsTheItems_andTriesAgainNextSync() = runTest {
        geminiKeys.key.value = "AIza-test"
        embeddings.unavailable = EmbeddingUnavailableException.Reason.QUOTA
        gmail.mailbox += Mail("m1", receivedAt = 1_000, pdfs = mapOf("1" to pdf(PDFBOX_RECEIPT)))
        val repository = repository()

        val result = repository.sync(lookBackMonths = 3)

        assertEquals(SyncResult.Success(downloaded = 1), result)
        assertEquals("READY", rows().single().status)
        assertEquals(0, database.productEmbeddingQueries.count().executeAsOne())

        embeddings.unavailable = null
        repository.sync(lookBackMonths = 3)
        assertEquals(2, database.productEmbeddingQueries.count().executeAsOne())
    }

    @Test
    fun withoutGeminiKey_noVectorsAreMade() = runTest {
        database.productQueries.insertIfNew("BANANA", "Banana", null, 1) // e.g. key removed after extraction

        repository().sync(lookBackMonths = 3)

        assertTrue(embeddings.sentTexts.isEmpty())
    }

    private companion object {
        const val NOW = 9_000L
    }
}
