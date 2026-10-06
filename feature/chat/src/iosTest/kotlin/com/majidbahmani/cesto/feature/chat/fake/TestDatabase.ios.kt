package com.majidbahmani.cesto.feature.chat.fake

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import com.majidbahmani.cesto.database.CestoDatabase

actual fun createTestDriver(): SqlDriver = inMemoryDriver(CestoDatabase.Schema)
