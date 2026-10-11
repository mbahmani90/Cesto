package com.majidbahmani.cesto.llm.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.majidbahmani.cesto.llm.data.local.GeminiKeyStore
import com.majidbahmani.cesto.llm.data.local.InMemoryGeminiKeyStore
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

internal actual val llmPlatformModule: Module = module {
    single<GeminiKeyStore> { InMemoryGeminiKeyStore() }
    single<DataStore<Preferences>> { settingsDataStore { documentsPath() + "/" + SETTINGS_FILE } }
}

@OptIn(ExperimentalForeignApi::class)
private fun documentsPath(): String {
    val documents = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    return requireNotNull(documents?.path) { "No Documents directory" }
}
