package com.majidbahmani.cesto.feature.chat.data.tools

import com.majidbahmani.cesto.core.logging.logWarning
import com.majidbahmani.cesto.database.CestoDatabase
import com.majidbahmani.cesto.database.toVector
import com.majidbahmani.cesto.feature.chat.domain.model.ReceiptToolNames
import com.majidbahmani.cesto.feature.chat.domain.model.ReceiptTools
import com.majidbahmani.cesto.feature.chat.domain.model.ToolCall
import com.majidbahmani.cesto.feature.chat.domain.model.ToolResult
import com.majidbahmani.cesto.llm.embedding.EmbeddingProvider
import com.majidbahmani.cesto.llm.embedding.EmbeddingUnavailableException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.math.roundToLong
import kotlin.time.Instant

/**
 * Runs the model's tool calls as fixed, read-only SQL queries on the phone. The arguments are checked here
 * (the model can send anything); a bad call returns {"error": …} so the model can correct itself.
 * Results are small on purpose: they are what goes to Gemini.
 */
class SqlReceiptTools(
    private val database: CestoDatabase,
    private val ioDispatcher: CoroutineDispatcher,
    private val embeddings: EmbeddingProvider,
    private val timeZone: TimeZone = LISBON,
) : ReceiptTools {

    private val queries get() = database.insightsQueries

    override suspend fun run(call: ToolCall): ToolResult = withContext(ioDispatcher) {
        try {
            when (call.name) {
                ReceiptToolNames.FIND_PRODUCTS -> findProducts(call)
                ReceiptToolNames.SEMANTIC_SEARCH -> semanticSearch(call)
                ReceiptToolNames.SUM_QUANTITY -> sumQuantity(call)
                ReceiptToolNames.SUM_SPENDING -> sumSpending(call)
                ReceiptToolNames.TOP_PRODUCTS -> topProducts(call)
                ReceiptToolNames.LIST_RECEIPTS -> listReceipts(call)
                else -> errorResult(call, "Unknown tool ${call.name}.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: BadArgumentException) {
            errorResult(call, e.message.orEmpty())
        } catch (e: Exception) {
            logWarning(TAG, "Tool ${call.name} failed", e)
            errorResult(call, "The tool failed.")
        }
    }

    private fun findProducts(call: ToolCall): ToolResult {
        val keywords = call.arguments.strings("keywords")
            .map { it.trim().lowercase().filter { c -> c != '%' && c != '_' } }
            .filter { it.length >= 2 }
            .distinct()
            .take(MAX_KEYWORDS)
        if (keywords.isEmpty()) throw BadArgumentException("Give at least one keyword of 2+ letters.")
        val products = keywords.flatMap { queries.searchProducts("%$it%").executeAsList() }
            .distinctBy { it.id }
            .take(MAX_ROWS)
        return ToolResult(
            call,
            buildJsonObject {
                putJsonArray("products") {
                    products.forEach { p ->
                        addJsonObject {
                            put("id", p.id)
                            put("name", p.normalized_name)
                            put("printedName", p.raw_name)
                            p.category?.let { put("category", it) }
                            if (p.units_per_pack > 1) put("unitsPerPack", p.units_per_pack)
                        }
                    }
                }
                if (products.isEmpty()) put("note", "No product matches. Try other Portuguese keywords.")
            },
        )
    }

    /**
     * By meaning: the search words get a vector (one Gemini request), compared on the phone with every
     * product's vector. Returns candidates with scores; the model decides which really match.
     */
    private suspend fun semanticSearch(call: ToolCall): ToolResult {
        val query = call.arguments.string("query") ?: throw BadArgumentException("query is required, e.g. \"dairy\".")
        val limit = (call.arguments.long("limit") ?: DEFAULT_SEMANTIC).coerceIn(1, MAX_SEMANTIC).toInt()
        val products = database.productEmbeddingQueries.vectorsForModel(embeddings.modelId).executeAsList()
        if (products.isEmpty()) {
            return ToolResult(call, buildJsonObject { put("note", "No products can be searched by meaning yet. Use findProducts.") })
        }
        val queryVector = try {
            embeddings.embedQuery(query)
        } catch (e: EmbeddingUnavailableException) {
            logWarning(TAG, "semanticSearch unavailable (${e.reason})")
            return errorResult(call, "Search by meaning isn't available right now. Use findProducts with Portuguese keywords.")
        }
        val matches = products
            .map { it to cosine(queryVector, it.vector.toVector()) }
            .filter { (_, score) -> score >= MIN_SCORE }
            .sortedByDescending { (_, score) -> score }
            .take(limit)
        return ToolResult(
            call,
            buildJsonObject {
                putJsonArray("products") {
                    matches.forEach { (p, score) ->
                        addJsonObject {
                            put("id", p.product_id)
                            put("name", p.normalized_name)
                            put("printedName", p.raw_name)
                            p.category?.let { put("category", it) }
                            put("score", (score * 100).roundToLong() / 100.0)
                        }
                    }
                }
                put("note", if (matches.isEmpty()) "Nothing close. Try other words or findProducts." else "Closest first; keep only the ones that match.")
            },
        )
    }

    private fun sumQuantity(call: ToolCall): ToolResult {
        val ids = call.arguments.productIds(required = true)
        val (from, to) = call.arguments.period()
        val rows = queries.quantityByProduct(ids, from, to).executeAsList()
        val units = rows.filter { it.unit == UNIT }
        val kg = rows.filter { it.unit == KG }
        return ToolResult(
            call,
            buildJsonObject {
                putJsonArray("perProduct") {
                    rows.forEach { r ->
                        addJsonObject {
                            put("productId", r.product_id)
                            put("name", r.normalized_name)
                            if (r.unit == KG) {
                                put("kg", r.quantity.rounded())
                            } else {
                                put("packs", r.quantity.rounded())
                                put("singleUnits", r.single_units.rounded())
                            }
                            put("receipts", r.receipts)
                        }
                    }
                }
                putTotals {
                    put("packs", units.sumOf { it.quantity }.rounded())
                    put("singleUnits", units.sumOf { it.single_units }.rounded())
                    put("kg", kg.sumOf { it.quantity }.rounded())
                }
                putUnreadReceipts(from, to)
            },
            receiptIds = queries.receiptIdsWithProducts(ids, from, to).executeAsList(),
        )
    }

    private fun sumSpending(call: ToolCall): ToolResult {
        val ids = call.arguments.productIds(required = false)
        val (from, to) = call.arguments.period()
        if (ids.isEmpty()) {
            val total = queries.totalSpending(from, to).executeAsOne()
            return ToolResult(
                call,
                buildJsonObject {
                    put("scope", "everything paid")
                    put("euros", (total.cents ?: 0).euros())
                    put("receipts", total.receipts)
                },
            )
        }
        val rows = queries.spendingByProduct(ids, from, to).executeAsList()
        return ToolResult(
            call,
            buildJsonObject {
                putJsonArray("perProduct") {
                    rows.forEach { r ->
                        addJsonObject {
                            put("productId", r.product_id)
                            put("name", r.normalized_name)
                            put("euros", r.cents.euros())
                            put("receipts", r.receipts)
                        }
                    }
                }
                putTotals { put("euros", rows.sumOf { it.cents }.euros()) }
                putUnreadReceipts(from, to)
            },
            receiptIds = queries.receiptIdsWithProducts(ids, from, to).executeAsList(),
        )
    }

    private fun topProducts(call: ToolCall): ToolResult {
        val (from, to) = call.arguments.period()
        val limit = (call.arguments.long("limit") ?: DEFAULT_TOP).coerceIn(1, MAX_TOP)
        val bySpending = when (call.arguments.string("by")) {
            "spending" -> true
            "quantity", null -> false
            else -> throw BadArgumentException("by must be \"quantity\" or \"spending\".")
        }
        val rows = if (bySpending) {
            queries.topProductsBySpending(from, to, limit).executeAsList().map { Triple(it.id to it.normalized_name, it.amount, it.cents) }
        } else {
            queries.topProductsByQuantity(from, to, limit).executeAsList().map { Triple(it.id to it.normalized_name, it.amount, it.cents) }
        }
        return ToolResult(
            call,
            buildJsonObject {
                putJsonArray("products") {
                    rows.forEach { (product, amount, cents) ->
                        addJsonObject {
                            put("productId", product.first)
                            put("name", product.second)
                            put("amount", amount.rounded())
                            put("euros", cents.euros())
                        }
                    }
                }
                put("amountMeans", "single units, or kg for weighed products")
                putUnreadReceipts(from, to)
            },
        )
    }

    private fun listReceipts(call: ToolCall): ToolResult {
        val (from, to) = call.arguments.period()
        val receipts = queries.listReceipts(from, to).executeAsList()
        return ToolResult(
            call,
            buildJsonObject {
                putJsonArray("receipts") {
                    receipts.forEach { r ->
                        addJsonObject {
                            put("id", r.id)
                            put("date", Instant.fromEpochMilliseconds(r.at).toLocalDateTime(timeZone).format(DATE_TIME))
                            r.total_cents?.let { put("euros", it.euros()) }
                            if (r.status == READY) put("items", r.items) else put("items", "not read yet")
                        }
                    }
                }
                if (receipts.size == MAX_ROWS) put("note", "Only the newest $MAX_ROWS are listed.")
            },
            receiptIds = receipts.map { it.id },
        )
    }

    private fun JsonObjectBuilder.putTotals(block: JsonObjectBuilder.() -> Unit) {
        put("total", buildJsonObject(block))
    }

    private fun JsonObjectBuilder.putUnreadReceipts(from: Long, to: Long) {
        val unread = queries.receiptsWithoutItems(from, to).executeAsOne()
        if (unread > 0) put("receiptsWithItemsNotReadYet", unread)
    }

    /** Inclusive [from, to] days, as epoch millis in the user's time zone. */
    private fun JsonObject.period(): Pair<Long, Long> {
        val from = date("from")
        val to = date("to")
        if (to < from) throw BadArgumentException("to is before from.")
        val start = from.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val end = to.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone).toEpochMilliseconds() - 1
        return start to end
    }

    private fun JsonObject.date(name: String): LocalDate {
        val text = string(name) ?: throw BadArgumentException("$name is required (YYYY-MM-DD).")
        return runCatching { LocalDate.parse(text) }.getOrNull()
            ?: throw BadArgumentException("$name must be YYYY-MM-DD, got \"$text\".")
    }

    private fun JsonObject.productIds(required: Boolean): List<Long> {
        val ids = longs("productIds").distinct()
        if (required && ids.isEmpty()) throw BadArgumentException("productIds is required: call findProducts first.")
        if (ids.size > MAX_ROWS) throw BadArgumentException("At most $MAX_ROWS productIds.")
        return ids
    }

    private class BadArgumentException(message: String) : Exception(message)

    private fun errorResult(call: ToolCall, message: String) =
        ToolResult(call, buildJsonObject { put("error", message) })

    private companion object {
        const val TAG = "SqlReceiptTools"
        const val MAX_ROWS = 50
        const val MAX_KEYWORDS = 5
        const val DEFAULT_TOP = 10L
        const val MAX_TOP = 20L
        const val DEFAULT_SEMANTIC = 15L
        const val MAX_SEMANTIC = 30L

        /** Below this, a product is unrelated to the search words. A loose floor: the model filters the rest. */
        const val MIN_SCORE = 0.25f
        const val UNIT = "UNIT"
        const val KG = "KG"
        const val READY = "READY"

        val DATE_TIME = LocalDateTime.Format {
            year(); char('-'); monthNumber(); char('-'); day(); char(' '); hour(); char(':'); minute()
        }
    }
}

val LISBON: TimeZone = TimeZone.of("Europe/Lisbon")

private fun Long.euros(): Double = this / 100.0

private fun Double.rounded(): Double = (this * 1000).roundToLong() / 1000.0

// The model's arguments: numbers may arrive as 3, 3.0 or "3".
private fun JsonObject.string(name: String): String? =
    (this[name] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

private fun JsonObject.long(name: String): Long? = (this[name] as? JsonPrimitive)?.toLongOrNull()

private fun JsonObject.strings(name: String): List<String> = when (val value = this[name]) {
    is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
    is JsonPrimitive -> listOfNotNull(value.contentOrNull)
    else -> emptyList()
}

private fun JsonObject.longs(name: String): List<Long> = when (val value = this[name]) {
    is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.toLongOrNull() }
    else -> emptyList()
}

private fun JsonPrimitive.toLongOrNull(): Long? = contentOrNull?.toDoubleOrNull()?.toLong()
