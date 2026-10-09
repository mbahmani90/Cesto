package com.majidbahmani.cesto.account.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `accounts:signInWithIdp` request: the Google ID token goes in [postBody] as form data. The client's Json
 * skips default values, so the fixed fields are always encoded.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class SignInWithIdpRequestDto(
    val postBody: String,
    /** Required by the API, unused for ID tokens. */
    @EncodeDefault val requestUri: String = "http://localhost",
    @EncodeDefault val returnSecureToken: Boolean = true,
    @EncodeDefault val returnIdpCredential: Boolean = true
)

@Serializable
internal data class SignInWithIdpResponseDto(
    val localId: String,
    val idToken: String,
    val refreshToken: String,
    /** Seconds, as a string ("3600"). */
    val expiresIn: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null
)

/** securetoken.googleapis.com answers in snake_case. */
@Serializable
internal data class RefreshTokenResponseDto(
    @SerialName("id_token") val idToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: String,
    @SerialName("user_id") val userId: String
)

/** Both APIs: `{"error":{"code":400,"message":"INVALID_IDP_RESPONSE : details"}}`. */
@Serializable
internal data class ErrorResponseDto(val error: ErrorDto) {
    @Serializable
    data class ErrorDto(val code: Int = 0, val message: String = "")
}
