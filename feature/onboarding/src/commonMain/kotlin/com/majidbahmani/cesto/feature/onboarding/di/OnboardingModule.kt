package com.majidbahmani.cesto.feature.onboarding.di

import com.majidbahmani.cesto.feature.onboarding.data.repository.GmailConnectionRepositoryImpl
import com.majidbahmani.cesto.feature.onboarding.data.repository.SignInRepositoryImpl
import com.majidbahmani.cesto.feature.onboarding.domain.repository.GmailConnectionRepository
import com.majidbahmani.cesto.feature.onboarding.domain.repository.SignInRepository
import com.majidbahmani.cesto.feature.onboarding.domain.usecase.CheckGmailConnectionUseCase
import com.majidbahmani.cesto.feature.onboarding.domain.usecase.CheckSignInUseCase
import com.majidbahmani.cesto.feature.onboarding.domain.usecase.ConnectGmailUseCase
import com.majidbahmani.cesto.feature.onboarding.domain.usecase.SignInUseCase
import com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel.OnboardingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Needs a GmailAuthorizer and a GoogleIdTokenProvider (platform implementations, from :app) and the account module. */
val onboardingModule = module {
    single<GmailConnectionRepository> { GmailConnectionRepositoryImpl(authorizer = get()) }
    single<SignInRepository> { SignInRepositoryImpl(googleIdTokens = get(), accounts = get()) }
    factory { CheckGmailConnectionUseCase(repository = get()) }
    factory { ConnectGmailUseCase(repository = get()) }
    factory { CheckSignInUseCase(repository = get()) }
    factory { SignInUseCase(repository = get()) }
    viewModel {
        OnboardingViewModel(checkSignIn = get(), signIn = get(), checkGmailConnection = get(), connectGmail = get())
    }
}
