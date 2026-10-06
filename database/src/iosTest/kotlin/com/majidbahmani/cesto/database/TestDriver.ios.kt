package com.majidbahmani.cesto.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver

actual fun createTestDriver(): SqlDriver = inMemoryDriver(CestoDatabase.Schema)
