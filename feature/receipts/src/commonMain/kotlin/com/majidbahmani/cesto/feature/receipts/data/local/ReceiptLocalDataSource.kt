package com.majidbahmani.cesto.feature.receipts.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.majidbahmani.cesto.database.CestoDatabase
import com.majidbahmani.cesto.database.Receipt as ReceiptRow
import com.majidbahmani.cesto.database.toBlob
import com.majidbahmani.cesto.feature.receipts.data.embedding.ProductText
import com.majidbahmani.cesto.feature.receipts.data.embedding.productEmbedText
import com.majidbahmani.cesto.feature.receipts.data.extraction.ExtractedLine
import com.majidbahmani.cesto.feature.receipts.data.parser.ParsedReceipt
import com.majidbahmani.cesto.feature.receipts.data.remote.PdfAttachmentRef
import com.majidbahmani.cesto.feature.receipts.domain.model.DailySpending
import com.majidbahmani.cesto.feature.receipts.domain.model.ReceiptStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** SQLite calls are blocking: every call here switches to [ioDispatcher] (doc 14). */
class ReceiptLocalDataSource(
    private val database: CestoDatabase,
    private val ioDispatcher: CoroutineDispatcher,
    private val currentTimeMillis: () -> Long
) {
    private val receipts = database.receiptQueries
    private val messages = database.gmailMessageQueries
    private val products = database.productQueries
    private val items = database.receiptItemQueries
    private val embeddings = database.productEmbeddingQueries
    private val insights = database.insightsQueries

    fun observeAll(): Flow<List<ReceiptRow>> = receipts.selectAll().asFlow().mapToList(ioDispatcher)

    suspend fun isChecked(messageId: String): Boolean = withContext(ioDispatcher) {
        messages.isChecked(messageId).executeAsOne()
    }

    /**
     * Saves the message's PDFs as receipts and marks the message as checked, in one transaction:
     * an interrupted sync never leaves a message "checked" without its receipts.
     * @return how many receipts are new.
     */
    suspend fun saveCheckedMessage(messageId: String, receivedAtMillis: Long, pdfs: List<PdfAttachmentRef>): Int =
        withContext(ioDispatcher) {
            database.transactionWithResult {
                pdfs.sumOf { pdf ->
                    receipts.insertIfNew(messageId, pdf.partId, pdf.fileName, receivedAtMillis)
                    receipts.changes().executeAsOne().toInt()
                }.also { messages.insert(messageId, receivedAtMillis, currentTimeMillis()) }
            }
        }

    suspend fun receiptsOfMessage(messageId: String): List<ReceiptRow> = withContext(ioDispatcher) {
        receipts.selectByMessage(messageId).executeAsList()
    }

    suspend fun receiptsWithStatus(status: ReceiptStatus): List<ReceiptRow> = withContext(ioDispatcher) {
        receipts.selectByStatus(status.name).executeAsList()
    }

    suspend fun markDownloaded(id: Long, pdfPath: String) = withContext(ioDispatcher) {
        receipts.markDownloaded(pdfPath, id)
    }

    /** Text and its parsed fields in one statement, status TEXT_EXTRACTED. */
    suspend fun markTextExtracted(id: Long, text: String, parsed: ParsedReceipt) = withContext(ioDispatcher) {
        receipts.markTextExtracted(
            text = text,
            purchased_at = parsed.purchasedAtMillis,
            total_cents = parsed.totalCents,
            receipt_number = parsed.receiptNumber,
            atcud = parsed.atcud,
            id = id
        )
    }

    /**
     * The receipt's lines and new products, then status READY, in one transaction: a receipt is never
     * half extracted. Running it again replaces the lines (products are kept, matched by raw name).
     */
    suspend fun saveItems(receiptId: Long, lines: List<ExtractedLine>) = withContext(ioDispatcher) {
        database.transaction {
            items.deleteByReceipt(receiptId)
            lines.forEachIndexed { index, line ->
                val productId = if (line.kind == ExtractedLine.Kind.ITEM) {
                    products.insertIfNew(line.rawName, line.normalizedName, line.category, line.unitsPerPack.toLong())
                    products.idByRawName(line.rawName).executeAsOne()
                } else {
                    null
                }
                items.insert(
                    receipt_id = receiptId,
                    line_number = index.toLong(),
                    kind = line.kind.name,
                    product_id = productId,
                    raw_name = line.rawName,
                    quantity = line.quantity,
                    unit = line.unit.name,
                    unit_price_cents = line.unitPriceCents,
                    line_total_cents = line.lineTotalCents
                )
            }
            receipts.markReady(receiptId)
        }
    }

    /** Products without a vector of [model], or whose vector was made from a different text. */
    suspend fun productsToEmbed(model: String): List<ProductText> = withContext(ioDispatcher) {
        embeddings.productsWithEmbedText(model).executeAsList().mapNotNull { row ->
            val text = productEmbedText(row.normalized_name, row.category)
            if (text == row.embed_text) null else ProductText(row.id, text)
        }
    }

    suspend fun saveEmbeddings(model: String, vectors: List<Pair<ProductText, FloatArray>>) = withContext(ioDispatcher) {
        database.transaction {
            vectors.forEach { (product, vector) -> embeddings.upsert(product.productId, model, product.text, vector.toBlob()) }
        }
    }

    /** Called once every product has a vector of [model]: older models' vectors are no longer searched. */
    suspend fun deleteEmbeddingsOfOtherModels(model: String) = withContext(ioDispatcher) {
        embeddings.deleteOtherModels(model)
    }

    /**
     * What was paid (TOTAL A PAGAR) for receipts bought between [fromMillis] and [toMillis], both included. Same
     * query as the chat's sumSpending tool; receipts without a total yet aren't counted.
     */
    suspend fun spendingBetween(fromMillis: Long, toMillis: Long): DailySpending = withContext(ioDispatcher) {
        val total = insights.totalSpending(fromMillis, toMillis).executeAsOne()
        DailySpending(totalCents = total.cents ?: 0, receiptCount = total.receipts.toInt())
    }

    suspend fun markFailed(id: Long) = withContext(ioDispatcher) {
        receipts.updateStatus(ReceiptStatus.FAILED.name, id)
    }
}
