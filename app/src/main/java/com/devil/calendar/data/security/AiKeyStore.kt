package com.devil.calendar.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.devil.calendar.data.ai.KeySanitizer
import com.devil.calendar.data.prefs.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the user's Sarvam key encrypted with an AES-256-GCM key that never
 * leaves the Android Keystore.
 *
 * Why not EncryptedSharedPreferences: that library is deprecated, and this is
 * ~80 lines of well-trodden crypto that adds no dependency and no black box.
 *
 * The plaintext key exists only in memory, is never logged, never written to
 * disk unencrypted, and never leaves the device except in the auth header of a
 * request to api.sarvam.ai.
 */
@Singleton
class AiKeyStore @Inject constructor(
    private val prefs: UserPreferences,
) {
    val hasKey: Flow<Boolean> = prefs.aiKeyCipher.map { !it.isNullOrBlank() }

    /** Encrypts and stores. Returns the sanitised key so callers can use it now. */
    suspend fun save(raw: String): String? {
        val key = KeySanitizer.sanitize(raw)
        if (!KeySanitizer.isUsable(key)) return null
        val blob = runCatching { encrypt(key) }.getOrNull() ?: return null
        prefs.setAiKeyCipher(blob)
        return key
    }

    suspend fun read(): String? {
        val blob = prefs.aiKeyCipher.first() ?: return null
        return runCatching { decrypt(blob) }.getOrNull()
    }

    suspend fun clear() = prefs.setAiKeyCipher(null)

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        // iv length is fixed at 12 bytes by GCM; store it in front of the body.
        val combined = ByteArray(iv.size + body.size)
        iv.copyInto(combined, 0)
        body.copyInto(combined, iv.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(blob: String): String {
        val combined = Base64.decode(blob, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, IV_BYTES)
        val body = combined.copyOfRange(IV_BYTES, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(body), Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (store.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val ALIAS = "devil_calendar_ai_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
