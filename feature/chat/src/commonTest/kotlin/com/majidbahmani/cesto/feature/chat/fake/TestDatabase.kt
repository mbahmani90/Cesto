package com.majidbahmani.cesto.feature.chat.fake

import app.cash.sqldelight.db.SqlDriver

/** A fresh in-memory database with the schema created (KMP has no shared test fixtures). */
expect fun createTestDriver(): SqlDriver
