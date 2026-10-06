package com.majidbahmani.cesto.feature.onboarding.domain.usecase

import com.majidbahmani.cesto.feature.onboarding.domain.repository.GmailConnectionRepository

/** Decides at app start whether onboarding can be skipped: access granted earlier still counts. */
class CheckGmailConnectionUseCase(
    private val repository: GmailConnectionRepository,
) {
    suspend operator fun invoke(): Boolean = repository.isConnected()
}
