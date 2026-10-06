package com.majidbahmani.cesto.database.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.majidbahmani.cesto.database.CestoDatabase
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

internal actual val sqlDriverModule: Module = module {
    single<SqlDriver> { AndroidSqliteDriver(CestoDatabase.Schema, get<Context>(), DATABASE_NAME) } onClose { it?.close() }
}
