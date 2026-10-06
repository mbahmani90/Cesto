package com.majidbahmani.cesto.feature.receipts.fake

import com.majidbahmani.cesto.feature.receipts.data.local.ReceiptFileStore

class FakeReceiptFileStore : ReceiptFileStore {
    val files = mutableMapOf<String, ByteArray>()

    override suspend fun save(fileName: String, bytes: ByteArray): String {
        files[fileName] = bytes
        return "receipts/$fileName"
    }
}
