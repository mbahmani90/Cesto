package com.majidbahmani.cesto.feature.onboarding.data.repository

import com.majidbahmani.cesto.feature.onboarding.domain.model.GmailConnectionResult
import com.majidbahmani.cesto.feature.onboarding.domain.repository.GmailConnectionRepository
import com.majidbahmani.cesto.gmailauth.GmailAccess
import com.majidbahmani.cesto.gmailauth.GmailAuthError
import com.majidbahmani.cesto.gmailauth.GmailAuthorizer
import com.majidbahmani.cesto.gmailauth.requestAccess

/** Onboarding only needs to know *whether* access is granted; the token itself is used later by receipts. */
class GmailConnectionRepositoryImpl(
    private val authorizer: GmailAuthorizer,
) : GmailConnectionRepository {

    override suspend fun isConnected(): Boolean =
        authorizer.requestAccess(interactive = false) is GmailAccess.Granted

    override suspend fun connect(): GmailConnectionResult =
        when (val access = authorizer.requestAccess(interactive = true)) {
            is GmailAccess.Granted -> GmailConnectionResult.CONNECTED
            is GmailAccess.Denied -> when (access.error) {
                GmailAuthError.CANCELLED -> GmailConnectionResult.CANCELLED
                GmailAuthError.NOT_GRANTED -> GmailConnectionResult.PERMISSION_DENIED
                GmailAuthError.FAILED -> GmailConnectionResult.FAILED
            }
        }
}
