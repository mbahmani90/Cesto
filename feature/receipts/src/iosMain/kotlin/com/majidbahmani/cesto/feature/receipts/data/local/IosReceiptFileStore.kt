package com.majidbahmani.cesto.feature.receipts.data.local

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToURL

/**
 * `Application Support/receipts`, excluded from iCloud backup. Only the relative path is stored:
 * the app container's absolute path changes between installs and updates.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosReceiptFileStore(
    private val ioDispatcher: CoroutineDispatcher,
) : ReceiptFileStore {

    override suspend fun save(fileName: String, bytes: ByteArray): String = withContext(ioDispatcher) {
        require(bytes.isNotEmpty()) { "empty file" }
        val directory = receiptsDirectory()
        val file = requireNotNull(directory.URLByAppendingPathComponent(fileName))
        val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        check(data.writeToURL(file, atomically = true)) { "couldn't write $fileName" }
        "$DIRECTORY/$fileName"
    }

    private fun receiptsDirectory(): NSURL {
        val fileManager = NSFileManager.defaultManager
        val base = requireNotNull(
            fileManager.URLForDirectory(NSApplicationSupportDirectory, NSUserDomainMask, null, true, null),
        )
        val directory = requireNotNull(base.URLByAppendingPathComponent(DIRECTORY))
        fileManager.createDirectoryAtURL(directory, withIntermediateDirectories = true, attributes = null, error = null)
        directory.setResourceValue(true, forKey = NSURLIsExcludedFromBackupKey, error = null)
        return directory
    }

    private companion object {
        const val DIRECTORY = "receipts"
    }
}
