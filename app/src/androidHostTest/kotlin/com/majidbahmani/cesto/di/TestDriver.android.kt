package com.majidbahmani.cesto.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.majidbahmani.cesto.database.CestoDatabase

actual fun createTestDriver(): SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { CestoDatabase.Schema.create(it) }
