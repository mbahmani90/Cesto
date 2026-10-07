package com.majidbahmani.cesto.feature.receipts.data.local

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfURL

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosReceiptFileStoreTest {

    private val fileManager = NSFileManager.defaultManager
    private val directory = fileManager.URLForDirectory(NSApplicationSupportDirectory, NSUserDomainMask, null, true, null)!!
        .URLByAppendingPathComponent("receipts")!!

    @AfterTest
    fun tearDown() {
        fileManager.removeItemAtURL(directory, null)
    }

    @Test
    fun save_writesTheFile_returnsARelativePath_andExcludesItFromBackup() = runTest {
        val store = IosReceiptFileStore(StandardTestDispatcher(testScheduler))

        val path = store.save("receipt-1.pdf", "%PDF-1.7".encodeToByteArray())

        assertEquals("receipts/receipt-1.pdf", path)
        val data = assertNotNull(NSData.dataWithContentsOfURL(directory.URLByAppendingPathComponent("receipt-1.pdf")!!))
        assertEquals(8, data.length.toInt())

        val excluded = directory.resourceValuesForKeys(listOf(NSURLIsExcludedFromBackupKey), null)
            ?.get(NSURLIsExcludedFromBackupKey) as? NSNumber
        assertTrue(excluded?.boolValue == true)
    }

    @Test
    fun read_returnsWhatWasSaved() = runTest {
        val store = IosReceiptFileStore(StandardTestDispatcher(testScheduler))
        val path = store.save("receipt-2.pdf", "%PDF-1.7 content".encodeToByteArray())

        assertEquals("%PDF-1.7 content", store.read(path).decodeToString())
    }
}
