package com.majidbahmani.cesto.feature.chat.data.tools

import com.majidbahmani.cesto.database.CestoDatabase
import com.majidbahmani.cesto.database.toBlob
import com.majidbahmani.cesto.feature.chat.domain.model.ToolCall
import com.majidbahmani.cesto.feature.chat.fake.FakeEmbeddingProvider
import com.majidbahmani.cesto.feature.chat.fake.createTestDriver
import com.majidbahmani.cesto.llm.embedding.EmbeddingUnavailableException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** Invented receipts in an in-memory database: September has two read receipts and one not read yet. */
class SqlReceiptToolsTest {

    private val database = CestoDatabase(createTestDriver())

    // 2-number vectors: x = "dairy", y = "fruit".
    private val embeddings = FakeEmbeddingProvider(mapOf("dairy" to floatArrayOf(1f, 0f), "fruit" to floatArrayOf(0f, 1f)))
    private val tools = SqlReceiptTools(database, Dispatchers.Unconfined, embeddings)

    private val yogurtPack = product("IOG GREGO NAT 4X125G", "Iogurte grego natural 4x125 g", "Laticinios", unitsPerPack = 4)
    private val yogurt = product("IOG LIQ MORANGO", "Iogurte liquido morango", "Laticinios")
    private val banana = product("BANANA", "Banana", "Frutas e Legumes")

    private val sept10 = receipt("m1", at(2026, 9, 10, 18, 0), totalCents = 1_000)
    private val sept30Late = receipt("m2", at(2026, 9, 30, 23, 30), totalCents = 550) // 22:30 UTC: still September in Lisbon
    private val sept20Unread = receipt("m3", at(2026, 9, 20, 10, 0), totalCents = 2_345, ready = false)
    private val aug5 = receipt("m4", at(2026, 8, 5, 9, 0), totalCents = 700)

    init {
        item(sept10, yogurtPack, quantity = 2.0, totalCents = 398)
        item(sept10, banana, quantity = 0.76, unit = "KG", totalCents = 90)
        item(sept30Late, yogurt, quantity = 3.0, totalCents = 150)
        item(aug5, yogurtPack, quantity = 1.0, totalCents = 199)
        vector(yogurtPack, 0.9f, 0.1f)
        vector(yogurt, 0.8f, 0.3f)
        vector(banana, 0.1f, 0.95f)
    }

    @Test
    fun semanticSearch_ranksByMeaning_closestFirst_dropsUnrelated() = runTest {
        val output = run("semanticSearch", buildJsonObject { put("query", "dairy") })

        val products = output["products"]!!.jsonArray.map { it.jsonObject }
        assertEquals(listOf(yogurtPack, yogurt), products.map { it["id"]!!.jsonPrimitive.long }) // banana: score 0.1
        assertEquals("Laticinios", products.first()["category"]!!.jsonPrimitive.content)
        assertTrue(products.first()["score"]!!.jsonPrimitive.double > products.last()["score"]!!.jsonPrimitive.double)
        assertEquals(listOf("dairy"), embeddings.asked)
    }

    @Test
    fun semanticSearch_respectsTheLimit() = runTest {
        val output = run(
            "semanticSearch",
            buildJsonObject {
                put("query", "dairy")
                put("limit", 1)
            }
        )

        assertEquals(1, output["products"]!!.jsonArray.size)
    }

    @Test
    fun semanticSearch_unavailable_pointsToFindProducts() = runTest {
        embeddings.unavailable = EmbeddingUnavailableException.Reason.QUOTA

        val output = run("semanticSearch", buildJsonObject { put("query", "dairy") })

        assertTrue(output["error"]!!.jsonPrimitive.content.contains("findProducts"))
    }

    @Test
    fun semanticSearch_withoutVectors_saysSo_withoutAskingGemini() = runTest {
        database.productEmbeddingQueries.deleteAll()

        val output = run("semanticSearch", buildJsonObject { put("query", "dairy") })

        assertTrue(output["note"]!!.jsonPrimitive.content.contains("findProducts"))
        assertTrue(embeddings.asked.isEmpty())
    }

