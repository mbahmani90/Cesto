package com.majidbahmani.cesto.account

import com.majidbahmani.cesto.account.dto.ErrorResponseDto
import com.majidbahmani.cesto.account.dto.RefreshTokenResponseDto
import com.majidbahmani.cesto.account.dto.SignInWithIdpRequestDto
import com.majidbahmani.cesto.account.dto.SignInWithIdpResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode
import io.ktor.http.parameters
import kotlinx.serialization.json.Json

/** A fresh Identity Platform session for one user. */
data class IdentityTokens(
    val localId: String,
    val idToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null
)

/**
 * Google Cloud Identity Platform REST API: exchanges a Google ID token for an Identity Platform user and
 * refreshes its ID token. The [apiKey] goes in the `x-goog-api-key` header, never in the URL.
 * Throws [IdentityPlatformException] when Google rejects a request; network errors pass through.
 */
class IdentityPlatformApi(private val client: HttpClient, private val apiKey: String) {

    /** Signs in (and signs up the first time) with a Google ID token whose audience is our Web client ID. */
    suspend fun signInWithGoogle(googleIdToken: String): IdentityTokens = rejectionsMapped {
        val response: SignInWithIdpResponseDto = client.post(IDENTITY_TOOLKIT_URL + "accounts:signInWithIdp") {
            header(API_KEY_HEADER, apiKey)
            contentType(ContentType.Application.Json)
            setBody(
                SignInWithIdpRequestDto(
                    postBody = parameters {
                        append("id_token", googleIdToken)
                        append("providerId", GOOGLE_PROVIDER)
                    }.formUrlEncode()
                )
            )
        }.body()
        IdentityTokens(
            localId = response.localId,
            idToken = response.idToken,
            refreshToken = response.refreshToken,
            expiresInSeconds = response.expiresIn.toLong(),
            email = response.email,
            displayName = response.displayName,
            photoUrl = response.photoUrl
        )
    }

    /** A new ID token (valid for an hour) from the refresh token; the refresh token may change too. */
    suspend fun refresh(refreshToken: String): IdentityTokens = rejectionsMapped {
        val response: RefreshTokenResponseDto = client.submitForm(
            url = SECURE_TOKEN_URL,
            formParameters = parameters {
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
            }
        ) {
            header(API_KEY_HEADER, apiKey)
        }.body()
        IdentityTokens(
            localId = response.userId,
            idToken = response.idToken,
            refreshToken = response.refreshToken,
            expiresInSeconds = response.expiresIn.toLong()
        )
    }

    private suspend fun <T> rejectionsMapped(block: suspend () -> T): T = try {
        block()
    } catch (e: ClientRequestException) {
        val message = runCatching { json.decodeFromString<ErrorResponseDto>(e.response.bodyAsText()).error.message }
            .getOrDefault("")
        throw IdentityPlatformException(IdentityPlatformError.from(message), e.response.status.value)
    }

    companion object {
        const val IDENTITY_TOOLKIT_URL = "https://identitytoolkit.googleapis.com/v1/"
        const val SECURE_TOKEN_URL = "https://securetoken.googleapis.com/v1/token"
        const val API_KEY_HEADER = "x-goog-api-key"
        const val GOOGLE_PROVIDER = "google.com"
        private val json = Json { ignoreUnknownKeys = true }
    }
}

/** Why Identity Platform said no. */
enum class IdentityPlatformError {
    /** The Google ID token is invalid, expired, or issued to a client ID that isn't allowed. */
    INVALID_GOOGLE_TOKEN,

    /** The refresh token no longer works (revoked, user deleted or disabled): sign in again. */
    SESSION_ENDED,

    /** The API key is wrong or not allowed to call this API (key restrictions). */
    API_KEY_REJECTED,

    /** New accounts are turned off in Identity Platform's settings. */
    SIGN_UP_DISABLED,

    /** Anything else (quota, unknown message). */
    FAILED;

    internal companion object {
        /** Google's message is a code, sometimes followed by " : details". */
        fun from(message: String): IdentityPlatformError = when (message.substringBefore(' ').substringBefore(':')) {
            "INVALID_IDP_RESPONSE", "INVALID_ID_TOKEN", "MISSING_OR_INVALID_NONCE" -> INVALID_GOOGLE_TOKEN

            "TOKEN_EXPIRED", "INVALID_REFRESH_TOKEN", "USER_DISABLED", "USER_NOT_FOUND" -> SESSION_ENDED

            "API_KEY_SERVICE_BLOCKED", "API_KEY_INVALID", "API_KEY_HTTP_REFERRER_BLOCKED",
            "API_KEY_ANDROID_APP_BLOCKED", "API_KEY_IOS_APP_BLOCKED" -> API_KEY_REJECTED

            "ADMIN_ONLY_OPERATION", "OPERATION_NOT_ALLOWED" -> SIGN_UP_DISABLED

            else -> if (message.startsWith("API key not valid")) API_KEY_REJECTED else FAILED
        }
    }
}

class IdentityPlatformException(val error: IdentityPlatformError, val httpStatus: Int) :
    Exception("Identity Platform rejected the request: $error (HTTP $httpStatus)")
