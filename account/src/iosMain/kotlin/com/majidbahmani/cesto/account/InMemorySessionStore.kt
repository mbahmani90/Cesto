package com.majidbahmani.cesto.account

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** iOS for now: kept only while the app runs. TODO: store in the iOS Keychain. */
class InMemorySessionStore : SessionStore {
    private val state = MutableStateFlow(Sessions())

    override val sessions: StateFlow<Sessions> = state

    override suspend fun save(sessions: Sessions) {
        state.value = sessions
    }
}