    @Test
    fun semanticSearch_onlyUsesVectorsOfTheCurrentModel() = runTest {
        database.productEmbeddingQueries.deleteAll()
        database.productEmbeddingQueries.upsert(banana, "old@2", "Banana", floatArrayOf(1f, 0f).toBlob())

        val output = run("semanticSearch", buildJsonObject { put("query", "dairy") })

        assertTrue("products" !in output)
    }

    @Test
    fun findProducts_matchesAnyKeyword_inNameOrCategory_caseInsensitive() = runTest {
        val output = run(
            "findProducts",
            buildJsonObject {
                putJsonArray("keywords") {
                    add("IOGURT")
                    add("frutas")
                }
            }
        )

        val names = output["products"]!!.jsonArray.map { it.jsonObject["name"]!!.jsonPrimitive.content }
        assertEquals(setOf("Iogurte grego natural 4x125 g", "Iogurte liquido morango", "Banana"), names.toSet())
        val pack = output["products"]!!.jsonArray.first { it.jsonObject["id"]!!.jsonPrimitive.long == yogurtPack }.jsonObject
        assertEquals(4, pack["unitsPerPack"]!!.jsonPrimitive.long)
    }

    @Test
    fun findProducts_withoutUsableKeywords_isAnError() = runTest {
        val output = run("findProducts", buildJsonObject { putJsonArray("keywords") { add("%") } })

        assertTrue("error" in output)
    }

    @Test
    fun sumQuantity_countsPacksAndSingleUnits_inTheLisbonPeriod() = runTest {
        val result = tools.run(call("sumQuantity", ids(yogurtPack, yogurt), "2026-09-01", "2026-09-30"))

        val total = result.output["total"]!!.jsonObject
        assertEquals(5.0, total["packs"]!!.jsonPrimitive.double) // 2 packs + 3 single, August left out
        assertEquals(11.0, total["singleUnits"]!!.jsonPrimitive.double) // 2 x 4 + 3
        assertEquals(setOf(sept10, sept30Late), result.receiptIds.toSet())
        assertEquals(1, result.output["receiptsWithItemsNotReadYet"]!!.jsonPrimitive.long)
    }

    @Test
    fun sumQuantity_weighedProductsInKg() = runTest {
        val output = run("sumQuantity", call("sumQuantity", ids(banana), "2026-09-01", "2026-09-30").arguments)

        assertEquals(0.76, output["total"]!!.jsonObject["kg"]!!.jsonPrimitive.double)
    }

    @Test
    fun sumQuantity_needsProductIds() = runTest {
        val output = run("sumQuantity", call("sumQuantity", JsonArray(emptyList()), "2026-09-01", "2026-09-30").arguments)

        assertTrue(output["error"]!!.jsonPrimitive.content.contains("findProducts"))
    }

    @Test
    fun sumSpending_withoutProducts_isWhatWasPaid_includingUnreadReceipts() = runTest {
        val output = run(
            "sumSpending",
            buildJsonObject {
                put("from", "2026-09-01")
                put("to", "2026-09-30")
            }
        )

        assertEquals(38.95, output["euros"]!!.jsonPrimitive.double) // 10,00 + 5,50 + 23,45
        assertEquals(3, output["receipts"]!!.jsonPrimitive.long)
    }

    @Test
    fun sumSpending_forProducts_perProductAndTotal() = runTest {
        val output = run("sumSpending", call("sumSpending", ids(yogurtPack, yogurt), "2026-08-01", "2026-09-30").arguments)

        assertEquals(7.47, output["total"]!!.jsonObject["euros"]!!.jsonPrimitive.double) // 3,98 + 1,99 + 1,50
        assertEquals(2, output["perProduct"]!!.jsonArray.size)
    }

