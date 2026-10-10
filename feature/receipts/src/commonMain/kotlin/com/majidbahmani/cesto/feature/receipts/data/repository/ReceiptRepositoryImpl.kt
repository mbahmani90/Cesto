package com.majidbahmani.cesto.feature.receipts.data.repository

import com.majidbahmani.cesto.core.logging.logWarning
import com.majidbahmani.cesto.database.Receipt as ReceiptRow
import com.majidbahmani.cesto.feature.receipts.data.embedding.ProductText
import com.majidbahmani.cesto.feature.receipts.data.extraction.ExtractionUnavailableException
import com.majidbahmani.cesto.feature.receipts.data.extraction.ReceiptItemExtractor
import com.majidbahmani.cesto.feature.receipts.data.local.PdfTextExtractor
import com.majidbahmani.cesto.feature.receipts.data.local.ReceiptFileStore
import com.majidbahmani.cesto.feature.receipts.data.local.ReceiptLocalDataSource
import com.majidbahmani.cesto.feature.receipts.data.mapper.toDomain
import com.majidbahmani.cesto.feature.receipts.data.parser.ContinenteReceiptParser
import com.majidbahmani.cesto.feature.receipts.data.parser.itemSection
import com.majidbahmani.cesto.feature.receipts.data.parser.subtotalCents
import com.majidbahmani.cesto.feature.receipts.data.remote.ContinenteReceipts
import com.majidbahmani.cesto.feature.receipts.data.remote.GmailApi
import com.majidbahmani.cesto.feature.receipts.data.remote.GmailNotAuthorizedException
import com.majidbahmani.cesto.feature.receipts.data.remote.decodeGmailBase64
import com.majidbahmani.cesto.feature.receipts.data.remote.dto.MessageDto
import com.majidbahmani.cesto.feature.receipts.data.remote.pdfAttachments
import com.majidbahmani.cesto.feature.receipts.data.remote.receivedAtMillis
import com.majidbahmani.cesto.feature.receipts.domain.model.DailySpending
import com.majidbahmani.cesto.feature.receipts.domain.model.Receipt
import com.majidbahmani.cesto.feature.receipts.domain.model.ReceiptStatus
import com.majidbahmani.cesto.feature.receipts.domain.model.SyncFailure
import com.majidbahmani.cesto.feature.receipts.domain.model.SyncResult
import com.majidbahmani.cesto.feature.receipts.domain.repository.ReceiptRepository
import com.majidbahmani.cesto.llm.GeminiKeyStore
import com.majidbahmani.cesto.llm.embedding.EmbeddingProvider
import com.majidbahmani.cesto.llm.embedding.EmbeddingUnavailableException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Gmail → database → files → text → items → vectors. Each sync runs two loops, then the vectors:
 * 1. Gmail loop, page by page. Of each page of ids, the emails not read before, or with a receipt still
 *    waiting for its download, are handled [batchSize] at a time: read the email, save its PDFs as FOUND
 *    receipts (and mark it checked), download them with the attachment ids from that same response,
 *    save the file, extract its text on the phone and parse the fixed fields (→ DOWNLOADED → TEXT_EXTRACTED).
 *    The next page is fetched when every batch of this one is done.
 * 2. Gemini loop: with a Gemini key, the items of every receipt with text, [batchSize] at a time (→ READY).
 *    Only the receipt's item section is sent to Gemini.
 * Last, new products get a vector for semantic search (only their name and category are sent).
 *
 * - A batch runs in parallel; the next batch starts when all of it is done.
 * - Every email and PDF succeeds or fails on its own. Failures are retried by the next sync: an
 *   unread email stays unchecked, an undownloaded receipt stays FOUND. Only a failed search, or
 *   revoked access, fails the whole sync.
 */
