package com.majidbahmani.cesto.feature.receipts.di

import com.majidbahmani.cesto.core.di.DefaultDispatcher
import com.majidbahmani.cesto.core.di.IoDispatcher
import com.majidbahmani.cesto.feature.receipts.data.local.IosPdfTextExtractor
import com.majidbahmani.cesto.feature.receipts.data.local.IosReceiptFileStore
import com.majidbahmani.cesto.feature.receipts.data.local.PdfTextExtractor
import com.majidbahmani.cesto.feature.receipts.data.local.ReceiptFileStore
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val receiptPlatformModule: Module = module {
    single<ReceiptFileStore> { IosReceiptFileStore(ioDispatcher = get(IoDispatcher)) }
    single<PdfTextExtractor> { IosPdfTextExtractor(defaultDispatcher = get(DefaultDispatcher)) }
}
