package com.sans.finance.core.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoManager {
    private const val KEY_ALIAS = "sans_finance_keystore_key"
    private const val LEGACY_KEY_ALIAS = "ai_settings_key"
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"

    private fun getOrCreateKey(alias: String = KEY_ALIAS): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        keyStore.getKey(alias, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private fun getExistingKey(alias: String): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        return keyStore.getKey(alias, null) as? SecretKey
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(KEY_ALIAS))
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherTextBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)
        return "$ivBase64|$cipherTextBase64"
    }

    private fun tryDecrypt(key: SecretKey, iv: ByteArray, cipherText: ByteArray): String? = try {
        val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        String(cipher.doFinal(cipherText), Charsets.UTF_8)
    } catch (_: Exception) {
        null
    }

    fun decrypt(encryptedData: String): String {
        if (encryptedData.isEmpty()) return ""
        val parts = encryptedData.split("|")
        val iv = if (parts.size == 2) {
            runCatching { Base64.decode(parts[0], Base64.NO_WRAP) }.getOrNull()
        } else {
            null
        }
        val cipherText = if (parts.size == 2) {
            runCatching { Base64.decode(parts[1], Base64.NO_WRAP) }.getOrNull()
        } else {
            null
        }

        val decrypted = if (iv != null && cipherText != null) {
            val primaryKey = getOrCreateKey(KEY_ALIAS)
            tryDecrypt(primaryKey, iv, cipherText)
                ?: getExistingKey(LEGACY_KEY_ALIAS)?.let { tryDecrypt(it, iv, cipherText) }
        } else null

        return decrypted ?: encryptedData
    }
}
