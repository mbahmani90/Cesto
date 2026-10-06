package com.majidbahmani.cesto.database.di

import com.majidbahmani.cesto.database.CestoDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

internal const val DATABASE_NAME = "cesto.db"

/** SqlDriver per platform (Android needs the Koin Android context); closed when Koin stops. */
internal expect val sqlDriverModule: Module

val databaseModule: Module = module {
    includes(sqlDriverModule)

    // One database for the app; generated queries are cheap properties of it.
    single { CestoDatabase(driver = get()) }
}
