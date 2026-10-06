package com.majidbahmani.cesto.feature.receipts.data.extraction

import com.majidbahmani.cesto.llm.GeminiApi
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.delay
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.math.roundToLong

/**
 * Gemini's structured output: the prompt explains Continente's line layout, the schema makes the answer
 * valid JSON with exactly these fields. Amounts come back in euros and are converted to cents here.
 */
class GeminiReceiptItemExtractor(private val gemini: GeminiApi) : ReceiptItemExtractor {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun extract(key: String, itemSection: String): List<ExtractedLine> {
        val answer = generateWithRetries(key, itemSection)
        return json.decodeFromString<ExtractedItemsDto>(answer).lines.map { it.toLine() }
    }

    /**
     * Gemini overload (503/500) is temporary: wait and retry, then the fallback model; if both stay
     * overloaded, pause extraction for this sync instead of sending every receipt into errors.
     */
    private suspend fun generateWithRetries(key: String, itemSection: String): String {
        for (model in listOf(GeminiApi.EXTRACTION_MODEL, GeminiApi.FALLBACK_MODEL)) {
            for (wait in RETRY_WAITS_MILLIS) {
                try {
                    return generate(key, itemSection, model)
                } catch (e: ServerResponseException) {
                    delay(wait)
                }
            }
        }
        throw ExtractionUnavailableException("Gemini overloaded (5xx on ${GeminiApi.EXTRACTION_MODEL} and ${GeminiApi.FALLBACK_MODEL})")
    }

    private suspend fun generate(key: String, itemSection: String, model: String): String =
        try {
            gemini.generateJson(key = key, systemInstruction = INSTRUCTION, prompt = itemSection, responseSchema = SCHEMA, model = model)
        } catch (e: ClientRequestException) {
            when (e.response.status) {
                // No point trying the other receipts now: wrong key, API not allowed, or quota used up.
                HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden,
                HttpStatusCode.TooManyRequests,
                -> throw ExtractionUnavailableException("Gemini answered ${e.response.status.value}", e)
                else -> throw e
            }
        }

    @Serializable
    private data class ExtractedItemsDto(val lines: List<LineDto> = emptyList())

    @Serializable
    private data class LineDto(
        val kind: String,
        val rawName: String,
        val normalizedName: String? = null,
        val category: String? = null,
        val quantity: Double = 1.0,
        val unit: String = "UNIT",
        val unitsPerPack: Int = 1,
        val unitPrice: Double? = null,
        val lineTotal: Double,
    ) {
        fun toLine() = ExtractedLine(
            kind = ExtractedLine.Kind.entries.firstOrNull { it.name == kind } ?: ExtractedLine.Kind.ITEM,
            rawName = rawName.trim(),
            normalizedName = normalizedName?.trim()?.takeIf { it.isNotEmpty() } ?: rawName.trim(),
            category = category?.trim()?.takeIf { it.isNotEmpty() },
            quantity = quantity,
            unit = if (unit == "KG") ExtractedLine.Unit.KG else ExtractedLine.Unit.UNIT,
            unitsPerPack = unitsPerPack.coerceAtLeast(1),
            unitPriceCents = unitPrice?.toCents(),
            lineTotalCents = lineTotal.toCents(),
        )
    }

    private companion object {
        /** Before each retry: 2 tries per model. */
        val RETRY_WAITS_MILLIS = listOf(2_000L, 5_000L)

        fun Double.toCents(): Long = (this * 100).roundToLong()

        val INSTRUCTION = """
            You read the item section of a Portuguese Continente supermarket receipt and return every line as JSON.
            Rules:
            - Each purchased product is one ITEM. Its line may be split in two: the name, then "quantity X unit price total",
              e.g. "0,760 X 1,19 0,90" means 0.760 kg at 1.19 €/kg = 0.90 €, and "6 X 0,23 1,38" means 6 units at 0.23 € = 1.38 €.
              Without such a line, quantity is 1 and the unit price equals the line total.
            - Lines under "Taras e Valor de Deposito" (bottle deposits) are kind DEPOSIT.
              Negative lines (savings, discounts) are kind DISCOUNT with a negative lineTotal.
            - Lines ending with ":" are category headers, not lines. Give each item the category of the header above it,
              without the colon.
            - rawName: the name exactly as printed, without the VAT letter like "(A)" and without prices.
            - normalizedName: the full Portuguese product name without abbreviations,
              e.g. "IOG GREGO NAT 4X125G" -> "Iogurte grego natural 4x125 g", "LEITE PAST GORDO VIGOR 1L" -> "Leite pasteurizado gordo Vigor 1 L".
            - unitsPerPack: units in one pack from the name ("4X125G" -> 4, "6X1L" -> 6), otherwise 1.
            - unit: KG when the quantity is a weight (like 0,760), otherwise UNIT.
            - Amounts in euros as numbers with a dot (1,19 -> 1.19).
            - Return only lines that are in the text. Never invent lines or names.
        """.trimIndent()

        val SCHEMA: JsonObject = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("lines") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("kind") {
                                put("type", "STRING")
                                putJsonArray("enum") { add("ITEM"); add("DEPOSIT"); add("DISCOUNT") }
                            }
                            putJsonObject("rawName") { put("type", "STRING") }
                            putJsonObject("normalizedName") { put("type", "STRING") }
                            putJsonObject("category") { put("type", "STRING"); put("nullable", true) }
                            putJsonObject("quantity") { put("type", "NUMBER") }
                            putJsonObject("unit") {
                                put("type", "STRING")
                                putJsonArray("enum") { add("UNIT"); add("KG") }
                            }
                            putJsonObject("unitsPerPack") { put("type", "INTEGER") }
                            putJsonObject("unitPrice") { put("type", "NUMBER"); put("nullable", true) }
                            putJsonObject("lineTotal") { put("type", "NUMBER") }
                        }
                        putJsonArray("required") {
                            listOf("kind", "rawName", "normalizedName", "quantity", "unit", "unitsPerPack", "lineTotal").forEach { add(it) }
                        }
                    }
                }
            }
            putJsonArray("required") { add("lines") }
        }
    }
}
