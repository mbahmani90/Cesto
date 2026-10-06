package com.majidbahmani.cesto.feature.settings.presentation.viewmodel

import com.majidbahmani.cesto.feature.settings.domain.model.SaveKeyResult

data class SettingsUiState(
    /** True until the key store has answered once. */
    val isLoading: Boolean = true,
    /** "AIza…x9Q2", or null when no key is saved. */
    val savedKeyMasked: String? = null,
    /** The two setup steps and the key field: shown without a key, or after "Change". */
    val isEditing: Boolean = false,
    /** Optional Google account the two steps open with; not saved. */
    val accountEmail: String = "",
    val keyInput: String = "",
    val isKeyVisible: Boolean = false,
    /** "Test and save" running. */
    val isTesting: Boolean = false,
    /** Result of the last "Test and save" (success or why it failed); cleared by typing. */
    val feedback: SaveKeyResult? = null,
)
