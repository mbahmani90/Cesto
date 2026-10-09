package com.majidbahmani.cesto.account

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** One signed-in Cesto account. [localId] is the Identity Platform user id, stable across devices. */
@Serializable
data class Account(val localId: String, val email: String?, val displayName: String? = null, val photoUrl: String? = null)

/** What survives an app restart: every signed-in account with its refresh token, and the active one. */
@Serializable
data class Sessions(val accounts: List<StoredAccount> = emptyList(), val activeLocalId: String? = null) {
    @Serializable
    data class StoredAccount(val account: Account, val refreshToken: String)
}

/**
 * Refresh tokens are as good as a password for the account: stored encrypted on the phone only
 * (Android Keystore, iOS Keychain later), never logged.
 */
interface SessionStore {
    /** The saved sessions; emits again after each [save]. */
    val sessions: Flow<Sessions>

    suspend fun save(sessions: Sessions)
}
