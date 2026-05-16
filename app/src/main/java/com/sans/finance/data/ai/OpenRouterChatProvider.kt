package com.sans.finance.data.ai

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OpenRouterChatProvider(
    private val client: OkHttpClient,
    private val apiKey: String,
    private val model: String,
    private val appName: String = "Sans Finance",
) : AiProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generateMonthlyReview(input: MonthlyReviewInput): MonthlyReviewResult {
        val bodyJson = buildJsonObject {
            put("model", JsonPrimitive(model))
            put(
                "messages",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", JsonPrimitive("system"))
                            put(
                                "content",
                                JsonPrimitive(
                                    """
                                    You are a pragmatic personal finance coach for an Indonesia-based user.
                                    Output concise, actionable guidance. Do not mention being an AI.
                                    Return a short JSON object matching this schema:
                                    { "headline": string, "insights": [{ "title": string, "why": string, "action": string, "severity": "INFO"|"WARN"|"CRITICAL" }] }
                                    """.trimIndent()
                                )
                            )
                        }
                    )
                    add(
                        buildJsonObject {
                            put("role", JsonPrimitive("user"))
                            put(
                                "content",
                                JsonPrimitive(
                                    """
                                    Month: ${input.monthLabel}
                                    Currency: ${input.baseCurrency}
                                    Income: ${input.income}
                                    Expense: ${input.expense}
                                    Top categories: ${input.topCategories.joinToString { "${it.first}=${it.second}" }}
                                    Notes: ${input.notes}
                                    """.trimIndent()
                                )
                            )
                        }
                    )
                }
            )
            put(
                "response_format",
                buildJsonObject {
                    put("type", JsonPrimitive("json_object"))
                }
            )
            put("temperature", JsonPrimitive(0.2))
            put("max_tokens", JsonPrimitive(500))
        }

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("HTTP-Referer", "https://github.com/sans-finance/sans-finance")
            .header("X-OpenRouter-Title", appName)
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { resp ->
                val raw = resp.body.string()
                if (!resp.isSuccessful) {
                    throw Exception("OpenRouter error ${resp.code}: ${raw.take(500)}")
                }

                val root = json.parseToJsonElement(raw).jsonObject
                val content = root["choices"]
                    ?.jsonArray
                    ?.firstOrNull()
                    ?.jsonObject
                    ?.get("message")
                    ?.jsonObject
                    ?.get("content")
                    ?.let { element -> runCatching { element.jsonPrimitive.content }.getOrNull() }

                val parsed = content?.let { parseJsonResult(it) }
                parsed ?: MonthlyReviewResult(
                    headline = "Monthly review",
                    insights = emptyList(),
                    rawText = content ?: raw
                )
            }
        }
    }

    private fun parseJsonResult(text: String): MonthlyReviewResult? {
        return runCatching {
            val obj = json.parseToJsonElement(text).jsonObject
            val headline = obj["headline"]?.jsonPrimitive?.content ?: "Monthly review"
            val insightsJson = obj["insights"]
            val insights = if (insightsJson == null) {
                emptyList()
            } else {
                json.decodeFromString<List<MonthlyReviewInsight>>(insightsJson.toString())
            }
            MonthlyReviewResult(headline = headline, insights = insights, rawText = null)
        }.getOrNull()
    }
}
