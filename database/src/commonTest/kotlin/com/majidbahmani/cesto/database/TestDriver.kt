package com.majidbahmani.cesto.database

import app.cash.sqldelight.db.SqlDriver

/** A fresh in-memory database with the schema created. */
expect fun createTestDriver(): SqlDriver
