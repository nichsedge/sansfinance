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
import com.sans.finance.domain.model.FinancialContextSnapshot
import com.sans.finance.domain.model.StreamEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

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

    // SSE streaming client: readTimeout(0) prevents SocketTimeoutException during long token pauses
    private val streamingClient = rawClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
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

    override fun streamPortfolioAnalysis(input: PortfolioAnalysisInput): Flow<StreamEvent> = callbackFlow {
        val systemPrompt = """
            You are an elite sovereign wealth advisor and portfolio strategist for a personal finance app.
            Provide a thorough, highly insightful portfolio health diagnosis and tactical rebalancing advisory in Bahasa Indonesia (with clean financial terminology).
            Format your response in clean, beautiful Markdown with these sections:
            1. 🎯 **Ringkasan Kesehatan & Alokasi Portofolio** (Analisis diversifikasi, konsentrasi aset, cash drag)
            2. ⚠️ **Observasi Kritis & Risiko** (Identifikasi aset overweight/underweight terhadap target, risiko likuiditas/inflasi)
            3. 💡 **Rekomendasi Aksi Rebalancing Taktis** (Langkah konkret alokasi dana baru / cash injection tanpa memicu taxable sales, strategi optimasi imbal hasil)
            Keep it sharp, actionable, realistic, and tailored strictly to the portfolio numbers provided.
        """.trimIndent()

        val userPrompt = """
            Tanggal Snapshot: ${input.dateLabel}
            Mata Uang Basis: ${input.currency}
            Total Nilai Portofolio: ${input.totalValue}
            XIRR Kinerja: ${input.xirr ?: "N/A"}
            Alokasi Kelas Aset: ${input.assetAllocation.joinToString { "${it.first}=${String.format(java.util.Locale.US, "%.1f", it.second)}%" }}
            Status Kesehatan vs Target: ${input.healthStatus.joinToString()}
            Progres Target Goals: ${input.goals.joinToString()}
            Catatan Tambahan: ${input.notes}
        """.trimIndent()

        val bodyJson = buildJsonObject {
            put("model", JsonPrimitive(model))
            put("stream", JsonPrimitive(true))
            put(
                "messages",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", JsonPrimitive("system"))
                            put("content", JsonPrimitive(systemPrompt))
                        }
                    )
                    add(
                        buildJsonObject {
                            put("role", JsonPrimitive("user"))
                            put("content", JsonPrimitive(userPrompt))
                        }
                    )
                }
            )
            put("temperature", JsonPrimitive(0.3))
            put("max_tokens", JsonPrimitive(2048))
        }

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("HTTP-Referer", appUrl)
            .header("X-Title", appName)
            .header("X-OpenRouter-Title", appName)
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        var activeCall = streamingClient.newCall(request)
        try {
            var response = activeCall.execute()

            if (!response.isSuccessful && response.code == 400) {
                val errorBody = response.body.string()
                if (errorBody.contains("reasoning", ignoreCase = true) || errorBody.contains("unrecognized", ignoreCase = true)) {
                    val fallbackBody = buildJsonObject {
                        bodyJson.forEach { (k, v) ->
                            if (k != "reasoning") put(k, v)
                        }
                    }
                    val retryReq = request.newBuilder()
                        .post(fallbackBody.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    activeCall = streamingClient.newCall(retryReq)
                    response = activeCall.execute()
                } else {
                    trySend(StreamEvent.Error(parseOpenRouterError(response.code, errorBody)))
                    close()
                    return@callbackFlow
                }
            }

            if (!response.isSuccessful) {
                val errorBody = response.body.string()
                trySend(StreamEvent.Error(parseOpenRouterError(response.code, errorBody)))
                close()
                return@callbackFlow
            }

            val fullText = StringBuilder()
            val source = response.body.source()

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: continue
                if (line.isBlank() || !line.startsWith("data: ")) continue

                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break

                try {
                    val chunk = json.parseToJsonElement(data).jsonObject
                    val deltaObj = chunk["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("delta")?.jsonObject
                    val contentDelta = deltaObj?.get("content")?.jsonPrimitive?.contentOrNull

                    if (!contentDelta.isNullOrEmpty()) {
                        fullText.append(contentDelta)
                        trySend(StreamEvent.TextDelta(contentDelta))
                    }
                } catch (_: Exception) {}
            }

            trySend(StreamEvent.Done(fullText.toString()))
            close()
        } catch (e: Exception) {
            trySend(StreamEvent.Error(e.localizedMessage ?: e.message ?: "Unknown streaming error"))
            close()
        }

        awaitClose { activeCall.cancel() }
    }.flowOn(Dispatchers.IO)

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
        conversationHistory: List<ChatMessage>,
        financialContext: FinancialContextSnapshot?
    ): AiAssistantResponse {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        val accountsFormatted = accounts.joinToString(separator = "\n") {
            "- ID: ${it.id} | Name: '${it.name}' | Type: ${it.type} | Currency: ${it.currency}"
        }
        val categoriesFormatted = categories.joinToString(separator = "\n") {
            "- ID: ${it.id} | Name: '${it.name}' | Type: ${it.type}"
        }

        val financialContextFormatted = buildFinancialContextString(financialContext, currency)

        val systemPrompt = """
            You are Sans Finance Copilot, an elite personal finance assistant and universal transaction harness for an Indonesia-based personal wealth and cashflow management app.
            Current Date: $todayStr
            Base Currency: $currency

            User's Registered Accounts:
            $accountsFormatted

            User's Registered Categories:
            $categoriesFormatted

            ${financialContextFormatted.ifBlank { "" }}

            Capabilities & Modes:
            1. CONVERSATIONAL & FINANCIAL ADVICE MODE:
               - When the user asks financial questions, inquires about their net worth, portfolio, investments, current expenses, income, savings, cash balances, budgeting strategies (50/30/20, zero-based), emergency fund sizing, cash runway, FIRE targets, or debt payoff horizons:
               - Use the real figures provided in the Financial Snapshot above to answer accurately and concretely.
               - Net Worth & Asset Invariant: When asked about net worth or total wealth, ALWAYS cite the exact Total Net Worth from [Balance Sheet & Net Worth Overview]. Net Worth = Total Assets (Liquid Cash + Investment Portfolio) - Liabilities. NEVER report net worth based solely on cash/bank accounts when an investment portfolio exists. Always clearly break down the composition (Net Worth, Liquid Cash & Bank, Investment Portfolio with allocation and top holdings, and any Liabilities/Debts).
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

    override fun streamChat(
        userMessage: String,
        accounts: List<AccountSummary>,
        categories: List<CategorySummary>,
        currency: String,
        conversationHistory: List<ChatMessage>,
        financialContext: FinancialContextSnapshot?
    ): Flow<StreamEvent> = callbackFlow {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        val accountsFormatted = accounts.joinToString(separator = "\n") {
            "- ID: ${it.id} | Name: '${it.name}' | Type: ${it.type} | Currency: ${it.currency}"
        }
        val categoriesFormatted = categories.joinToString(separator = "\n") {
            "- ID: ${it.id} | Name: '${it.name}' | Type: ${it.type}"
        }

        val financialContextFormatted = buildFinancialContextString(financialContext, currency)

        // Static system prompt (no dynamic timestamps) for prompt caching
        val systemPrompt = buildChatSystemPrompt(todayStr, currency, accountsFormatted, categoriesFormatted)

        val bodyJson = buildJsonObject {
            put("model", JsonPrimitive(model))
            put("stream", JsonPrimitive(true))
            put(
                "messages",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", JsonPrimitive("system"))
                            put("content", JsonPrimitive(systemPrompt))
                        }
                    )
                    // Dynamic context as a separate user turn for prompt caching
                    val dynamicContextContent = buildString {
                        append("Context: current_timestamp=${System.currentTimeMillis()}, date=$todayStr")
                        if (financialContextFormatted.isNotBlank()) {
                            append("\n\n").append(financialContextFormatted)
                        }
                    }
                    add(
                        buildJsonObject {
                            put("role", JsonPrimitive("user"))
                            put("content", JsonPrimitive(dynamicContextContent))
                        }
                    )
                    add(
                        buildJsonObject {
                            put("role", JsonPrimitive("assistant"))
                            put("content", JsonPrimitive("Understood. I have your up-to-date financial context and accounts."))
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
            // Do not force reasoning effort = none; models like DeepSeek R1 / openrouter/free require reasoning
            put("temperature", JsonPrimitive(0.2))
            put("max_tokens", JsonPrimitive(4096))
        }

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("HTTP-Referer", appUrl)
            .header("X-Title", appName)
            .header("X-OpenRouter-Title", appName)
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        Log.d("OpenRouter", "Starting SSE stream (model=$model)")

        var activeCall = streamingClient.newCall(request)
        try {
            var response = activeCall.execute()

            // If 400 occurs due to unrecognized parameters, retry cleanly
            if (!response.isSuccessful && response.code == 400) {
                val errorBody = response.body.string()
                Log.w("OpenRouter", "Stream HTTP 400: $errorBody")
                if (errorBody.contains("reasoning", ignoreCase = true) || errorBody.contains("unrecognized", ignoreCase = true)) {
                    val fallbackBody = buildJsonObject {
                        bodyJson.forEach { (k, v) ->
                            if (k != "reasoning") put(k, v)
                        }
                    }
                    val retryReq = request.newBuilder()
                        .post(fallbackBody.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    activeCall = streamingClient.newCall(retryReq)
                    response = activeCall.execute()
                } else {
                    trySend(StreamEvent.Error(parseOpenRouterError(response.code, errorBody)))
                    close()
                    return@callbackFlow
                }
            }

            if (!response.isSuccessful) {
                val errorBody = response.body.string()
                Log.e("OpenRouter", "Stream error HTTP ${response.code}: $errorBody")
                trySend(StreamEvent.Error(parseOpenRouterError(response.code, errorBody)))
                close()
                return@callbackFlow
            }

            val fullText = StringBuilder()
            val source = response.body.source()

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: continue
                if (line.isBlank() || !line.startsWith("data: ")) continue

                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break

                try {
                    val chunk = json.parseToJsonElement(data).jsonObject
                    val deltaObj = chunk["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("delta")?.jsonObject
                    val contentDelta = deltaObj?.get("content")?.jsonPrimitive?.contentOrNull
                    val reasoningDelta = deltaObj?.get("reasoning")?.jsonPrimitive?.contentOrNull

                    val textToEmit = when {
                        !contentDelta.isNullOrEmpty() -> contentDelta
                        // If model only emits reasoning tokens (thinking phase), we can skip emitting or let it stream
                        else -> null
                    }

                    if (!textToEmit.isNullOrEmpty()) {
                        fullText.append(textToEmit)
                        trySend(StreamEvent.TextDelta(textToEmit))
                    }
                } catch (e: Exception) {
                    Log.w("OpenRouter", "Failed to parse SSE chunk: $data", e)
                }
            }

            trySend(StreamEvent.Done(fullText.toString()))
            close()
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("OpenRouter", "Stream socket timeout for model: $model", e)
            trySend(StreamEvent.Error(
                "Waktu tunggu habis (timeout). Model '$model' terlalu lama merespons. " +
                "Disarankan gunakan model cepat seperti 'google/gemini-2.0-flash-001' atau 'openai/gpt-4.1-mini'."
            ))
            close()
        } catch (e: Exception) {
            Log.e("OpenRouter", "Stream exception: ${e.message}", e)
            trySend(StreamEvent.Error(e.localizedMessage ?: e.message ?: "Unknown streaming error"))
            close()
        }

        awaitClose { activeCall.cancel() }
    }.flowOn(Dispatchers.IO)

    private fun buildChatSystemPrompt(
        todayStr: String,
        currency: String,
        accountsFormatted: String,
        categoriesFormatted: String
    ): String = """
        You are Sans Finance Copilot, an elite personal finance assistant and universal transaction harness for an Indonesia-based personal wealth and cashflow management app.
        Current Date: $todayStr
        Base Currency: $currency

        User's Registered Accounts:
        $accountsFormatted

        User's Registered Categories:
        $categoriesFormatted

        Capabilities & Modes:
        1. CONVERSATIONAL & FINANCIAL COPILOT MODE:
           - You are SansAI, an elite, data-driven personal wealth and cashflow intelligence copilot for Sans Finance.
           - Grounding Invariant: Thoroughly inspect the Real-time Financial Context provided in the context turn. Never say historical data is unavailable when previous month (M-1) or 3-month baseline is present. Always cite exact numbers, percentages, and deltas between months.
           - Net Worth & Balance Sheet Invariant:
             * When the user asks about their net worth ("berapa net worth saya?", "total kekayaan saya"), ALWAYS cite the exact Total Net Worth from [Balance Sheet & Net Worth Overview].
             * Never fetch or calculate net worth from cash/bank accounts alone! Net Worth = Total Assets (Liquid Cash + Investment Portfolio) - Liabilities.
             * Always break down the composition clearly: Net Worth, Liquid Cash & Bank, Investment Portfolio (including asset allocation and top holdings), and any Liabilities/Debts.
           - Deep Expense & Variance Analysis:
             * When the user asks why they spent so much ("kok boros?", "kenapa naik?"), do NOT give generic platitudes (e.g. "cabut colokan listrik", "kurangi AC", "gunakan metode 50/30/20").
             * Compare Current Month vs Previous Month and 3-Month Rolling Average directly.
             * Check the "Top Big-Ticket Discrete Expenses" to pinpoint the exact single transactions causing spikes (e.g. sewa kos/rent, tuition, electronics, flight).
             * Clearly distinguish Fixed Commitments (Kos/sewa, insurance, recurring bills) from Discretionary Leaks (food delivery, coffee, impulsive shopping, entertainment).
             * Acknowledge savings health: if the user's savings rate is high (e.g. >50% or 70%+), reassure them that their net cashflow remains strongly positive, but clearly highlight where the delta came from.
           - Tone & Style: Direct, sharp, analytical, empathetic, and concise Indonesian (or English if prompted in English). Use bolding and concise bullet points.
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
           - Date: Epoch timestamp in milliseconds. Extract transaction timestamp from receipt if available, otherwise use the current timestamp provided in the context turn above.
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
              "date": 0,
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

    private fun buildFinancialContextString(
        snapshot: FinancialContextSnapshot?,
        baseCurrency: String
    ): String {
        if (snapshot == null) return ""
        val sb = StringBuilder()
        sb.append("=== Real-time Financial Context & Metrics ===\n")

        // 1. Balance Sheet & Net Worth Overview
        sb.append("[Balance Sheet & Net Worth Overview]\n")
        sb.append("- Total Net Worth: ${com.sans.finance.core.util.CurrencyFormatter.formatAmount(snapshot.netWorth, baseCurrency)}\n")
        sb.append("- Total Assets: ${com.sans.finance.core.util.CurrencyFormatter.formatAmount(snapshot.totalAssets, baseCurrency)}\n")
        sb.append("  * Liquid Cash & Bank: ${com.sans.finance.core.util.CurrencyFormatter.formatAmount(snapshot.liquidCashAssets, baseCurrency)}\n")
        sb.append("  * Investment Portfolio: ${com.sans.finance.core.util.CurrencyFormatter.formatAmount(snapshot.portfolioInvestmentValue, baseCurrency)}\n")
        sb.append("- Total Liabilities / Debts: ${com.sans.finance.core.util.CurrencyFormatter.formatAmount(snapshot.totalLiabilities, baseCurrency)}\n")
        if (snapshot.runwayMonths > 0.0) {
            sb.append("- Emergency Runway: ${String.format(java.util.Locale.US, "%.1f", snapshot.runwayMonths)} months of expenses\n")
        }
        if (snapshot.monthlyPassiveIncome > 0L || snapshot.annualPassiveIncome > 0L) {
            sb.append("- Estimated Passive Income (Yield/Dividends/Coupons): ${com.sans.finance.core.util.CurrencyFormatter.formatAmount(snapshot.monthlyPassiveIncome, baseCurrency)}/month (${com.sans.finance.core.util.CurrencyFormatter.formatAmount(snapshot.annualPassiveIncome, baseCurrency)}/year)\n")
        }

        // 2. Investment Portfolio Breakdown
        if (snapshot.portfolioInvestmentValue > 0L || snapshot.portfolioAssetClassBreakdown.isNotEmpty() || snapshot.topPortfolioHoldings.isNotEmpty()) {
            sb.append("\n[Investment Portfolio Allocation & Holdings]\n")
            if (snapshot.portfolioAssetClassBreakdown.isNotEmpty()) {
                sb.append("- Asset Allocation:\n")
                snapshot.portfolioAssetClassBreakdown.forEach { (assetClass, amount) ->
                    val pct = if (snapshot.portfolioInvestmentValue > 0) {
                        (amount.toDouble() / snapshot.portfolioInvestmentValue.toDouble()) * 100.0
                    } else 0.0
                    sb.append("  * $assetClass: ${com.sans.finance.core.util.CurrencyFormatter.formatAmount(amount, baseCurrency)} (${String.format(java.util.Locale.US, "%.1f", pct)}%)\n")
                }
            }
            if (snapshot.topPortfolioHoldings.isNotEmpty()) {
                sb.append("- Top Holdings:\n")
                snapshot.topPortfolioHoldings.forEach { (holdingName, amount) ->
                    sb.append("  * $holdingName: ${com.sans.finance.core.util.CurrencyFormatter.formatAmount(amount, baseCurrency)}\n")
                }
            }
        }

        // 3. Current Month Cashflow
        sb.append("\n[Current Month Cashflow: ${snapshot.monthLabel.ifBlank { "Current Month" }}]\n")
        sb.append("- Total Income: ${com.sans.finance.core.util.CurrencyFormatter.formatAmount(snapshot.totalIncomeThisMonth, baseCurrency)}\n")
        sb.append("- Total Expense: ${com.sans.finance.core.util.CurrencyFormatter.formatAmount(snapshot.totalExpenseThisMonth, baseCurrency)}\n")
        sb.append("- Net Cashflow: ${com.sans.finance.core.util.CurrencyFormatter.formatAmount(snapshot.netCashflowThisMonth, baseCurrency)}\n")
        sb.append("- Savings Rate: ${String.format(java.util.Locale.US, "%.1f", snapshot.savingsRatePercentage * 100f)}%\n")

        if (snapshot.prevMonthLabel.isNotBlank()) {
            sb.append("\n[Previous Month: ${snapshot.prevMonthLabel}]\n")
            sb.append("- Total Income: ${com.sans.finance.core.util.CurrencyFormatter.formatAmount(snapshot.prevMonthIncome, baseCurrency)}\n")
            sb.append("- Total Expense: ${com.sans.finance.core.util.CurrencyFormatter.formatAmount(snapshot.prevMonthExpense, baseCurrency)}\n")
            sb.append("- Savings Rate: ${String.format(java.util.Locale.US, "%.1f", snapshot.prevMonthSavingsRate * 100f)}%\n")
            if (snapshot.prevMonthTopCategories.isNotEmpty()) {
                val catStr = snapshot.prevMonthTopCategories.joinToString { (name, amt) ->
                    "$name (${com.sans.finance.core.util.CurrencyFormatter.formatAmount(amt, baseCurrency)})"
                }
                sb.append("- Top Categories: $catStr\n")
            }
        }

        if (snapshot.threeMonthAverageExpense > 0L) {
            sb.append("\n[3-Month Rolling Baseline Average Expense: ${com.sans.finance.core.util.CurrencyFormatter.formatAmount(snapshot.threeMonthAverageExpense, baseCurrency)}/month]\n")
        }

        if (snapshot.topBigTicketExpenses.isNotEmpty()) {
            sb.append("\n[Current Month Top Big-Ticket Expenses (Spike/Variance Drivers)]:\n")
            snapshot.topBigTicketExpenses.forEach { tx ->
                sb.append("  * $tx\n")
            }
        }

        if (snapshot.topExpenseCategories.isNotEmpty()) {
            sb.append("\n[Current Month Top Spending Categories]:\n")
            snapshot.topExpenseCategories.forEach { (categoryName, amount) ->
                sb.append("  * $categoryName: ${com.sans.finance.core.util.CurrencyFormatter.formatAmount(amount, baseCurrency)}\n")
            }
        }

        if (snapshot.accountBalances.isNotEmpty()) {
            sb.append("\n[Liquid Bank & Cash Account Balances]:\n")
            snapshot.accountBalances.forEach { (name, balance) ->
                sb.append("  * $name: ${com.sans.finance.core.util.CurrencyFormatter.formatAmount(balance, baseCurrency)}\n")
            }
        }

        if (snapshot.liabilityAccountBalances.isNotEmpty()) {
            sb.append("\n[Liabilities & Credit Accounts]:\n")
            snapshot.liabilityAccountBalances.forEach { (name, balance) ->
                sb.append("  * $name: ${com.sans.finance.core.util.CurrencyFormatter.formatAmount(balance, baseCurrency)}\n")
            }
        }

        if (snapshot.recentTransactions.isNotEmpty()) {
            sb.append("\n[Latest Transactions]:\n")
            snapshot.recentTransactions.take(6).forEach { tx ->
                sb.append("  * $tx\n")
            }
        }
        sb.append("=============================================")
        return sb.toString().trim()
    }
}

