package com.majidbahmani.cesto.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Phones with version 1 (receipts already downloaded) must keep their data after the update. */
class MigrationTest {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

    @AfterTest
    fun tearDown() = driver.close()

    /** The receipt table exactly as version 1 created it. */
    private fun createVersion1() {
        driver.execute(
            null,
            """
            CREATE TABLE receipt (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                gmail_message_id TEXT NOT NULL,
                gmail_part_id TEXT NOT NULL,
                file_name TEXT NOT NULL,
                received_at INTEGER NOT NULL,
                pdf_path TEXT,
                status TEXT NOT NULL,
                UNIQUE (gmail_message_id, gmail_part_id)
            )
            """.trimIndent(),
            0,
        )
        driver.execute(null, "CREATE INDEX receipt_received_at ON receipt(received_at)", 0)
        driver.execute(
            null,
            "CREATE TABLE gmail_message (id TEXT NOT NULL PRIMARY KEY, received_at INTEGER NOT NULL, checked_at INTEGER NOT NULL)",
            0,
        )
        driver.execute(
            null,
            "INSERT INTO receipt(gmail_message_id, gmail_part_id, file_name, received_at, pdf_path, status) " +
                "VALUES ('m1', '1', 'f.pdf', 1000, 'receipts/receipt-1.pdf', 'DOWNLOADED')",
            0,
        )
    }

    @Test
    fun version1To2_keepsReceipts_andAddsTheExtractedFields() {
        createVersion1()

        CestoDatabase.Schema.migrate(driver, oldVersion = 1, newVersion = CestoDatabase.Schema.version)
        val receipts = CestoDatabase(driver).receiptQueries

        val old = receipts.selectAll().executeAsOne()
        assertEquals("receipts/receipt-1.pdf", old.pdf_path)
        assertEquals("DOWNLOADED", old.status)
        assertNull(old.text)

        receipts.markTextExtracted("TOTAL A PAGAR 4,52", 1_000, 452, "FS A/1", "ATCUD-1", old.id)
        val extracted = receipts.selectAll().executeAsOne()
        assertEquals("TEXT_EXTRACTED", extracted.status)
        assertEquals(452, extracted.total_cents)
    }

    @Test
    fun version1To3_addsItemsAndProducts() {
        createVersion1()

        CestoDatabase.Schema.migrate(driver, oldVersion = 1, newVersion = CestoDatabase.Schema.version)
        val database = CestoDatabase(driver)
        val receiptId = database.receiptQueries.selectAll().executeAsOne().id

        database.productQueries.insertIfNew("IOG GREGO NAT 4X125G", "Iogurte grego natural 4x125 g", "Laticinios", 4)
        val productId = database.productQueries.idByRawName("IOG GREGO NAT 4X125G").executeAsOne()
        database.receiptItemQueries.insert(receiptId, 0, "ITEM", productId, "IOG GREGO NAT 4X125G", 1.0, "UNIT", 249, 249)

        assertEquals(1, database.receiptItemQueries.selectByReceipt(receiptId).executeAsList().size)
        assertEquals("DOWNLOADED", database.receiptQueries.selectAll().executeAsOne().status) // untouched
    }

    @Test
    fun schemaVersion_is3() {
        assertEquals(3, CestoDatabase.Schema.version)
    }
}
