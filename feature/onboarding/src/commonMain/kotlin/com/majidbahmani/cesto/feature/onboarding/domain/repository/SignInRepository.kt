package com.majidbahmani.cesto.feature.onboarding.domain.repository

import com.majidbahmani.cesto.feature.onboarding.domain.model.SignInResult

interface SignInRepository {
    /** True if a Cesto account is signed in on this phone; never shows UI. */
    suspend fun isSignedIn(): Boolean

    /** Shows Google's account picker, then signs in to Cesto with the chosen account. */
    suspend fun signIn(): SignInResult
}
