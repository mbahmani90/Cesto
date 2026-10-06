package com.majidbahmani.cesto.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import com.majidbahmani.cesto.database.CestoDatabase

actual fun createTestDriver(): SqlDriver = inMemoryDriver(CestoDatabase.Schema)
