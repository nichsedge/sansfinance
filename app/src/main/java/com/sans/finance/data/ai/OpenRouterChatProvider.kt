package com.sans.finance.data.ai

import android.util.Log
import com.sans.finance.domain.model.AccountSummary
import com.sans.finance.domain.model.AiAssistantResponse
import com.sans.finance.domain.model.AiTransactionProposal
import com.sans.finance.domain.model.CategorySummary
import com.sans.finance.domain.model.ChatMessage
import com.sans.finance.domain.model.ChatSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class OpenRouterChatProvider(
    rawClient: OkHttpClient,
    rawApiKey: String,
    rawModel: String,
    private val appName: String = "Sans Finance",
    private val appUrl: String = "https://github.com/nichsedge/sansfinance",
) : AiProvider {

    private val apiKey = rawApiKey.filter { it > ' ' && it.code < 127 }
    private val model = rawModel.filter { it > ' ' && it.code < 127 }

    // Dedicated client with extended timeouts for AI reasoning/routers (90s for free queues)
    private val client = rawClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

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
            if (!model.contains("free", ignoreCase = true)) {
                put(
                    "response_format",
                    buildJsonObject {
                        put("type", JsonPrimitive("json_object"))
                    }
                )
            }
            put("temperature", JsonPrimitive(0.2))
            put("max_tokens", JsonPrimitive(500))
        }

        val rawContent = executeChatCompletion(bodyJson)
        val parsed = parseJsonResult(rawContent)
        return parsed ?: MonthlyReviewResult(
            headline = "Monthly review",
            insights = emptyList(),
            rawText = rawContent
        )
    }

    private fun parseJsonResult(text: String): MonthlyReviewResult? {
        val clean = AiJsonParser.extractJsonString(text)
        return runCatching {
            val obj = json.parseToJsonElement(clean).jsonObject
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

    override suspend fun generatePortfolioAnalysis(input: PortfolioAnalysisInput): PortfolioAnalysisResult {
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
                                    You are an expert wealth management advisor. 
                                    Analyze the user's portfolio and return a concise JSON object.
                                    Schema: { "summary": string, "insights": [{ "title": string, "observation": string, "suggestion": string, "importance": "LOW"|"MEDIUM"|"HIGH" }] }
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
                                    Date: ${input.dateLabel}
                                    Currency: ${input.currency}
                                    Total Value: ${input.totalValue}
                                    XIRR: ${input.xirr ?: "N/A"}
                                    Allocation: ${input.assetAllocation.joinToString { "${it.first}=${it.second}%" }}
                                    Health: ${input.healthStatus.joinToString()}
                                    Goals: ${input.goals.joinToString()}
                                    Notes: ${input.notes}
                                    """.trimIndent()
                                )
                            )
                        }
                    )
                }
            )
            if (!model.contains("free", ignoreCase = true)) {
                put(
                    "response_format",
                    buildJsonObject {
                        put("type", JsonPrimitive("json_object"))
                    }
                )
            }
            put("temperature", JsonPrimitive(0.3))
            put("max_tokens", JsonPrimitive(1000))
        }

        val rawContent = executeChatCompletion(bodyJson)
        val parsed = parsePortfolioJsonResult(rawContent)
        return parsed ?: PortfolioAnalysisResult(
            summary = "Portfolio Analysis",
            insights = emptyList(),
            rawText = rawContent
        )
    }

    private fun parsePortfolioJsonResult(text: String): PortfolioAnalysisResult? {
        val clean = AiJsonParser.extractJsonString(text)
        return runCatching {
            val obj = json.parseToJsonElement(clean).jsonObject
            val summary = obj["summary"]?.jsonPrimitive?.content ?: "Portfolio Analysis"
            val insightsJson = obj["insights"]
            val insights = if (insightsJson == null) {
                emptyList()
            } else {
                json.decodeFromString<List<PortfolioAnalysisInsight>>(insightsJson.toString())
            }
            PortfolioAnalysisResult(summary = summary, insights = insights, rawText = null)
        }.getOrNull()
    }

    override suspend fun parseReceiptOrChat(
        userMessage: String,
        accounts: List<AccountSummary>,
        categories: List<CategorySummary>,
        currency: String,
        conversationHistory: List<ChatMessage>
    ): AiAssistantResponse {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        val accountsFormatted = accounts.joinToString(separator = "\n") {
            "- ID: ${it.id} | Name: '${it.name}' | Type: ${it.type} | Currency: ${it.currency}"
        }
        val categoriesFormatted = categories.joinToString(separator = "\n") {
            "- ID: ${it.id} | Name: '${it.name}' | Type: ${it.type}"
        }

        val systemPrompt = """
            You are Sans Finance Copilot, an elite personal finance assistant and universal transaction harness for an Indonesia-based personal wealth and cashflow management app.
            Current Date: $todayStr
            Base Currency: $currency

            User's Registered Accounts:
            $accountsFormatted

            User's Registered Categories:
            $categoriesFormatted

            Capabilities & Modes:
            1. CONVERSATIONAL & FINANCIAL ADVICE MODE:
               - When the user asks financial questions, seeks budgeting strategies (50/30/20, zero-based), inquires about emergency fund sizing, cash runway, FIRE targets, debt payoff horizons, or general wealth habits:
               - Provide a clear, insightful, motivating, and mathematically sound answer in Indonesian (or English if prompted in English).
               - Set "proposals": [] (empty array). Do NOT invent fictional transactions.

            2. UNIVERSAL TRANSACTION INGESTION MODE:
               - When the user pastes ANY receipt, bank mutation, invoice, purchase confirmation, QRIS payment, bill, or income notification:
                 * Daily Spends & Shopping: Supermarket, groceries (Indomaret, Alfamart, Superindo), food & dining (GoFood, Grab, restaurants), coffee, e-commerce (Tokopedia, Shopee, Blibli), transport, fuel.
                 * Bills & Utilities: PLN electricity, PDAM, internet/WiFi, cellular, subscriptions (Netflix, Spotify, Google, iCloud).
                 * Incomes & Payouts: Salary/gaji, freelance fees, bonus, THR, reimbursement, cashbacks, gifts.
                 * Passive Income & Investments: SBN coupon payouts (ORI, SR, PBS, ST, FR), stock dividends, P2P lending interest, crypto staking yields, bank deposit interest. Kupon/dividends are ALWAYS type 'INCOME'. Use the net credited nominal (after tax if stated).
                 * Account Transfers: Transfer between user accounts (e.g. BCA to GoPay, Mandiri to Bibit, Bank to RDN) with type 'TRANSFER'.
               - Extract EVERY discrete transaction found in the message into the "proposals" array. If multiple transactions or bulk notifications are present, create a separate proposal item for each one.
               - Nominal Amount: Standard numeric value in major currency units (e.g. 150000 for Rp 150.000, 2500000 for Rp 2.500.000).
               - Title: Clean, descriptive merchant/transaction title (e.g. 'Kopi Kenangan', 'Superindo Kelapa Gading', 'PLN Token Listrik', 'Kupon SBN ORI026', 'Gaji Bulanan', 'Top Up GoPay').
               - Type: Exactly 'EXPENSE', 'INCOME', or 'TRANSFER'.
               - Date: Epoch timestamp in milliseconds. Extract transaction timestamp from receipt if available, otherwise use current timestamp (${System.currentTimeMillis()}).
               - Account: Match with the best Account ID from the user's registered accounts list. If no specific bank/account is mentioned or identifiable in the receipt/message, set accountName to 'Cash' (it will automatically default to the user's primary cash account).
               - Category: Match with the best Category ID matching the transaction type and purpose.
               - Notes: Concise additional context (e.g. reference number, tax deducted, items purchased).
               - Tags: Relevant lowercase tags (e.g. ["makan", "coffee"], ["belanja", "groceries"], ["investasi", "sbn", "kupon"], ["utilities"]).

            Always output a valid, well-formed JSON object matching this schema:
            {
              "reply": "Conversational explanation or friendly answer. If transactions were detected, summarize count and total nominal.",
              "proposals": [
                {
                  "title": "string",
                  "amount": 150000,
                  "type": "EXPENSE",
                  "date": ${System.currentTimeMillis()},
                  "accountId": 1,
                  "accountName": "Cash",
                  "categoryId": 2,
                  "categoryName": "string",
                  "notes": "string",
                  "tags": ["tag1", "tag2"]
                }
              ]
            }

            CRITICAL DIRECTIVE:
            Do NOT include chain-of-thought, thinking traces, or <think> tags.
            Output ONLY valid JSON starting immediately with '{' and ending with '}'.
        """.trimIndent()

        val bodyJson = buildJsonObject {
            put("model", JsonPrimitive(model))
            put(
                "messages",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", JsonPrimitive("system"))
                            put("content", JsonPrimitive(systemPrompt))
                        }
                    )
                    conversationHistory.takeLast(4).forEach { msg ->
                        add(
                            buildJsonObject {
                                put("role", JsonPrimitive(if (msg.sender == ChatSender.USER) "user" else "assistant"))
                                put("content", JsonPrimitive(msg.text))
                            }
                        )
                    }
                    add(
                        buildJsonObject {
                            put("role", JsonPrimitive("user"))
                            put("content", JsonPrimitive(userMessage))
                        }
                    )
                }
            )
            // Suppress reasoning for models that support the standard reasoning parameter
            put(
                "reasoning",
                buildJsonObject {
                    put("effort", JsonPrimitive("none"))
                }
            )
            if (!model.contains("free", ignoreCase = true)) {
                put(
                    "response_format",
                    buildJsonObject {
                        put("type", JsonPrimitive("json_object"))
                    }
                )
            }
            put("temperature", JsonPrimitive(0.2))
            put("max_tokens", JsonPrimitive(4096))
        }

        val content = executeChatCompletion(bodyJson)
        return AiJsonParser.parseAssistantResponse(content, accounts, categories)
    }

    private suspend fun executeChatCompletion(bodyJson: JsonObject): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("HTTP-Referer", appUrl)
            .header("X-Title", appName)
            .header("X-OpenRouter-Title", appName)
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        Log.d("OpenRouter", "Sending request to OpenRouter (model=$model)")

        val response = try {
            client.newCall(request).execute()
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("OpenRouter", "Socket timeout (90s) for model: $model", e)
            throw Exception("Waktu tunggu habis (timeout 90 detik). Model '$model' terlalu lama merespons. Jika menggunakan model gratis, antrean server sedang padat. Disarankan gunakan model cepat seperti 'google/gemini-2.0-flash-001' atau 'openai/gpt-4.1-mini'.")
        } catch (e: Exception) {
            Log.e("OpenRouter", "Network exception for model $model: ${e.message}", e)
            throw e
        }

        response.use { resp ->
            var raw = resp.body.string()
            Log.d("OpenRouter", "OpenRouter HTTP ${resp.code} response: $raw")

            // If 400 occurred and mentions reasoning, unrecognized parameters, or response_format, automatically retry without it
            if (!resp.isSuccessful && resp.code == 400) {
                val shouldStripReasoning = (raw.contains("reasoning", ignoreCase = true) || raw.contains("unrecognized", ignoreCase = true)) && bodyJson.containsKey("reasoning")
                val shouldStripResponseFormat = raw.contains("response_format", ignoreCase = true) && bodyJson.containsKey("response_format")

                if (shouldStripReasoning || shouldStripResponseFormat) {
                    Log.w("OpenRouter", "Model $model rejected parameters (strip reasoning=$shouldStripReasoning, response_format=$shouldStripResponseFormat); retrying...")
                    val fallbackBody = buildJsonObject {
                        bodyJson.forEach { (k, v) ->
                            if (k == "reasoning" && shouldStripReasoning) return@forEach
                            if (k == "response_format" && shouldStripResponseFormat) return@forEach
                            put(k, v)
                        }
                    }
                    val retryReq = request.newBuilder()
                        .post(fallbackBody.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(retryReq).execute().use { retryResp ->
                        raw = retryResp.body.string()
                        Log.d("OpenRouter", "OpenRouter retry HTTP ${retryResp.code}: $raw")
                        if (!retryResp.isSuccessful) {
                            val errMsg = parseOpenRouterError(retryResp.code, raw)
                            Log.e("OpenRouter", "Retry failed: $errMsg")
                            throw Exception(errMsg)
                        }
                    }
                } else {
                    val errMsg = parseOpenRouterError(resp.code, raw)
                    Log.e("OpenRouter", "Request failed: $errMsg")
                    throw Exception(errMsg)
                }
            } else if (!resp.isSuccessful) {
                val errMsg = parseOpenRouterError(resp.code, raw)
                Log.e("OpenRouter", "Request failed: $errMsg")
                throw Exception(errMsg)
            }

            val root = json.parseToJsonElement(raw).jsonObject
            val choice = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
            val messageObj = choice?.get("message")?.jsonObject
            val content = messageObj?.get("content")?.let { element -> runCatching { element.jsonPrimitive.content }.getOrNull() }
            val reasoning = messageObj?.get("reasoning")?.let { element -> runCatching { element.jsonPrimitive.content }.getOrNull() }

            val finalOutput = when {
                !content.isNullOrBlank() -> content
                !reasoning.isNullOrBlank() -> reasoning
                else -> {
                    Log.w("OpenRouter", "Empty content and reasoning in choices: $raw")
                    throw Exception("OpenRouter mengembalikan balasan kosong.")
                }
            }

            finalOutput
        }
    }

    private fun parseOpenRouterError(code: Int, responseBody: String): String {
        val fallback = "OpenRouter HTTP $code: ${responseBody.take(200)}"
        return runCatching {
            val root = json.parseToJsonElement(responseBody).jsonObject
            val errorObj = root["error"]?.jsonObject
            val msg = errorObj?.get("message")?.jsonPrimitive?.content
            when {
                code == 401 -> "API key OpenRouter tidak valid atau belum diisi. Periksa kembali API key Anda di Pengaturan AI."
                code == 402 -> "Saldo OpenRouter tidak mencukupi (insufficient credits). Silakan isi saldo atau gunakan model gratis."
                code == 404 -> "Model '$model' tidak ditemukan di OpenRouter. Silakan pilih model lain dari daftar rekomendasi."
                code == 429 -> "Rate limit OpenRouter tercapai atau server model sedang penuh. Tunggu beberapa saat dan coba lagi."
                !msg.isNullOrBlank() -> "OpenRouter ($code): $msg"
                else -> fallback
            }
        }.getOrDefault(fallback)
    }

}

