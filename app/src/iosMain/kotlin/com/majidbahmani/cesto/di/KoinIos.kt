package com.majidbahmani.cesto.di

import com.majidbahmani.cesto.gmailauth.GmailAuthorizer
import com.majidbahmani.cesto.gmailauth.GoogleIdTokenProvider

/**
 * Swift entry point: `KoinIosKt.doInitKoinIos(gmailAuthorizer:googleIdTokenProvider:identityPlatformApiKey:)`.
 * Kotlin default arguments aren't visible from Swift, hence this wrapper.
 */
fun initKoinIos(gmailAuthorizer: GmailAuthorizer, googleIdTokenProvider: GoogleIdTokenProvider, identityPlatformApiKey: String) {
    initKoin(PlatformServices(gmailAuthorizer, googleIdTokenProvider, identityPlatformApiKey))
}
