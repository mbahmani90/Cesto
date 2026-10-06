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
    fun schemaVersion_is2() {
        assertEquals(2, CestoDatabase.Schema.version)
    }
}
