package com.majidbahmani.cesto.feature.settings.di

import com.majidbahmani.cesto.feature.settings.data.repository.GeminiKeyRepositoryImpl
import com.majidbahmani.cesto.feature.settings.domain.repository.GeminiKeyRepository
import com.majidbahmani.cesto.feature.settings.domain.usecase.ObserveGeminiKeyUseCase
import com.majidbahmani.cesto.feature.settings.domain.usecase.RemoveGeminiKeyUseCase
import com.majidbahmani.cesto.feature.settings.domain.usecase.SaveGeminiKeyUseCase
import com.majidbahmani.cesto.feature.settings.presentation.viewmodel.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Needs GeminiKeyStore and GeminiApi from :llm. */
val settingsModule = module {
    single<GeminiKeyRepository> { GeminiKeyRepositoryImpl(store = get(), gemini = get()) }
    factory { ObserveGeminiKeyUseCase(repository = get()) }
    factory { SaveGeminiKeyUseCase(repository = get()) }
    factory { RemoveGeminiKeyUseCase(repository = get()) }
    viewModel {
        SettingsViewModel(observeGeminiKey = get(), saveGeminiKey = get(), removeGeminiKey = get())
    }
}
