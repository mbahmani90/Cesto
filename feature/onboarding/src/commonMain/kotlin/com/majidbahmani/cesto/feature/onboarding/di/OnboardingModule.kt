package com.majidbahmani.cesto.feature.onboarding.di

import com.majidbahmani.cesto.feature.onboarding.data.repository.GmailConnectionRepositoryImpl
import com.majidbahmani.cesto.feature.onboarding.domain.repository.GmailConnectionRepository
import com.majidbahmani.cesto.feature.onboarding.domain.usecase.CheckGmailConnectionUseCase
import com.majidbahmani.cesto.feature.onboarding.domain.usecase.ConnectGmailUseCase
import com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel.OnboardingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Needs a GmailAuthorizer, provided by :app from the platform implementation. */
val onboardingModule = module {
    single<GmailConnectionRepository> { GmailConnectionRepositoryImpl(authorizer = get()) }
    factory { CheckGmailConnectionUseCase(repository = get()) }
    factory { ConnectGmailUseCase(repository = get()) }
    viewModel { OnboardingViewModel(checkGmailConnection = get(), connectGmail = get()) }
}
