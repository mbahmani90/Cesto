package com.majidbahmani.cesto.core.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** For blocking work (SQLite, files) in the class that does it; injected so tests can replace it. */
val IoDispatcher = named("IoDispatcher")

val dispatchersModule = module {
    single<CoroutineDispatcher>(IoDispatcher) { Dispatchers.IO }
}
