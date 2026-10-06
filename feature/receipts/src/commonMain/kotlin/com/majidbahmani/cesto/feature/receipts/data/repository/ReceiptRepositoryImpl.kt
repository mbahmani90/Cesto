package com.majidbahmani.cesto.feature.receipts.data.repository

import com.majidbahmani.cesto.core.logging.logWarning
import com.majidbahmani.cesto.database.Receipt as ReceiptRow
import com.majidbahmani.cesto.feature.receipts.data.local.ReceiptFileStore
import com.majidbahmani.cesto.feature.receipts.data.local.ReceiptLocalDataSource
import com.majidbahmani.cesto.feature.receipts.data.mapper.toDomain
import com.majidbahmani.cesto.feature.receipts.data.remote.ContinenteReceipts
import com.majidbahmani.cesto.feature.receipts.data.remote.GmailApi
import com.majidbahmani.cesto.feature.receipts.data.remote.GmailNotAuthorizedException
import com.majidbahmani.cesto.feature.receipts.data.remote.decodeGmailBase64
import com.majidbahmani.cesto.feature.receipts.data.remote.dto.MessageDto
import com.majidbahmani.cesto.feature.receipts.data.remote.pdfAttachments
import com.majidbahmani.cesto.feature.receipts.data.remote.receivedAtMillis
import com.majidbahmani.cesto.feature.receipts.domain.model.Receipt
import com.majidbahmani.cesto.feature.receipts.domain.model.ReceiptStatus
import com.majidbahmani.cesto.feature.receipts.domain.model.SyncFailure
import com.majidbahmani.cesto.feature.receipts.domain.model.SyncResult
import com.majidbahmani.cesto.feature.receipts.domain.repository.ReceiptRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Gmail → database → files.
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
    private val currentTimeMillis: () -> Long,
    private val maxParallelEmails: Int = 4,
) : ReceiptRepository {

    /** What one email (or one retry) contributed to the sync. */
    private data class Outcome(val newReceipts: Int = 0, val downloaded: Int = 0, val incomplete: Int = 0) {
        operator fun plus(other: Outcome) =
            Outcome(newReceipts + other.newReceipts, downloaded + other.downloaded, incomplete + other.incomplete)
    }

    override fun observeReceipts(): Flow<List<Receipt>> =
        local.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun sync(lookBackMonths: Int): SyncResult = try {
        // Read before handling new emails, so this sync's own failures aren't retried twice.
        val leftovers = local.receiptsWithStatus(ReceiptStatus.FOUND).groupBy { it.gmail_message_id }
        val newIds = allMatchingMessageIds(ContinenteReceipts.gmailQuery(lookBackMonths)).filterNot { local.isChecked(it) }

        val limit = Semaphore(maxParallelEmails)
        val outcome = coroutineScope {
            val newEmails = newIds.map { id -> async { limit.withPermit { syncNewEmail(id) } } }
            val retries = leftovers.map { (id, rows) -> async { limit.withPermit { retryDownloads(id, rows) } } }
            (newEmails + retries).awaitAll().fold(Outcome(), Outcome::plus)
        }
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
            pdfs = message.pdfAttachments(),
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
                    Outcome(downloaded = 1)
                }
            }
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
        const val TAG = "ReceiptSync"
    }
}
