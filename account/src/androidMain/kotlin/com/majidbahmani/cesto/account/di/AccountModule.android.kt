package com.majidbahmani.cesto.account.di

import android.content.Context
import com.majidbahmani.cesto.account.AndroidSessionStore
import com.majidbahmani.cesto.account.SessionStore
import com.majidbahmani.cesto.core.di.IoDispatcher
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val sessionStoreModule: Module = module {
    single<SessionStore> { AndroidSessionStore(context = get<Context>(), ioDispatcher = get(IoDispatcher)) }
}
