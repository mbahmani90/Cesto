package com.majidbahmani.cesto.feature.onboarding.domain.usecase

import com.majidbahmani.cesto.feature.onboarding.domain.model.GmailConnectionResult
import com.majidbahmani.cesto.feature.onboarding.domain.repository.GmailConnectionRepository

/** The user's explicit "Connect Gmail": the only place the permission dialog is requested from. */
class ConnectGmailUseCase(
    private val repository: GmailConnectionRepository,
) {
    suspend operator fun invoke(): GmailConnectionResult = repository.connect()
}
