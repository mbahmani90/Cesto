package com.majidbahmani.cesto.llm.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Encrypts the Gemini key with an AES-GCM key that lives in the Android Keystore (it never leaves it)
 * and keeps only the ciphertext in its own DataStore file, separate from the plain settings.
 */
class AndroidGeminiKeyStore(private val dataStore: DataStore<Preferences>, private val ioDispatcher: CoroutineDispatcher) : GeminiKeyStore {

    override val key: Flow<String?> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e } // unreadable → no key
        .map { prefs -> prefs[ENCRYPTED_KEY] }
        .distinctUntilChanged()
        .map { encrypted -> encrypted?.let { runCatching { decrypt(it) }.getOrNull() } } // Keystore key lost → no key
        .flowOn(ioDispatcher)

    override suspend fun save(key: String) {
        val encrypted = withContext(ioDispatcher) { encrypt(key) }
        dataStore.edit { it[ENCRYPTED_KEY] = encrypted }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(ENCRYPTED_KEY) }
    }

    private fun encrypt(key: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        return Base64.encodeToString(cipher.iv + cipher.doFinal(key.encodeToByteArray()), Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String {
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
        val ENCRYPTED_KEY = stringPreferencesKey("gemini_key")
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "cesto_gemini_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
