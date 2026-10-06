package com.majidbahmani.cesto.feature.receipts.data.local

/** Receipt PDFs in app-private storage, excluded from backups. Implemented per platform. */
interface ReceiptFileStore {
    /** Writes the file and returns its path relative to the app's storage (stored in the database). */
    suspend fun save(fileName: String, bytes: ByteArray): String
}