class ReceiptRepositoryImpl(
    private val gmail: GmailApi,
    private val local: ReceiptLocalDataSource,
    private val files: ReceiptFileStore,
    private val textExtractor: PdfTextExtractor,
    private val parser: ContinenteReceiptParser,
    private val itemExtractor: ReceiptItemExtractor,
    private val embeddings: EmbeddingProvider,
    private val geminiKeys: GeminiKeyStore,
    private val currentTimeMillis: () -> Long,
    private val batchSize: Int = 4
) : ReceiptRepository {

    /** What one email contributed to the sync. */
    private data class Outcome(val downloaded: Int = 0, val incomplete: Int = 0) {
        operator fun plus(other: Outcome) = Outcome(downloaded + other.downloaded, incomplete + other.incomplete)
    }

    override fun observeReceipts(): Flow<List<Receipt>> = local.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun spendingBetween(fromMillis: Long, toMillis: Long): DailySpending = local.spendingBetween(fromMillis, toMillis)

    override suspend fun sync(lookBackMonths: Int): SyncResult = try {
        val outcome = syncEmails(ContinenteReceipts.gmailQuery(lookBackMonths))
        forEachBatchWithStatus(ReceiptStatus.DOWNLOADED) { extractFromFile(it) } // downloaded but never read
        extractItems() // after the Gmail loop, so this sync's new receipts are included
        embedProducts() // after items, so this sync's new products are included
        SyncResult.Success(outcome.downloaded, outcome.incomplete)
    } catch (e: CancellationException) {
        throw e
    } catch (e: GmailNotAuthorizedException) {
        logWarning(TAG, "sync: Gmail not authorized (${e.reason})")
        SyncResult.Failure(SyncFailure.NOT_AUTHORIZED)
    } catch (e: Exception) {
        logWarning(TAG, "sync: Gmail search failed", e)
        SyncResult.Failure(SyncFailure.FAILED)
    }

    /** The Gmail loop. A page is ids only (cheap): a few receipts a week stay well under [MAX_PAGES] pages. */
    private suspend fun syncEmails(query: String): Outcome {
        var outcome = Outcome()
        var pageToken: String? = null
        repeat(MAX_PAGES) {
            // Fetch the email items with PAGE_SIZE
            val page = gmail.listMessages(query, pageToken)
            page.messages.map {
                it.id
            }.filter {
                // Drop the last items which are already downloading
                local.needsSync(it)
            }.chunked(batchSize).forEach { batch ->
                // Chunk the receipts to batchSize and get the content and download of each chuck sequentially
                outcome = batch.inParallel {
                    syncEmail(it)
                }.fold(outcome, Outcome::plus)
            }
            pageToken = page.nextPageToken ?: return outcome
        }
        return outcome
    }

    /** Reads the email once: saves its PDFs as FOUND receipts, then downloads them with the same attachment ids. */
    private suspend fun syncEmail(messageId: String): Outcome = isolated(messageId, onFailure = Outcome(incomplete = 1)) {
        // Get the content of each email by id
        val message = gmail.getMessage(messageId)
        // Save the emailId and attachmentId and datetime stamp
        local.saveCheckedMessage(
            messageId = message.id,
            receivedAtMillis = message.receivedAtMillis() ?: currentTimeMillis(),
            pdfs = message.pdfAttachments()
        )
        // Download the attachment file by id
        download(message, local.foundReceiptsOf(message.id))
    }

    /**
     * Every receipt with [status], [batchSize] at a time, in id order. A receipt [block] leaves in [status]
     * (e.g. after a failure) isn't picked again this sync.
     */
    private suspend fun forEachBatchWithStatus(status: ReceiptStatus, block: suspend (ReceiptRow) -> Unit) {
        var lastId = 0L
        while (true) {
            val batch = local.receiptsWithStatus(status, afterId = lastId, limit = batchSize)
            if (batch.isEmpty()) return
            batch.inParallel(block)
            lastId = batch.last().id
        }
    }

    /** Runs [block] for every item at once and waits for all of them. */
    private suspend fun <T, R> List<T>.inParallel(block: suspend (T) -> R): List<R> = coroutineScope {
        map { item -> async { block(item) } }.awaitAll()
    }

    private suspend fun download(message: MessageDto, receipts: List<ReceiptRow>): Outcome {
        val attachments = message.pdfAttachments().associateBy { it.partId }
        return receipts.fold(Outcome()) { total, receipt ->
            total + isolated(message.id, onFailure = Outcome(incomplete = 1)) {
                val bytes = attachments[receipt.gmail_part_id]
                    ?.let { gmail.getAttachment(message.id, it.attachmentId).data }
                    ?.let { runCatching { decodeGmailBase64(it) }.getOrNull() }
                    ?.takeIf { it.isNotEmpty() }
                if (bytes == null) {
                    local.markFailed(receipt.id) // gone from the email or unreadable: retrying won't help
                    Outcome()
                } else {
                    local.markDownloaded(receipt.id, files.save("receipt-${receipt.id}.pdf", bytes))
                    extractText(receipt.id, bytes) // the bytes are already here: no need to read the file back
                    Outcome(downloaded = 1)
                }
            }
        }
    }

    /** Downloaded earlier (e.g. before this app version): read the saved file, no Gmail request. */
    private suspend fun extractFromFile(receipt: ReceiptRow): Outcome = isolated(receipt.gmail_message_id, onFailure = Outcome()) {
        val path = receipt.pdf_path
        if (path == null) local.markFailed(receipt.id) else extractText(receipt.id, files.read(path))
        Outcome()
    }

    /**
     * PDF → text → fields. Failures here are permanent (same file, same result), so the receipt is
     * marked FAILED instead of retried; its PDF stays on the phone.
     */
    private suspend fun extractText(receiptId: Long, pdf: ByteArray) {
        val text = try {
            textExtractor.extractText(pdf)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logWarning(TAG, "sync: receipt $receiptId: PDF text unreadable", e)
            null
        }
        if (text.isNullOrBlank()) {
            local.markFailed(receiptId) // no text layer (scanned) or broken PDF: needs OCR, later
        } else {
            local.markTextExtracted(receiptId, text, parser.parse(text))
        }
    }

    /**
     * Gemini reads the item section of every receipt with text (TEXT_EXTRACTED). Skipped without a key.
     * A rejected key or used-up quota stops this step; the rest waits for the next sync.
     */
    private suspend fun extractItems() {
        val key = geminiKeys.key.first() ?: return
        try {
            forEachBatchWithStatus(ReceiptStatus.TEXT_EXTRACTED) { extractItemsOf(key, it) }
        } catch (e: ExtractionUnavailableException) {
            // The cause carries Gemini's error text (e.g. which quota was hit): Google's message, no receipt data.
            logWarning(TAG, "sync: item extraction paused (${e.message}); retried next sync", e.cause)
        }
    }

    private suspend fun extractItemsOf(key: String, receipt: ReceiptRow) {
        val text = receipt.text ?: return
        val section = itemSection(text)
        if (section == null) {
            logWarning(TAG, "sync: receipt ${receipt.id}: no item section found, nothing sent")
            return
        }
        try {
            val lines = itemExtractor.extract(key, section)
            // Without a discount there's no SUBTOTAL line, and TOTAL A PAGAR is the sum of the lines.
            val expected = subtotalCents(text) ?: receipt.total_cents
            val sum = lines.sumOf { it.lineTotalCents }
            if (expected != null && sum != expected) {
                // Kept, but visible in the log: the cheapest way to find bad extractions.
                logWarning(TAG, "sync: receipt ${receipt.id}: lines add up to $sum, receipt says $expected")
            }
            local.saveItems(receipt.id, lines)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ExtractionUnavailableException) {
            throw e
        } catch (e: Exception) {
            logWarning(TAG, "sync: receipt ${receipt.id}: item extraction failed, retried next sync", e)
        }
    }

    /**
     * Vectors for semantic search, only for products that have none (or whose text changed): a product bought
     * every week is embedded once. Batches of [EMBED_BATCH] are saved one by one, so a used-up quota keeps
     * what's done and the next sync continues. Skipped without a key.
     */
    private suspend fun embedProducts() {
        geminiKeys.key.first() ?: return
        val model = embeddings.modelId
        try {
            local.productsToEmbed(model).chunked(EMBED_BATCH).forEach { batch: List<ProductText> ->
                val vectors = embeddings.embedDocuments(batch.map { it.text })
                local.saveEmbeddings(model, batch.zip(vectors))
            }
            local.deleteEmbeddingsOfOtherModels(model) // every product has a vector of this model now
        } catch (e: CancellationException) {
            throw e
        } catch (e: EmbeddingUnavailableException) {
            logWarning(TAG, "sync: product vectors paused (${e.reason}); retried next sync", e.cause)
        } catch (e: Exception) {
            logWarning(TAG, "sync: product vectors failed; retried next sync", e)
        }
    }

    /** Runs one unit of work; a failure only affects this unit (cancellation and lost access still stop the sync). */
    private inline fun isolated(messageId: String, onFailure: Outcome, block: () -> Outcome): Outcome = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: GmailNotAuthorizedException) {
        throw e
    } catch (e: Exception) {
        logWarning(TAG, "sync: email $messageId not completed, retried next sync", e)
        onFailure
    }

    private companion object {
        const val MAX_PAGES = 10

        /** Products per embedding request (Gemini's batch limit). */
        const val EMBED_BATCH = 100
        const val TAG = "ReceiptSync"
    }
}
