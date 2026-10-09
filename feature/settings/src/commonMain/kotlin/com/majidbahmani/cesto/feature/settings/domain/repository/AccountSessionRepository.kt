package com.majidbahmani.cesto.feature.settings.domain.repository

import kotlinx.coroutines.flow.Flow

interface AccountSessionRepository {
    /** The signed-in account's email (or another label when Google gave none); null when signed out. */
    fun observeSignedInEmail(): Flow<String?>

    /** Signs the active account out of Cesto and forgets Google's sign-in on this phone. */
    suspend fun signOut()
}
