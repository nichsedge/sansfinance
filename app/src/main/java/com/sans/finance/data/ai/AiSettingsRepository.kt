package com.sans.finance.data.ai

import kotlinx.coroutines.flow.Flow

interface AiSettingsRepository {
    val settings: Flow<AiSettings>

    suspend fun setProvider(provider: AiProviderType)

    suspend fun setOpenAiApiKey(apiKey: String)
    suspend fun setOpenAiModel(model: String)

    suspend fun setOpenRouterApiKey(apiKey: String)
    suspend fun setOpenRouterModel(model: String)
}