    @Test
    fun topProducts_bySpending() = runTest {
        val output = run(
            "topProducts",
            buildJsonObject {
                put("from", "2026-01-01")
                put("to", "2026-12-31")
                put("by", "spending")
                put("limit", 2.0)
            }
        )

        val first = output["products"]!!.jsonArray.first().jsonObject
        assertEquals("Iogurte grego natural 4x125 g", first["name"]!!.jsonPrimitive.content)
        assertEquals(5.97, first["euros"]!!.jsonPrimitive.double)
        assertEquals(2, output["products"]!!.jsonArray.size)
    }

    @Test
    fun listReceipts_newestFirst_andSaysWhichAreNotRead() = runTest {
        val result = tools.run(
            ToolCall(
                "listReceipts",
                buildJsonObject {
                    put("from", "2026-09-01")
                    put("to", "2026-09-30")
                }
            )
        )

        assertEquals(listOf(sept30Late, sept20Unread, sept10), result.receiptIds)
        val receipts = result.output["receipts"]!!.jsonArray.map { it.jsonObject }
        assertEquals("2026-09-30 23:30", receipts[0]["date"]!!.jsonPrimitive.content)
        assertEquals("not read yet", receipts[1]["items"]!!.jsonPrimitive.content)
    }

    @Test
    fun badDates_andUnknownTools_areErrorsTheModelCanRead() = runTest {
        assertTrue(
            "error" in run(
                "listReceipts",
                buildJsonObject {
                    put("from", "last month")
                    put("to", "2026-09-30")
                }
            )
        )
        assertTrue(
            "error" in run(
                "listReceipts",
                buildJsonObject {
                    put("from", "2026-09-30")
                    put("to", "2026-09-01")
                }
            )
        )
        assertTrue("error" in run("deleteEverything", JsonObject(emptyMap())))
    }

    @Test
    fun noData_isNotAnError() = runTest {
        val output = run(
            "sumSpending",
            buildJsonObject {
                put("from", "2020-01-01")
                put("to", "2020-12-31")
            }
        )

        assertEquals(0.0, output["euros"]!!.jsonPrimitive.double)
        assertNull(output["error"])
    }

    private suspend fun run(name: String, arguments: JsonObject): JsonObject = tools.run(ToolCall(name, arguments)).output

    private fun ids(vararg ids: Long) = JsonArray(ids.map { JsonPrimitive(it) })

    private fun call(name: String, productIds: JsonArray, from: String, to: String) = ToolCall(
        name,
        buildJsonObject {
            put("productIds", productIds)
            put("from", from)
            put("to", to)
        }
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime(year, month, day, hour, minute).toInstant(LISBON).toEpochMilliseconds()

    private fun product(raw: String, name: String, category: String, unitsPerPack: Long = 1): Long {
        database.productQueries.insertIfNew(raw, name, category, unitsPerPack)
        return database.productQueries.idByRawName(raw).executeAsOne()
    }

    private fun receipt(messageId: String, purchasedAt: Long, totalCents: Long, ready: Boolean = true): Long {
        database.receiptQueries.insertIfNew(messageId, "1", "$messageId.pdf", purchasedAt + 60_000)
        val id = database.receiptQueries.selectAll().executeAsList().first { it.gmail_message_id == messageId }.id
        database.receiptQueries.markTextExtracted("text", purchasedAt, totalCents, null, null, id)
        if (ready) database.receiptQueries.markReady(id)
        return id
    }

    private var line = 0L

    private fun vector(productId: Long, x: Float, y: Float) =
        database.productEmbeddingQueries.upsert(productId, embeddings.modelId, "text", floatArrayOf(x, y).toBlob())

    private fun item(receiptId: Long, productId: Long, quantity: Double, totalCents: Long, unit: String = "UNIT") {
        database.receiptItemQueries.insert(receiptId, line++, "ITEM", productId, "raw", quantity, unit, null, totalCents)
    }
}
