package com.majidbahmani.cesto.account.di

import com.majidbahmani.cesto.account.InMemorySessionStore
import com.majidbahmani.cesto.account.SessionStore
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val sessionStoreModule: Module = module {
    single<SessionStore> { InMemorySessionStore() }
}
