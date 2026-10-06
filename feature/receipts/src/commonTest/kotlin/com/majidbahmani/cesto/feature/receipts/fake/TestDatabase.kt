package com.majidbahmani.cesto.feature.receipts.fake

import app.cash.sqldelight.db.SqlDriver

/** A fresh in-memory database with the schema created (same as :database's tests; KMP has no shared test fixtures). */
expect fun createTestDriver(): SqlDriver
