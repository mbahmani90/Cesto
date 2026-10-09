package com.majidbahmani.cesto.account

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * The sessions as JSON, encrypted with an AES-GCM key that lives in the Android Keystore (it never leaves
 * it); only the ciphertext is kept in app-private preferences, which are excluded from backup.
 */
class AndroidSessionStore(context: Context, private val ioDispatcher: CoroutineDispatcher) : SessionStore {

    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    // Read from disk on first collection (on the IO dispatcher), then kept in memory and updated on save.
    private val state = MutableStateFlow(Sessions())

    @Volatile private var loaded = false

    override val sessions: Flow<Sessions> = flow {
        if (!loaded) {
            // Unreadable (e.g. the Keystore key was lost): start signed out rather than crash.
            val stored = withContext(ioDispatcher) { runCatching { read() }.getOrNull() }
            if (!loaded) {
                state.value = stored ?: Sessions()
                loaded = true
            }
        }
        emitAll(state)
    }

    override suspend fun save(sessions: Sessions) = withContext(ioDispatcher) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val encrypted = cipher.iv + cipher.doFinal(Json.encodeToString(sessions).encodeToByteArray())
        preferences.edit().putString(PREF_KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
        loaded = true
        state.value = sessions
    }

    private fun read(): Sessions? {
        val stored = preferences.getString(PREF_KEY, null) ?: return null
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, IV_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
        }
        return Json.decodeFromString(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES).decodeToString())
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "cesto_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        const val PREFERENCES = "cesto_sessions"
        const val PREF_KEY = "sessions"
    }
}
