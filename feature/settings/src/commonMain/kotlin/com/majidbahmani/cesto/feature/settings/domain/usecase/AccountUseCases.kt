package com.majidbahmani.cesto.feature.settings.domain.usecase

import com.majidbahmani.cesto.feature.settings.domain.repository.AccountSessionRepository
import kotlinx.coroutines.flow.Flow

class ObserveSignedInAccountUseCase(private val repository: AccountSessionRepository) {
    operator fun invoke(): Flow<String?> = repository.observeSignedInEmail()
}

/** Receipts, the Gemini key and Gmail access stay on the phone: signing in again picks them up. */
class SignOutUseCase(private val repository: AccountSessionRepository) {
    suspend operator fun invoke() = repository.signOut()
}
