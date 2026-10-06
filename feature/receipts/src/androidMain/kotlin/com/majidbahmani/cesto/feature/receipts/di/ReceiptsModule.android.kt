package com.majidbahmani.cesto.feature.receipts.di

import android.content.Context
import com.majidbahmani.cesto.core.di.DefaultDispatcher
import com.majidbahmani.cesto.core.di.IoDispatcher
import com.majidbahmani.cesto.feature.receipts.data.local.AndroidPdfTextExtractor
import com.majidbahmani.cesto.feature.receipts.data.local.AndroidReceiptFileStore
import com.majidbahmani.cesto.feature.receipts.data.local.PdfTextExtractor
import com.majidbahmani.cesto.feature.receipts.data.local.ReceiptFileStore
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val receiptPlatformModule: Module = module {
    single<ReceiptFileStore> { AndroidReceiptFileStore(context = get<Context>(), ioDispatcher = get(IoDispatcher)) }
    single<PdfTextExtractor> { AndroidPdfTextExtractor(context = get<Context>(), defaultDispatcher = get(DefaultDispatcher)) }
}
