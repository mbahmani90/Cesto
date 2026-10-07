package com.majidbahmani.cesto.feature.chat.data.agent

import com.majidbahmani.cesto.database.CestoDatabase
import com.majidbahmani.cesto.feature.chat.data.tools.LISBON
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** What the receipts cover: lets the model resolve "last month" and say when data is missing. */
data class DataCoverage(val first: LocalDate?, val last: LocalDate?, val receipts: Long, val receiptsWithItems: Long)

/** The system instruction: rules for the model, today's date and what data exists (no receipt content). */
class ReceiptAgentPrompt(
    private val database: CestoDatabase,
    private val ioDispatcher: CoroutineDispatcher,
    private val currentTimeMillis: () -> Long,
    private val timeZone: TimeZone = LISBON
) {
    suspend fun build(): String {
        val coverage = withContext(ioDispatcher) {
            database.insightsQueries.dataRange().executeAsOne().let {
                DataCoverage(it.first?.toDate(), it.last?.toDate(), it.receipts, it.ready ?: 0)
            }
        }
        return systemInstruction(today = currentTimeMillis().toDate(), coverage = coverage)
    }

    private fun Long.toDate(): LocalDate = Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone).date
}

internal fun systemInstruction(today: LocalDate, coverage: DataCoverage): String {
    val data = if (coverage.receipts == 0L || coverage.first == null) {
        "There are no receipts yet: say so and suggest syncing receipts in the Receipts tab."
    } else {
        "The receipts go from ${coverage.first} to ${coverage.last}: ${coverage.receipts} receipts, " +
            "items read on ${coverage.receiptsWithItems} of them (the totals paid are known for all)."
    }
    return """
        You answer questions about the user's grocery purchases at Continente (a Portuguese supermarket),
        using only the tools, which read the user's receipts on their phone.
        Today is $today (${today.dayOfWeek.name.lowercase()}). $data

        Rules:
        - Take every number from a tool result, including totals. Never estimate or invent numbers or products.
        - Product names are in Portuguese (iogurte, leite, ovos, frango). For names, brands or simple words, translate the
          user's words to Portuguese keywords for findProducts and try a few variations if nothing is found.
          For categories or meaning (dairy, snacks, sweet things, cleaning products), use semanticSearch.
        - From findProducts' or semanticSearch's results, use only the products that really match the question. If the choice isn't obvious,
          say briefly which products you counted.
        - Turn periods like "last month" or "this year" into dates from today. Without a period, use all the data.
        - These are purchases, not what was eaten: say "bought", never "ate" or "drank".
        - If a result has receiptsWithItemsNotReadYet, say the answer may be incomplete because some receipts aren't read yet.
        - Answer in the user's language, in one to three short sentences. Amounts in euros, e.g. 12,34 €.
        - Only answer questions about these purchases. For anything else, say what you can help with.
    """.trimIndent()
}
