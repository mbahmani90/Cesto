package com.majidbahmani.cesto.account

import com.majidbahmani.cesto.core.network.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.http.parseUrlEncodedParameters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class IdentityPlatformApiTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun api(status: HttpStatusCode = HttpStatusCode.OK, body: String) = IdentityPlatformApi(
        createHttpClient(
            MockEngine { request ->
                requests += request
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        ),
        apiKey = "AIza-test"
    )

    private fun HttpRequestData.bodyText() = (body as OutgoingContent.ByteArrayContent).bytes().decodeToString()

    private val signInBody = """{"kind":"identitytoolkit#VerifyAssertionResponse","localId":"user-1","email":"ana@example.com",
        |"displayName":"Ana","idToken":"id-1","refreshToken":"refresh-1","expiresIn":"3600","providerId":"google.com"}
    """.trimMargin()

    @Test
    fun signIn_sendsTheGoogleTokenAsPostBody_keyInHeader() = runTest {
        val tokens = api(body = signInBody).signInWithGoogle("google+token/=")

        assertEquals(IdentityTokens("user-1", "id-1", "refresh-1", 3600, "ana@example.com", "Ana"), tokens)
        val request = requests.single()
        assertEquals("https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp", request.url.toString())
        assertEquals("AIza-test", request.headers[IdentityPlatformApi.API_KEY_HEADER])
        assertFalse(request.url.toString().contains("AIza-test"))
        val json = Json.parseToJsonElement(request.bodyText()).jsonObject
        val postBody = json["postBody"]!!.jsonPrimitive.content.parseUrlEncodedParameters()
        assertEquals("google+token/=", postBody["id_token"])
        assertEquals("google.com", postBody["providerId"])
        assertEquals("true", json["returnSecureToken"]!!.jsonPrimitive.content)
    }

    @Test
    fun refresh_sendsAForm_readsSnakeCase() = runTest {
        val api =
            api(body = """{"id_token":"id-2","refresh_token":"refresh-2","expires_in":"3600","user_id":"user-1","token_type":"Bearer"}""")

        assertEquals(IdentityTokens("user-1", "id-2", "refresh-2", 3600), api.refresh("refresh-1"))
        val request = requests.single()
        assertEquals("https://securetoken.googleapis.com/v1/token", request.url.toString())
        val form = (request.body as FormDataContent).formData
        assertEquals("refresh_token", form["grant_type"])
        assertEquals("refresh-1", form["refresh_token"])
    }

    @Test
    fun rejections_mapToTheReason() = runTest {
        suspend fun errorFor(message: String, status: HttpStatusCode = HttpStatusCode.BadRequest) =
            assertFailsWith<IdentityPlatformException> {
                api(status, """{"error":{"code":${status.value},"message":"$message"}}""").signInWithGoogle("t")
            }.error

        assertEquals(IdentityPlatformError.INVALID_GOOGLE_TOKEN, errorFor("INVALID_IDP_RESPONSE : Invalid Idp Response"))
        assertEquals(IdentityPlatformError.SESSION_ENDED, errorFor("TOKEN_EXPIRED"))
        assertEquals(IdentityPlatformError.SESSION_ENDED, errorFor("USER_DISABLED"))
        assertEquals(IdentityPlatformError.API_KEY_REJECTED, errorFor("API key not valid. Please pass a valid API key."))
        assertEquals(IdentityPlatformError.API_KEY_REJECTED, errorFor("API_KEY_SERVICE_BLOCKED", HttpStatusCode.Forbidden))
        assertEquals(IdentityPlatformError.SIGN_UP_DISABLED, errorFor("ADMIN_ONLY_OPERATION"))
        assertEquals(IdentityPlatformError.FAILED, errorFor("QUOTA_EXCEEDED", HttpStatusCode.TooManyRequests))
        assertEquals(IdentityPlatformError.FAILED, errorFor("not json", HttpStatusCode.BadRequest))
    }
}
