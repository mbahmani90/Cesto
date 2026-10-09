package com.majidbahmani.cesto.feature.onboarding.domain.usecase

import com.majidbahmani.cesto.feature.onboarding.domain.model.SignInResult
import com.majidbahmani.cesto.feature.onboarding.domain.repository.SignInRepository

/** At app start: a signed-in account is the first condition for skipping onboarding. */
class CheckSignInUseCase(private val repository: SignInRepository) {
    suspend operator fun invoke(): Boolean = repository.isSignedIn()
}

/** The user's explicit "Continue with Google": the only place the sign-in dialog is requested from. */
class SignInUseCase(private val repository: SignInRepository) {
    suspend operator fun invoke(): SignInResult = repository.signIn()
}
