package com.majidbahmani.cesto.account

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Thrown by [AccountRepository.validIdToken] when no account is signed in or its session ended. */
class NotSignedInException : Exception("No signed-in Cesto account")

/** The Cesto accounts on this phone. Several can be signed in; one is active. */
interface AccountRepository {
    val accounts: Flow<List<Account>>

    val activeAccount: Flow<Account?>

    /** Signs in with a Google ID token, adds the account (or updates it) and makes it active. */
    suspend fun signIn(googleIdToken: String): Account

    suspend fun switchTo(localId: String)

    /**
     * An Identity Platform ID token for the active account, for our own backend later. Refreshed when it
     * has less than 5 minutes left. Throws [NotSignedInException], and removes the account if its
     * session ended; network errors pass through.
     */
    suspend fun validIdToken(): String

    /** Removes the account from this phone; another one becomes active if there is one. */
    suspend fun signOut(localId: String)
}

class AccountRepositoryImpl(
    private val api: IdentityPlatformApi,
    private val store: SessionStore,
    private val currentTimeMillis: () -> Long
) : AccountRepository {

    /** ID tokens only live in memory: they expire in an hour and are cheap to get again. */
    private class CachedToken(val idToken: String, val expiresAtMillis: Long)

    private val idTokens = mutableMapOf<String, CachedToken>()
    private val mutex = Mutex()

    override val accounts: Flow<List<Account>> = store.sessions.map { sessions -> sessions.accounts.map { it.account } }

    override val activeAccount: Flow<Account?> = store.sessions.map { sessions ->
        sessions.accounts.firstOrNull { it.account.localId == sessions.activeLocalId }?.account
    }

    override suspend fun signIn(googleIdToken: String): Account {
        val tokens = api.signInWithGoogle(googleIdToken)
        val account = Account(tokens.localId, tokens.email, tokens.displayName, tokens.photoUrl)
        mutex.withLock {
            val sessions = store.sessions.first()
            val others = sessions.accounts.filterNot { it.account.localId == account.localId }
            store.save(Sessions(others + Sessions.StoredAccount(account, tokens.refreshToken), activeLocalId = account.localId))
            cache(tokens)
        }
        return account
    }

    override suspend fun switchTo(localId: String) = mutex.withLock {
        val sessions = store.sessions.first()
        require(sessions.accounts.any { it.account.localId == localId }) { "Unknown account" }
        store.save(sessions.copy(activeLocalId = localId))
    }

    override suspend fun validIdToken(): String = mutex.withLock {
        val sessions = store.sessions.first()
        val stored = sessions.accounts.firstOrNull { it.account.localId == sessions.activeLocalId }
            ?: throw NotSignedInException()
        idTokens[stored.account.localId]
            ?.takeIf { it.expiresAtMillis - currentTimeMillis() > REFRESH_MARGIN_MILLIS }
            ?.let { return@withLock it.idToken }

        val tokens = try {
            api.refresh(stored.refreshToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IdentityPlatformException) {
            if (e.error != IdentityPlatformError.SESSION_ENDED) throw e
            removeLocked(stored.account.localId)
            throw NotSignedInException()
        }
        if (tokens.refreshToken != stored.refreshToken) {
            store.save(
                sessions.copy(
                    accounts = sessions.accounts.map {
                        if (it.account.localId == stored.account.localId) it.copy(refreshToken = tokens.refreshToken) else it
                    }
                )
            )
        }
        cache(tokens)
        tokens.idToken
    }

    override suspend fun signOut(localId: String) = mutex.withLock { removeLocked(localId) }

    private suspend fun removeLocked(localId: String) {
        val sessions = store.sessions.first()
        val remaining = sessions.accounts.filterNot { it.account.localId == localId }
        val active = sessions.activeLocalId.takeIf { it != localId } ?: remaining.firstOrNull()?.account?.localId
        store.save(Sessions(remaining, active))
        idTokens.remove(localId)
    }

    private fun cache(tokens: IdentityTokens) {
        idTokens[tokens.localId] = CachedToken(tokens.idToken, currentTimeMillis() + tokens.expiresInSeconds * 1000)
    }

    private companion object {
        const val REFRESH_MARGIN_MILLIS = 5 * 60 * 1000L
    }
}
