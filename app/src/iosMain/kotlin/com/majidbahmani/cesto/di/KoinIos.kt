package com.majidbahmani.cesto.di

import com.majidbahmani.cesto.gmailauth.GmailAuthorizer

/**
 * Swift entry point: `KoinIosKt.doInitKoinIos(gmailAuthorizer:)`.
 * Kotlin default arguments aren't visible from Swift, hence this wrapper.
 */
fun initKoinIos(gmailAuthorizer: GmailAuthorizer) {
    initKoin(gmailAuthorizer)
}
