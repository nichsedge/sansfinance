package com.sans.finance.data.ai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sans.finance.core.util.CryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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



    private companion object {
        val KEY_PROVIDER = stringPreferencesKey("provider")
        val KEY_OPENAI_KEY = stringPreferencesKey("openai_api_key")
        val KEY_OPENAI_MODEL = stringPreferencesKey("openai_model")
        val KEY_OPENROUTER_KEY = stringPreferencesKey("openrouter_api_key")
        val KEY_OPENROUTER_MODEL = stringPreferencesKey("openrouter_model")
    }
}
