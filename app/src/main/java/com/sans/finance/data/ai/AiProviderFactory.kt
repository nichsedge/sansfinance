package com.sans.finance.data.ai

import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiProviderFactory @Inject constructor(
    private val httpClient: OkHttpClient,
    private val settingsRepository: AiSettingsRepository
) {
    suspend fun create(): AiProvider? {
        val settings = settingsRepository.settings.first()

        return when (settings.provider) {
            AiProviderType.OFF -> null
            AiProviderType.OPENAI -> {
                if (settings.openAiApiKey.isBlank()) null
                else OpenAiResponsesProvider(
                    httpClient,
                    settings.openAiApiKey,
                    settings.openAiModel
                )
            }

            AiProviderType.OPENROUTER -> {
                if (settings.openRouterApiKey.isBlank()) null
                else OpenRouterChatProvider(
                    httpClient,
                    settings.openRouterApiKey,
                    settings.openRouterModel
                )
            }
        }
    }
}
