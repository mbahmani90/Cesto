package com.majidbahmani.cesto.di

import app.cash.sqldelight.db.SqlDriver

/** In-memory database for the Koin graph test (the real driver needs a Context / a file). */
expect fun createTestDriver(): SqlDriver
