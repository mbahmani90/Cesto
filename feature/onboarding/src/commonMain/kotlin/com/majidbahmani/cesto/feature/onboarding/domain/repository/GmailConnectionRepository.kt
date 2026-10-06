package com.majidbahmani.cesto.feature.onboarding.domain.repository

import com.majidbahmani.cesto.feature.onboarding.domain.model.GmailConnectionResult

interface GmailConnectionRepository {
    /** True if Gmail access was granted before; never shows UI. */
    suspend fun isConnected(): Boolean

    /** Shows Google's account picker and consent dialog when needed. */
    suspend fun connect(): GmailConnectionResult
}
