package com.sans.finance.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sans.finance.domain.preferences.AiPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.aiDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_settings")

class AiPreferencesImpl(
    private val context: Context
) : AiPreferences {

    companion object {
        private val AI_MODEL_PATH_KEY = stringPreferencesKey("ai_model_path")
    }

    override fun getAiModelPath(): Flow<String?> {
        return context.aiDataStore.data.map { preferences ->
            preferences[AI_MODEL_PATH_KEY]
        }
    }

    override suspend fun setAiModelPath(path: String) {
        context.aiDataStore.edit { preferences ->
            preferences[AI_MODEL_PATH_KEY] = path
        }
    }
}
