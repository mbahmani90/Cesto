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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Gmail → database → files → text → items → vectors. For each receipt: download the PDF, save the file,
 * extract its text on the phone, parse the fixed fields, save them (FOUND → DOWNLOADED → TEXT_EXTRACTED);
 * then, with a Gemini key, extract its items (→ READY). Only the receipt's item section is sent to Gemini.
 * Last, new products get a vector for semantic search (only their name and category are sent).
 *
 * - Up to [maxParallelEmails] emails are handled at once; the next starts as soon as one finishes.
 * - Each email is read once: its PDFs are downloaded right away with the attachment ids from that
 *   same response (Gmail changes attachment ids per request, so they're never stored).
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
    private val maxParallelEmails: Int = 4
) : ReceiptRepository {

    /** What one email (or one retry) contributed to the sync. */
    private data class Outcome(val newReceipts: Int = 0, val downloaded: Int = 0, val incomplete: Int = 0) {
        operator fun plus(other: Outcome) =
            Outcome(newReceipts + other.newReceipts, downloaded + other.downloaded, incomplete + other.incomplete)
    }

    override fun observeReceipts(): Flow<List<Receipt>> = local.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun spendingBetween(fromMillis: Long, toMillis: Long): DailySpending = local.spendingBetween(fromMillis, toMillis)

    override suspend fun sync(lookBackMonths: Int): SyncResult = try {
        // Read before handling new emails, so this sync's own failures aren't retried twice.
        val leftovers = local.receiptsWithStatus(ReceiptStatus.FOUND).groupBy { it.gmail_message_id }
        val unread = local.receiptsWithStatus(ReceiptStatus.DOWNLOADED) // text not extracted yet (or app updated)
        val newIds = allMatchingMessageIds(ContinenteReceipts.gmailQuery(lookBackMonths)).filterNot { local.isChecked(it) }

        val limit = Semaphore(maxParallelEmails)
        val outcome = coroutineScope {
            val newEmails = newIds.map { id -> async { limit.withPermit { syncNewEmail(id) } } }
            val retries = leftovers.map { (id, rows) -> async { limit.withPermit { retryDownloads(id, rows) } } }
            val texts = unread.map { receipt -> async { limit.withPermit { extractFromFile(receipt) } } }
            (newEmails + retries + texts).awaitAll().fold(Outcome(), Outcome::plus)
        }
        extractItems(limit) // after the text step, so this sync's new receipts are included
        embedProducts() // after items, so this sync's new products are included
        SyncResult.Success(outcome.newReceipts, outcome.downloaded, outcome.incomplete)
    } catch (e: CancellationException) {
        throw e
    } catch (e: GmailNotAuthorizedException) {
        logWarning(TAG, "sync: Gmail not authorized (${e.reason})")
        SyncResult.Failure(SyncFailure.NOT_AUTHORIZED)
    } catch (e: Exception) {
        logWarning(TAG, "sync: Gmail search failed", e)
        SyncResult.Failure(SyncFailure.FAILED)
    }

    /** Ids only (cheap): a few receipts a week stay well under [MAX_PAGES] pages. */
    private suspend fun allMatchingMessageIds(query: String): List<String> {
        val ids = mutableListOf<String>()
        var pageToken: String? = null
        repeat(MAX_PAGES) {
            val page = gmail.listMessages(query, pageToken)
            ids += page.messages.map { it.id }
            pageToken = page.nextPageToken ?: return ids
        }
        return ids
    }

    /** Read the email once, save its receipts (and mark it checked), then download them. */
    private suspend fun syncNewEmail(messageId: String): Outcome = isolated(messageId, onFailure = Outcome(incomplete = 1)) {
        val message = gmail.getMessage(messageId)
        val newReceipts = local.saveCheckedMessage(
            messageId = message.id,
            receivedAtMillis = message.receivedAtMillis() ?: currentTimeMillis(),
            pdfs = message.pdfAttachments()
        )
        val pending = local.receiptsOfMessage(message.id).filter { it.status == ReceiptStatus.FOUND.name }
        download(message, pending).copy(newReceipts = newReceipts)
    }

    /** Receipts left FOUND by an earlier sync: read their email again for fresh attachment ids. */
    private suspend fun retryDownloads(messageId: String, receipts: List<ReceiptRow>): Outcome =
        isolated(messageId, onFailure = Outcome(incomplete = receipts.size)) {
            download(gmail.getMessage(messageId), receipts)
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
    private suspend fun extractItems(limit: Semaphore) {
        val key = geminiKeys.key.first() ?: return
        val pending = local.receiptsWithStatus(ReceiptStatus.TEXT_EXTRACTED)
        try {
            coroutineScope {
                pending.map { receipt -> async { limit.withPermit { extractItemsOf(key, receipt) } } }.awaitAll()
            }
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
