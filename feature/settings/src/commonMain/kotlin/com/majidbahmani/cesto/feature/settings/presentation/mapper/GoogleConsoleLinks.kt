package com.majidbahmani.cesto.feature.settings.presentation.mapper

/** Step 1: a new Google Cloud project (billing is turned on there). */
internal const val CLOUD_PROJECT_URL = "https://console.cloud.google.com/projectcreate"

/** Step 2: API keys in Google AI Studio, where the project is chosen. */
internal const val AI_STUDIO_KEYS_URL = "https://aistudio.google.com/apikey"

/**
 * [url] for a specific Google account: Google's web apps take `authuser=<email>` and switch to it (or ask
 * to sign in to it). Without a usable email the browser's current account is used.
 */
internal fun withGoogleAccount(url: String, email: String): String {
    val account = email.trim()
    if (!looksLikeEmail(account)) return url
    val separator = if ('?' in url) '&' else '?'
    return "$url${separator}authuser=${encodeQueryValue(account)}"
}

internal fun looksLikeEmail(text: String): Boolean = EMAIL.matches(text.trim())

private val EMAIL = Regex("""[^\s@]+@[^\s@]+\.[^\s@]+""")

/** Percent-encodes everything but unreserved characters (RFC 3986), e.g. "@" → %40, "+" → %2B. */
private fun encodeQueryValue(value: String): String = buildString {
    value.encodeToByteArray().forEach { byte ->
        val c = byte.toInt().toChar()
        if (c.isLetterOrDigit() && c.code < 128 || c in "-._~") {
            append(c)
        } else {
            append('%').append((byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0'))
        }
    }
}
