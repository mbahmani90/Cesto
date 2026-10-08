package com.majidbahmani.cesto.feature.receipts.di

import android.content.Context
import androidx.work.WorkManager
import com.majidbahmani.cesto.core.di.DefaultDispatcher
import com.majidbahmani.cesto.core.di.IoDispatcher
import com.majidbahmani.cesto.feature.receipts.data.local.AndroidPdfTextExtractor
import com.majidbahmani.cesto.feature.receipts.data.local.AndroidReceiptFileStore
import com.majidbahmani.cesto.feature.receipts.data.local.PdfTextExtractor
import com.majidbahmani.cesto.feature.receipts.data.local.ReceiptFileStore
import com.majidbahmani.cesto.feature.receipts.data.work.WorkManagerDailySpendingScheduler
import com.majidbahmani.cesto.feature.receipts.domain.repository.DailySpendingScheduler
import com.majidbahmani.cesto.feature.receipts.presentation.notification.DailySpendingNotifier
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val receiptPlatformModule: Module = module {
    single<ReceiptFileStore> { AndroidReceiptFileStore(context = get<Context>(), ioDispatcher = get(IoDispatcher)) }
    single<PdfTextExtractor> { AndroidPdfTextExtractor(context = get<Context>(), defaultDispatcher = get(DefaultDispatcher)) }
    // Only created here; CestoApp and the worker call it (see DailySpendingScheduler).
    single<DailySpendingScheduler> { WorkManagerDailySpendingScheduler(workManager = WorkManager.getInstance(get<Context>())) }
    single { DailySpendingNotifier(context = get<Context>()) }
}
