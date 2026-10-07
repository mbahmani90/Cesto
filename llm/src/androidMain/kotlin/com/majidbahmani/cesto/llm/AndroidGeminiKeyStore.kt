package com.majidbahmani.cesto.llm

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

/**
 * Encrypts the Gemini key with an AES-GCM key that lives in the Android Keystore (it never leaves it)
 * and keeps only the ciphertext in app-private preferences, which are excluded from backup.
 */
class AndroidGeminiKeyStore(context: Context, private val ioDispatcher: CoroutineDispatcher) : GeminiKeyStore {

    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    // Read from disk on first collection (on the IO dispatcher), then kept in memory and updated on save/clear.
    private val state = MutableStateFlow<String?>(null)

    @Volatile private var loaded = false

    override val key: Flow<String?> = flow {
        if (!loaded) {
            val stored = withContext(ioDispatcher) { runCatching { readKey() }.getOrNull() }
            if (!loaded) {
                state.value = stored
                loaded = true
            }
        }
        emitAll(state)
    }

    override suspend fun save(key: String) = withContext(ioDispatcher) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val encrypted = cipher.iv + cipher.doFinal(key.encodeToByteArray())
        preferences.edit().putString(PREF_KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
        loaded = true
        state.value = key
    }

    override suspend fun clear() = withContext(ioDispatcher) {
        preferences.edit().remove(PREF_KEY).apply()
        loaded = true
        state.value = null
    }

    private fun readKey(): String? {
        val stored = preferences.getString(PREF_KEY, null) ?: return null
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, IV_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
        }
        return cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES).decodeToString()
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
        const val ALIAS = "cesto_gemini_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        const val PREFERENCES = "cesto_secrets"
        const val PREF_KEY = "gemini_key"
    }
}
