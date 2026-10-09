package com.majidbahmani.cesto.feature.settings.di

import com.majidbahmani.cesto.feature.settings.data.repository.AccountSessionRepositoryImpl
import com.majidbahmani.cesto.feature.settings.data.repository.GeminiKeyRepositoryImpl
import com.majidbahmani.cesto.feature.settings.domain.repository.AccountSessionRepository
import com.majidbahmani.cesto.feature.settings.domain.repository.GeminiKeyRepository
import com.majidbahmani.cesto.feature.settings.domain.usecase.ObserveGeminiKeyUseCase
import com.majidbahmani.cesto.feature.settings.domain.usecase.ObserveSignedInAccountUseCase
import com.majidbahmani.cesto.feature.settings.domain.usecase.RemoveGeminiKeyUseCase
import com.majidbahmani.cesto.feature.settings.domain.usecase.SaveGeminiKeyUseCase
import com.majidbahmani.cesto.feature.settings.domain.usecase.SignOutUseCase
import com.majidbahmani.cesto.feature.settings.presentation.viewmodel.AccountViewModel
import com.majidbahmani.cesto.feature.settings.presentation.viewmodel.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Needs GeminiKeyStore and GeminiApi from :llm, AccountRepository from :account and the GoogleIdTokenProvider. */
val settingsModule = module {
    single<GeminiKeyRepository> { GeminiKeyRepositoryImpl(store = get(), gemini = get()) }
    factory { ObserveGeminiKeyUseCase(repository = get()) }
    factory { SaveGeminiKeyUseCase(repository = get()) }
    factory { RemoveGeminiKeyUseCase(repository = get()) }
    single<AccountSessionRepository> { AccountSessionRepositoryImpl(accounts = get(), googleIdTokens = get()) }
    factory { ObserveSignedInAccountUseCase(repository = get()) }
    factory { SignOutUseCase(repository = get()) }
    viewModel { AccountViewModel(observeSignedInAccount = get(), signOut = get()) }
    viewModel {
        SettingsViewModel(observeGeminiKey = get(), saveGeminiKey = get(), removeGeminiKey = get())
    }
}
