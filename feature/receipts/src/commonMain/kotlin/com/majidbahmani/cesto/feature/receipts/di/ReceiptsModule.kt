package com.majidbahmani.cesto.feature.receipts.di

import com.majidbahmani.cesto.core.di.IoDispatcher
import com.majidbahmani.cesto.feature.receipts.data.local.ReceiptLocalDataSource
import com.majidbahmani.cesto.feature.receipts.data.parser.ContinenteReceiptParser
import com.majidbahmani.cesto.feature.receipts.data.remote.GmailApi
import com.majidbahmani.cesto.feature.receipts.data.remote.GmailTokenProvider
import com.majidbahmani.cesto.feature.receipts.data.remote.KtorGmailApi
import com.majidbahmani.cesto.feature.receipts.data.repository.ReceiptRepositoryImpl
import com.majidbahmani.cesto.feature.receipts.domain.repository.ReceiptRepository
import com.majidbahmani.cesto.feature.receipts.domain.usecase.ObserveReceiptsUseCase
import com.majidbahmani.cesto.feature.receipts.domain.usecase.SyncReceiptsUseCase
import com.majidbahmani.cesto.feature.receipts.presentation.viewmodel.ReceiptsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import kotlin.time.Clock

/** ReceiptFileStore and PdfTextExtractor per platform (Android needs the Koin Android context). */
internal expect val receiptPlatformModule: Module

/** Needs HttpClient + IO dispatcher (:core), CestoDatabase (:database) and GmailAuthorizer (platform apps). */
val receiptsModule = module {
    includes(receiptPlatformModule)

    single { GmailTokenProvider(authorizer = get(), currentTimeMillis = { Clock.System.now().toEpochMilliseconds() }) }
    single<GmailApi> { KtorGmailApi(client = get(), tokens = get()) }
    single {
        ReceiptLocalDataSource(
            database = get(),
            ioDispatcher = get(IoDispatcher),
            currentTimeMillis = { Clock.System.now().toEpochMilliseconds() },
        )
    }
    single { ContinenteReceiptParser() }
    single<ReceiptRepository> {
        ReceiptRepositoryImpl(
            gmail = get(),
            local = get(),
            files = get(),
            textExtractor = get(),
            parser = get(),
            currentTimeMillis = { Clock.System.now().toEpochMilliseconds() },
        )
    }
    factory { ObserveReceiptsUseCase(repository = get()) }
    factory { SyncReceiptsUseCase(repository = get()) }
    viewModel { ReceiptsViewModel(observeReceipts = get(), syncReceipts = get()) }
}
