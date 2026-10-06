package com.majidbahmani.cesto.database

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ProductEmbeddingTest {

    private val driver = createTestDriver()
    private val database = CestoDatabase(driver)
    private val products = database.productQueries
    private val embeddings = database.productEmbeddingQueries

    @AfterTest
    fun tearDown() = driver.close()

    private fun product(raw: String, name: String): Long {
        products.insertIfNew(raw, name, "Laticinios", 1)
        return products.idByRawName(raw).executeAsOne()
    }

    @Test
    fun vector_survivesTheBlob_bitForBit() {
        val vector = floatArrayOf(0.0123f, -0.0456f, 1f, -0f, Float.MIN_VALUE, 3.4e38f)

        assertEquals(vector.toList(), vector.toBlob().toVector().toList())
        assertEquals(24, vector.toBlob().size)
    }

    @Test
    fun brokenBlob_isRejected() {
        assertFailsWith<IllegalArgumentException> { ByteArray(5).toVector() }
    }

    @Test
    fun productsWithEmbedText_showsWhichNeedAVector_forThatModel() {
        val milk = product("LEITE MG 1L", "Leite meio-gordo 1 L")
        val yogurt = product("IOG NAT", "Iogurte natural")
        embeddings.upsert(milk, "model-a", "Leite meio-gordo 1 L. Categoria: Laticinios", floatArrayOf(1f).toBlob())

        val forA = embeddings.productsWithEmbedText("model-a").executeAsList().associateBy { it.id }
        assertEquals("Leite meio-gordo 1 L. Categoria: Laticinios", forA.getValue(milk).embed_text)
        assertNull(forA.getValue(yogurt).embed_text)

        val forB = embeddings.productsWithEmbedText("model-b").executeAsList()
        assertEquals(listOf(null, null), forB.map { it.embed_text }) // another model: everything again
    }

    @Test
    fun upsert_replacesTheVectorOfThatModelOnly() {
        val milk = product("LEITE MG 1L", "Leite meio-gordo 1 L")
        embeddings.upsert(milk, "model-a", "old", floatArrayOf(1f).toBlob())
        embeddings.upsert(milk, "model-b", "text", floatArrayOf(2f).toBlob())

        embeddings.upsert(milk, "model-a", "new", floatArrayOf(3f).toBlob())

        assertEquals(2, embeddings.count().executeAsOne())
        assertEquals(listOf(3f), embeddings.vectorsForModel("model-a").executeAsOne().vector.toVector().toList())

        embeddings.deleteOtherModels("model-a")
        assertEquals(1, embeddings.count().executeAsOne())
    }
}
