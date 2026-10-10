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
 * Gmail → database → files → text → items → vectors. Each sync runs these steps in order:
 * 1. Read every email not read before and save its PDFs as FOUND receipts (and mark it checked).
 * 2. Download every FOUND receipt: save the file, extract its text on the phone, parse the fixed fields
 *    (FOUND → DOWNLOADED → TEXT_EXTRACTED).
 * 3. Extract the text of receipts downloaded earlier but never read (e.g. before this app version).
 * 4. With a Gemini key, extract the items (→ READY). Only the receipt's item section is sent to Gemini.
 * 5. New products get a vector for semantic search (only their name and category are sent).
 *
 * - Up to [maxParallelEmails] emails are handled at once; the next starts as soon as one finishes.
 * - Gmail changes attachment ids per request, so they're never stored: step 2 reads the email again first.
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

    /** What one email contributed to the sync. */
    private data class Outcome(val downloaded: Int = 0, val incomplete: Int = 0) {
        operator fun plus(other: Outcome) = Outcome(downloaded + other.downloaded, incomplete + other.incomplete)
    }

    override fun observeReceipts(): Flow<List<Receipt>> = local.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun spendingBetween(fromMillis: Long, toMillis: Long): DailySpending = local.spendingBetween(fromMillis, toMillis)

    override suspend fun sync(lookBackMonths: Int): SyncResult = try {
        val limit = Semaphore(maxParallelEmails)
        val newIds = allMatchingMessageIds(ContinenteReceipts.gmailQuery(lookBackMonths)).filterNot { local.isChecked(it) }
        val read = newIds.inParallel(limit) { saveNewEmail(it) }
        val downloads = local.receiptsWithStatus(ReceiptStatus.FOUND).groupBy { it.gmail_message_id }.toList()
            .inParallel(limit) { (id, receipts) -> downloadFoundReceipts(id, receipts) }
        local.receiptsWithStatus(ReceiptStatus.DOWNLOADED).inParallel(limit) { extractFromFile(it) }
        extractItems(limit) // after the text step, so this sync's new receipts are included
        embedProducts() // after items, so this sync's new products are included
        val outcome = (read + downloads).fold(Outcome(), Outcome::plus)
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

    /** Runs [block] for every item, at most [limit] at once, and waits for all of them. */
    private suspend fun <T, R> List<T>.inParallel(limit: Semaphore, block: suspend (T) -> R): List<R> = coroutineScope {
        map { item -> async { limit.withPermit { block(item) } } }.awaitAll()
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

    /** Saves the email's PDFs as FOUND receipts and marks it checked. Downloading is the next step. */
    private suspend fun saveNewEmail(messageId: String): Outcome = isolated(messageId, onFailure = Outcome(incomplete = 1)) {
        val message = gmail.getMessage(messageId)
        local.saveCheckedMessage(
            messageId = message.id,
            receivedAtMillis = message.receivedAtMillis() ?: currentTimeMillis(),
            pdfs = message.pdfAttachments()
        )
        Outcome()
    }

    /** Reads the email again for fresh attachment ids, then downloads its FOUND receipts. */
    private suspend fun downloadFoundReceipts(messageId: String, receipts: List<ReceiptRow>): Outcome =
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
            pending.inParallel(limit) { extractItemsOf(key, it) }
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
