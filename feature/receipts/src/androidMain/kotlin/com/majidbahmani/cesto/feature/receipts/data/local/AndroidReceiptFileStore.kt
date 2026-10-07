package com.majidbahmani.cesto.feature.receipts.data.local

import android.content.Context
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** `filesDir/receipts`: app-private, excluded from backup and device transfer by the manifest rules. */
class AndroidReceiptFileStore(private val context: Context, private val ioDispatcher: CoroutineDispatcher) : ReceiptFileStore {

    override suspend fun save(fileName: String, bytes: ByteArray): String = withContext(ioDispatcher) {
        val directory = File(context.filesDir, DIRECTORY).apply { mkdirs() }
        File(directory, fileName).writeBytes(bytes)
        "$DIRECTORY/$fileName"
    }

    override suspend fun read(relativePath: String): ByteArray = withContext(ioDispatcher) {
        File(context.filesDir, relativePath).readBytes()
    }

    private companion object {
        const val DIRECTORY = "receipts"
    }
}
