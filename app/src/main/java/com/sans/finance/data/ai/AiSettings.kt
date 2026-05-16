package com.sans.finance.data.ai

data class AiSettings(
    val provider: AiProviderType = AiProviderType.OFF,
    val openAiApiKey: String = "",
    val openAiModel: String = "gpt-5.4-mini",
    val openRouterApiKey: String = "",
    val openRouterModel: String = "openai/gpt-4.1-mini"
)

