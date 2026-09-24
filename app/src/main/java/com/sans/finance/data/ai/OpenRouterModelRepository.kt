package com.sans.finance.data.ai

import android.util.Log
import com.sans.finance.domain.model.AiModelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenRouterModelRepository @Inject constructor(
    httpClient: OkHttpClient
) {
    private val client = httpClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private var cachedModels: List<AiModelInfo>? = null
    private var lastFetchTime: Long = 0L
    private val cacheTtlMillis = TimeUnit.HOURS.toMillis(6)

    suspend fun getModels(forceRefresh: Boolean = false): List<AiModelInfo> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedModels != null && (now - lastFetchTime < cacheTtlMillis)) {
            return@withContext cachedModels.orEmpty()
        }

        try {
            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/models")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w("OpenRouterModelRepo", "Failed to fetch models: HTTP ${resp.code}")
                    return@withContext cachedModels ?: getFallbackModels()
                }

                val body = resp.body.string()
                val root = json.parseToJsonElement(body).jsonObject
                val data = root["data"]?.jsonArray ?: return@withContext cachedModels ?: getFallbackModels()

                val models = data.mapNotNull { element ->
                    runCatching {
                        val obj = element.jsonObject
                        val id = obj["id"]?.jsonPrimitive?.content ?: return@runCatching null
                        val name = obj["name"]?.jsonPrimitive?.content ?: id.substringAfter("/")
                        val description = obj["description"]?.jsonPrimitive?.content.orEmpty().take(120)
                        val contextLength = obj["context_length"]?.jsonPrimitive?.intOrNull ?: 0

                        val pricingObj = obj["pricing"]?.jsonObject
                        val promptPricing = pricingObj?.get("prompt")?.jsonPrimitive?.doubleOrNull ?: 0.0
                        val completionPricing = pricingObj?.get("completion")?.jsonPrimitive?.doubleOrNull ?: 0.0

                        val isFree = id == "openrouter/free" ||
                            id.endsWith(":free", ignoreCase = true) ||
                            (promptPricing == 0.0 && completionPricing == 0.0)

                        AiModelInfo(
                            id = id,
                            name = name,
                            description = description,
                            contextLength = contextLength,
                            promptPricing = promptPricing,
                            completionPricing = completionPricing,
                            isFree = isFree
                        )
                    }.getOrNull()
                }

                if (models.isNotEmpty()) {
                    // Pre-sort models: Free first, then alphabetically, to avoid UI re-sort overhead
                    val sorted = models.sortedWith(
                        compareByDescending<AiModelInfo> { it.isFree }
                            .thenBy { it.name }
                    )
                    cachedModels = sorted
                    lastFetchTime = now
                    return@withContext sorted
                }
            }
        } catch (e: Exception) {
            Log.e("OpenRouterModelRepo", "Error fetching OpenRouter models", e)
        }

        cachedModels ?: getFallbackModels()
    }

    private fun getFallbackModels(): List<AiModelInfo> = listOf(
        AiModelInfo(
            id = "openrouter/free",
            name = "Free Models Router",
            description = "Auto-routes to currently available free models on OpenRouter.",
            contextLength = 200000,
            isFree = true
        ),
        AiModelInfo(
            id = "meta-llama/llama-3.3-70b-instruct:free",
            name = "Meta: Llama 3.3 70B Instruct (free)",
            description = "High intelligence 70B open weights model.",
            contextLength = 128000,
            isFree = true
        ),
        AiModelInfo(
            id = "google/gemini-2.0-flash-exp:free",
            name = "Google: Gemini 2.0 Flash Experimental (free)",
            description = "Next-gen multimodal, ultra-fast latency.",
            contextLength = 1000000,
            isFree = true
        ),
        AiModelInfo(
            id = "google/gemini-2.0-flash-001",
            name = "Google: Gemini 2.0 Flash",
            description = "Ultra-fast flagship performance with extreme cost efficiency.",
            contextLength = 1000000,
            promptPricing = 0.0000001,
            completionPricing = 0.0000004,
            isFree = false
        ),
        AiModelInfo(
            id = "openai/gpt-4.1-mini",
            name = "OpenAI: GPT-4.1 Mini",
            description = "Balanced intelligence and high responsiveness.",
            contextLength = 128000,
            promptPricing = 0.00000015,
            completionPricing = 0.0000006,
            isFree = false
        ),
        AiModelInfo(
            id = "deepseek/deepseek-chat",
            name = "DeepSeek: DeepSeek V3",
            description = "Top-tier open reasoning and structured data extraction.",
            contextLength = 64000,
            promptPricing = 0.00000014,
            completionPricing = 0.00000028,
            isFree = false
        ),
        AiModelInfo(
            id = "anthropic/claude-3.5-haiku",
            name = "Anthropic: Claude 3.5 Haiku",
            description = "Anthropic's fastest, highly accurate lightweight model.",
            contextLength = 200000,
            promptPricing = 0.0000008,
            completionPricing = 0.000004,
            isFree = false
        )
    )
}
