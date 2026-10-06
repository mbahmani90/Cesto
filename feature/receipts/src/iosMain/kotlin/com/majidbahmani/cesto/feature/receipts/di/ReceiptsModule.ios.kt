package com.majidbahmani.cesto.feature.receipts.di

import com.majidbahmani.cesto.core.di.IoDispatcher
import com.majidbahmani.cesto.feature.receipts.data.local.IosReceiptFileStore
import com.majidbahmani.cesto.feature.receipts.data.local.ReceiptFileStore
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val receiptFileStoreModule: Module = module {
    single<ReceiptFileStore> { IosReceiptFileStore(ioDispatcher = get(IoDispatcher)) }
}
