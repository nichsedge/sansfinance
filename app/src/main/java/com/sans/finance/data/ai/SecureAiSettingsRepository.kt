package com.sans.finance.data.ai

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureAiSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : AiSettingsRepository {

    @Suppress("DEPRECATION")

    private val prefs = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "ai_settings_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val state = MutableStateFlow(load())

    override val settings: Flow<AiSettings> = state.asStateFlow()

    override suspend fun setProvider(provider: AiProviderType) {
        prefs.edit { putString(KEY_PROVIDER, provider.name) }
        state.value = state.value.copy(provider = provider)
    }

    override suspend fun setOpenAiApiKey(apiKey: String) {
        prefs.edit { putString(KEY_OPENAI_KEY, apiKey.trim()) }
        state.value = state.value.copy(openAiApiKey = apiKey.trim())
    }

    override suspend fun setOpenAiModel(model: String) {
        val clean = model.trim()
        prefs.edit { putString(KEY_OPENAI_MODEL, clean) }
        state.value = state.value.copy(openAiModel = clean)
    }

    override suspend fun setOpenRouterApiKey(apiKey: String) {
        prefs.edit { putString(KEY_OPENROUTER_KEY, apiKey.trim()) }
        state.value = state.value.copy(openRouterApiKey = apiKey.trim())
    }

    override suspend fun setOpenRouterModel(model: String) {
        val clean = model.trim()
        prefs.edit { putString(KEY_OPENROUTER_MODEL, clean) }
        state.value = state.value.copy(openRouterModel = clean)
    }

    private fun load(): AiSettings {
        val provider = prefs.getString(KEY_PROVIDER, AiProviderType.OFF.name)
            ?.let { runCatching { AiProviderType.valueOf(it) }.getOrNull() }
            ?: AiProviderType.OFF

        return AiSettings(
            provider = provider,
            openAiApiKey = prefs.getString(KEY_OPENAI_KEY, "").orEmpty(),
            openAiModel = prefs.getString(KEY_OPENAI_MODEL, AiSettings().openAiModel).orEmpty(),
            openRouterApiKey = prefs.getString(KEY_OPENROUTER_KEY, "").orEmpty(),
            openRouterModel = prefs.getString(KEY_OPENROUTER_MODEL, AiSettings().openRouterModel)
                .orEmpty()
        )
    }

    private companion object {
        private const val KEY_PROVIDER = "provider"
        private const val KEY_OPENAI_KEY = "openai_api_key"
        private const val KEY_OPENAI_MODEL = "openai_model"
        private const val KEY_OPENROUTER_KEY = "openrouter_api_key"
        private const val KEY_OPENROUTER_MODEL = "openrouter_model"
    }
}

