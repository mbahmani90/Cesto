package com.majidbahmani.cesto.feature.settings.presentation.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GoogleConsoleLinksTest {

    @Test
    fun email_isAddedAsAuthuser_encoded() {
        assertEquals(
            "https://aistudio.google.com/apikey?authuser=name.surname%2Bkeys%40gmail.com",
            withGoogleAccount(AI_STUDIO_KEYS_URL, "  name.surname+keys@gmail.com "),
        )
    }

    @Test
    fun existingQuery_getsAnAmpersand() {
        assertEquals("https://x.test/a?b=1&authuser=a%40b.pt", withGoogleAccount("https://x.test/a?b=1", "a@b.pt"))
    }

    @Test
    fun emptyOrInvalid_keepsTheBrowsersAccount() {
        assertEquals(CLOUD_PROJECT_URL, withGoogleAccount(CLOUD_PROJECT_URL, ""))
        assertEquals(CLOUD_PROJECT_URL, withGoogleAccount(CLOUD_PROJECT_URL, "not an email"))
        assertEquals(CLOUD_PROJECT_URL, withGoogleAccount(CLOUD_PROJECT_URL, "name@gmail"))
    }

    @Test
    fun looksLikeEmail_basicShape() {
        assertTrue(looksLikeEmail("someone@example.com"))
        assertFalse(looksLikeEmail("someone@"))
        assertFalse(looksLikeEmail("some one@example.com"))
    }
}
