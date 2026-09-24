package com.sans.finance.data.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_settings")

@Singleton
class SecureAiSettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AiSettingsRepository {

    private val dataStore = context.dataStore

    override val settings: Flow<AiSettings> = dataStore.data.map { preferences ->
        val provider = preferences[KEY_PROVIDER]?.let { runCatching { AiProviderType.valueOf(it) }.getOrNull() }
            ?: AiProviderType.OFF

        val openAiApiKeyEncrypted = preferences[KEY_OPENAI_KEY].orEmpty()
        val openAiApiKey = runCatching { CryptoManager.decrypt(openAiApiKeyEncrypted) }
            .getOrDefault("").filter { it > ' ' && it.code < 127 }

        val openAiModel = (preferences[KEY_OPENAI_MODEL] ?: AiSettings().openAiModel)
            .filter { it > ' ' && it.code < 127 }

        val openRouterApiKeyEncrypted = preferences[KEY_OPENROUTER_KEY].orEmpty()
        val openRouterApiKey = runCatching { CryptoManager.decrypt(openRouterApiKeyEncrypted) }
            .getOrDefault("").filter { it > ' ' && it.code < 127 }

        val openRouterModel = (preferences[KEY_OPENROUTER_MODEL] ?: AiSettings().openRouterModel)
            .filter { it > ' ' && it.code < 127 }

        AiSettings(
            provider = provider,
            openAiApiKey = openAiApiKey,
            openAiModel = openAiModel,
            openRouterApiKey = openRouterApiKey,
            openRouterModel = openRouterModel
        )
    }

    override suspend fun setProvider(provider: AiProviderType) {
        dataStore.edit { prefs ->
            prefs[KEY_PROVIDER] = provider.name
        }
    }

    override suspend fun setOpenAiApiKey(apiKey: String) {
        val clean = apiKey.filter { it > ' ' && it.code < 127 }
        val encrypted = CryptoManager.encrypt(clean)
        dataStore.edit { prefs ->
            prefs[KEY_OPENAI_KEY] = encrypted
        }
    }

    override suspend fun setOpenAiModel(model: String) {
        val clean = model.filter { it > ' ' && it.code < 127 }
        dataStore.edit { prefs ->
            prefs[KEY_OPENAI_MODEL] = clean
        }
    }

    override suspend fun setOpenRouterApiKey(apiKey: String) {
        val clean = apiKey.filter { it > ' ' && it.code < 127 }
        val encrypted = CryptoManager.encrypt(clean)
        dataStore.edit { prefs ->
            prefs[KEY_OPENROUTER_KEY] = encrypted
        }
    }

    override suspend fun setOpenRouterModel(model: String) {
        val clean = model.filter { it > ' ' && it.code < 127 }
        dataStore.edit { prefs ->
            prefs[KEY_OPENROUTER_MODEL] = clean
        }
    }

    private object CryptoManager {
        private const val KEY_ALIAS = "ai_settings_key"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"

        private fun getOrCreateKey(): SecretKey {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEY_STORE
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            return keyGenerator.generateKey()
        }

        fun encrypt(plainText: String): String {
            if (plainText.isEmpty()) return ""
            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherTextBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)
            return "$ivBase64|$cipherTextBase64"
        }

        fun decrypt(encryptedData: String): String {
            if (encryptedData.isEmpty()) return ""
            val parts = encryptedData.split("|")
            if (parts.size != 2) return ""
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
            return String(cipher.doFinal(cipherText), Charsets.UTF_8)
        }
    }

    private companion object {
        val KEY_PROVIDER = stringPreferencesKey("provider")
        val KEY_OPENAI_KEY = stringPreferencesKey("openai_api_key")
        val KEY_OPENAI_MODEL = stringPreferencesKey("openai_model")
        val KEY_OPENROUTER_KEY = stringPreferencesKey("openrouter_api_key")
        val KEY_OPENROUTER_MODEL = stringPreferencesKey("openrouter_model")
    }
}
