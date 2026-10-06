package com.majidbahmani.cesto.database.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.majidbahmani.cesto.database.CestoDatabase
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

internal actual val sqlDriverModule: Module = module {
    single<SqlDriver> { NativeSqliteDriver(CestoDatabase.Schema, DATABASE_NAME) } onClose { it?.close() }
}
