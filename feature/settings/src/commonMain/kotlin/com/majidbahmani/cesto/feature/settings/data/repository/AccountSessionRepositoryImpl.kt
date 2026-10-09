package com.majidbahmani.cesto.feature.settings.data.repository

import com.majidbahmani.cesto.account.AccountRepository
import com.majidbahmani.cesto.feature.settings.domain.repository.AccountSessionRepository
import com.majidbahmani.cesto.gmailauth.GoogleIdTokenProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AccountSessionRepositoryImpl(private val accounts: AccountRepository, private val googleIdTokens: GoogleIdTokenProvider) :
    AccountSessionRepository {

    override fun observeSignedInEmail(): Flow<String?> = accounts.activeAccount.map { account ->
        account?.let { it.email ?: it.displayName ?: it.localId }
    }

    override suspend fun signOut() {
        val account = accounts.activeAccount.first() ?: return
        accounts.signOut(account.localId)
        googleIdTokens.signOut()
    }
}
