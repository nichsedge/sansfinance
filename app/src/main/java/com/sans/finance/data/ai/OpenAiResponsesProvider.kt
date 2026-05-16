package com.sans.finance.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OpenAiResponsesProvider(
    private val client: OkHttpClient,
    private val apiKey: String,
    private val model: String,
) : AiProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generateMonthlyReview(input: MonthlyReviewInput): MonthlyReviewResult {
        val bodyJson = buildJsonObject {
            put("model", JsonPrimitive(model))
            put(
                "instructions",
                JsonPrimitive(
                    """
                    You are a pragmatic personal finance coach for an Indonesia-based user.
                    Output concise, actionable guidance. Do not mention being an AI.
                    Return a short JSON object matching this schema:
                    { "headline": string, "insights": [{ "title": string, "why": string, "action": string, "severity": "INFO"|"WARN"|"CRITICAL" }] }
                    """.trimIndent()
                )
            )
            put(
                "input",
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
            put(
                "response_format",
                buildJsonObject {
                    put("type", JsonPrimitive("json_object"))
                }
            )
        }

        val request = Request.Builder()
            .url("https://api.openai.com/v1/responses")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { resp ->
                val raw = resp.body.string()
                if (!resp.isSuccessful) {
                    throw Exception("OpenAI error ${resp.code}: ${raw.take(500)}")
                }

                val root = json.parseToJsonElement(raw).jsonObject
                val outputText = root["output_text"]?.let { element ->
                    runCatching { element.jsonPrimitive.content }.getOrNull()
                }
                val parsed = outputText?.let { parseJsonResult(it) }
                parsed ?: MonthlyReviewResult(
                    headline = "Monthly review",
                    insights = emptyList(),
                    rawText = outputText ?: raw
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
