package com.majidbahmani.cesto.feature.receipts.di

import com.majidbahmani.cesto.feature.receipts.data.remote.GmailApi
import com.majidbahmani.cesto.feature.receipts.data.remote.GmailTokenProvider
import com.majidbahmani.cesto.feature.receipts.data.remote.KtorGmailApi
import org.koin.dsl.module

/** Needs the HttpClient (:core) and the GmailAuthorizer (platform apps, via :app). */
val receiptsModule = module {
    single { GmailTokenProvider(authorizer = get()) }
    single<GmailApi> { KtorGmailApi(client = get(), tokens = get()) }
}
