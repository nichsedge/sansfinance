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
import com.sans.finance.domain.model.AiToolCall
import com.sans.finance.domain.model.FinancialContextSnapshot
import kotlinx.serialization.json.intOrNull
import java.util.UUID
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

    // Dedicated client with extended timeouts for AI reasoning/routers (180s for free queues)
    private val client = rawClient.newBuilder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // SSE streaming client: HTTP/1.1 and pingInterval prevent connection resets from intermediate edge proxies during long vision inference
    private val streamingClient = rawClient.newBuilder()
        .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
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
        financialContext: FinancialContextSnapshot?,
        base64Image: String?
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
        val systemPrompt = buildChatSystemPrompt(todayStr, currency, accountsFormatted, categoriesFormatted)

        val messagesArray = buildJsonArray {
            add(
                buildJsonObject {
                    put("role", JsonPrimitive("system"))
                    put("content", JsonPrimitive(systemPrompt))
                }
            )
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
            add(buildUserMessageObject(userMessage, base64Image))
        }

        val bodyJson = buildJsonObject {
            put("model", JsonPrimitive(model))
            put("messages", messagesArray)
            put("tools", buildToolsJson())
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
        financialContext: FinancialContextSnapshot?,
        base64Image: String?,
        toolExecutor: (suspend (name: String, argumentsJson: String) -> String)?
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
        val systemPrompt = buildChatSystemPrompt(todayStr, currency, accountsFormatted, categoriesFormatted)

        val messagesList = mutableListOf<JsonObject>()
        messagesList.add(
            buildJsonObject {
                put("role", JsonPrimitive("system"))
                put("content", JsonPrimitive(systemPrompt))
            }
        )
        val dynamicContextContent = buildString {
            append("Context: current_timestamp=${System.currentTimeMillis()}, date=$todayStr")
            if (financialContextFormatted.isNotBlank()) {
                append("\n\n").append(financialContextFormatted)
            }
        }
        messagesList.add(
            buildJsonObject {
                put("role", JsonPrimitive("user"))
                put("content", JsonPrimitive(dynamicContextContent))
            }
        )
        messagesList.add(
            buildJsonObject {
                put("role", JsonPrimitive("assistant"))
                put("content", JsonPrimitive("Understood. I have your up-to-date financial context and accounts."))
            }
        )
        conversationHistory.takeLast(4).forEach { msg ->
            messagesList.add(
                buildJsonObject {
                    put("role", JsonPrimitive(if (msg.sender == ChatSender.USER) "user" else "assistant"))
                    put("content", JsonPrimitive(msg.text))
                }
            )
        }
        messagesList.add(buildUserMessageObject(userMessage, base64Image))

        var activeCall: okhttp3.Call? = null
        val accumulatedProse = StringBuilder()
        var turnCount = 0
        val maxTurns = 3

        while (turnCount < maxTurns) {
            turnCount++
            val bodyJson = buildJsonObject {
                put("model", JsonPrimitive(model))
                put("stream", JsonPrimitive(true))
                put("messages", buildJsonArray { messagesList.forEach { add(it) } })
                put("tools", buildToolsJson())
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

            activeCall = streamingClient.newCall(request)
            var response = try {
                activeCall.execute()
            } catch (e: Exception) {
                trySend(StreamEvent.Error(e.localizedMessage ?: e.message ?: "Network error"))
                close()
                return@callbackFlow
            }

            // Retry on HTTP 400 if model rejects tools or reasoning
            if (!response.isSuccessful && response.code == 400) {
                val errorBody = response.body.string()
                Log.w("OpenRouter", "Stream HTTP 400: $errorBody")
                val stripTools = errorBody.contains("tools", ignoreCase = true)
                val stripReasoning = errorBody.contains("reasoning", ignoreCase = true) || errorBody.contains("unrecognized", ignoreCase = true)

                if (stripTools || stripReasoning) {
                    val fallbackBody = buildJsonObject {
                        bodyJson.forEach { (k, v) ->
                            if (stripTools && k == "tools") return@forEach
                            if (stripReasoning && k == "reasoning") return@forEach
                            put(k, v)
                        }
                    }
                    val retryReq = request.newBuilder()
                        .post(fallbackBody.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    activeCall = streamingClient.newCall(retryReq)
                    response = try {
                        activeCall.execute()
                    } catch (e: Exception) {
                        trySend(StreamEvent.Error(e.localizedMessage ?: e.message ?: "Retry network error"))
                        close()
                        return@callbackFlow
                    }
                }
            }

            if (!response.isSuccessful) {
                val errorBody = response.body.string()
                trySend(StreamEvent.Error(parseOpenRouterError(response.code, errorBody)))
                close()
                return@callbackFlow
            }

            val turnText = StringBuilder()
            class ToolAcc(var id: String = "", var name: String = "", val args: StringBuilder = StringBuilder())
            val toolAccMap = mutableMapOf<Int, ToolAcc>()

            try {
                val source = response.body.source()
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: continue
                    if (line.isBlank() || line.startsWith(":")) continue
                    if (!line.startsWith("data: ")) continue

                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") break

                    try {
                        val chunk = json.parseToJsonElement(data).jsonObject
                        val deltaObj = chunk["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                            ?.get("delta")?.jsonObject ?: continue

                        // 1. Thinking / reasoning tokens
                        val reasoning = deltaObj["reasoning"]?.jsonPrimitive?.contentOrNull
                        if (!reasoning.isNullOrEmpty()) {
                            trySend(StreamEvent.ThinkingDelta(reasoning))
                        }

                        // 2. Prose content tokens
                        val content = deltaObj["content"]?.jsonPrimitive?.contentOrNull
                        if (!content.isNullOrEmpty()) {
                            turnText.append(content)
                            accumulatedProse.append(content)
                            trySend(StreamEvent.TextDelta(content))
                        }

                        // 3. Tool call deltas
                        val toolCallsArr = deltaObj["tool_calls"]?.jsonArray
                        if (toolCallsArr != null) {
                            for (tcElem in toolCallsArr) {
                                val tc = tcElem.jsonObject
                                val index = tc["index"]?.jsonPrimitive?.intOrNull ?: 0
                                val acc = toolAccMap.getOrPut(index) { ToolAcc() }
                                tc["id"]?.jsonPrimitive?.contentOrNull?.let { if (it.isNotBlank()) acc.id = it }
                                val fn = tc["function"]?.jsonObject
                                if (fn != null) {
                                    fn["name"]?.jsonPrimitive?.contentOrNull?.let { if (it.isNotBlank()) acc.name = it }
                                    fn["arguments"]?.jsonPrimitive?.contentOrNull?.let { acc.args.append(it) }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("OpenRouter", "Error parsing SSE chunk: $data", e)
                    }
                }
            } catch (e: Exception) {
                Log.w("OpenRouter", "SSE stream reading interrupted: ${e.message}", e)
                if (accumulatedProse.isNotBlank()) {
                    val fallbackProposals = AiJsonParser.parseAssistantResponse(accumulatedProse.toString(), accounts, categories)
                    trySend(StreamEvent.Done(fullText = accumulatedProse.toString(), toolCalls = emptyList(), proposals = fallbackProposals.proposals))
                    close()
                    return@callbackFlow
                } else {
                    trySend(StreamEvent.Error("Koneksi ke AI terputus (${e.localizedMessage ?: e.message}). Antrean model gratis mungkin sedang padat. Silakan coba kirim ulang."))
                    close()
                    return@callbackFlow
                }
            }

            val completedToolCalls = toolAccMap.values.map {
                AiToolCall(
                    id = it.id.ifBlank { UUID.randomUUID().toString() },
                    name = it.name,
                    argumentsJson = it.args.toString()
                )
            }

            // Case A: Model called propose_transactions (terminal action tool)
            val proposeCall = completedToolCalls.firstOrNull { it.name == "propose_transactions" }
            if (proposeCall != null) {
                val proposals = AiJsonParser.parseProposalArguments(proposeCall.argumentsJson, accounts, categories)
                trySend(StreamEvent.Done(fullText = accumulatedProse.toString(), toolCalls = completedToolCalls, proposals = proposals))
                close()
                return@callbackFlow
            }

            // Case B: Model called query tools
            val queryCalls = completedToolCalls.filter { it.name != "propose_transactions" && it.name.isNotBlank() }
            if (queryCalls.isNotEmpty() && toolExecutor != null) {
                // Add assistant message with tool calls to history
                messagesList.add(
                    buildJsonObject {
                        put("role", JsonPrimitive("assistant"))
                        if (turnText.isNotEmpty()) put("content", JsonPrimitive(turnText.toString()))
                        put("tool_calls", buildJsonArray {
                            for (call in completedToolCalls) {
                                add(buildJsonObject {
                                    put("id", JsonPrimitive(call.id))
                                    put("type", JsonPrimitive("function"))
                                    put("function", buildJsonObject {
                                        put("name", JsonPrimitive(call.name))
                                        put("arguments", JsonPrimitive(call.argumentsJson))
                                    })
                                })
                            }
                        })
                    }
                )

                for (q in queryCalls) {
                    val statusMessage = when (q.name) {
                        "get_safe_to_spend" -> "🔍 Mengecek Safe-to-Spend hari ini..."
                        "get_installment_horizon" -> "📊 Memeriksa horizon cicilan & komitmen..."
                        "query_expenses" -> "🔎 Mencari riwayat transaksi di database..."
                        "get_passive_income_summary" -> "📈 Menghitung proyeksi imbal hasil & kupon SBN..."
                        else -> "🔍 Mengambil data keuangan..."
                    }
                    trySend(StreamEvent.StatusUpdate(statusMessage))
                    val toolResultJson = toolExecutor(q.name, q.argumentsJson)
                    messagesList.add(
                        buildJsonObject {
                            put("role", JsonPrimitive("tool"))
                            put("tool_call_id", JsonPrimitive(q.id))
                            put("name", JsonPrimitive(q.name))
                            put("content", JsonPrimitive(toolResultJson))
                        }
                    )
                }
                // Loop to let the model generate the final explanation based on tool results
                continue
            }

            // Case C: No tool calls. Check fallback if raw JSON in text
            val fallbackProposals = AiJsonParser.parseAssistantResponse(accumulatedProse.toString(), accounts, categories)
            if (fallbackProposals.proposals.isNotEmpty()) {
                trySend(StreamEvent.Done(fullText = fallbackProposals.reply, proposals = fallbackProposals.proposals))
            } else {
                trySend(StreamEvent.Done(fullText = accumulatedProse.toString()))
            }
            close()
            return@callbackFlow
        }

        trySend(StreamEvent.Done(fullText = accumulatedProse.toString()))
        close()
        awaitClose { activeCall?.cancel() }
    }.flowOn(Dispatchers.IO)

    private fun buildUserMessageObject(userMessage: String, base64Image: String?): JsonObject {
        return if (!base64Image.isNullOrBlank()) {
            buildJsonObject {
                put("role", JsonPrimitive("user"))
                put("content", buildJsonArray {
                    add(buildJsonObject {
                        put("type", JsonPrimitive("text"))
                        put("text", JsonPrimitive(userMessage.ifBlank { "Analisis struk atau transaksi ini:" }))
                    })
                    add(buildJsonObject {
                        put("type", JsonPrimitive("image_url"))
                        put("image_url", buildJsonObject {
                            put("url", JsonPrimitive("data:image/jpeg;base64,$base64Image"))
                        })
                    })
                })
            }
        } else {
            buildJsonObject {
                put("role", JsonPrimitive("user"))
                put("content", JsonPrimitive(userMessage))
            }
        }
    }

    private fun buildToolsJson(): kotlinx.serialization.json.JsonArray = buildJsonArray {
        // 1. propose_transactions
        add(buildJsonObject {
            put("type", JsonPrimitive("function"))
            put("function", buildJsonObject {
                put("name", JsonPrimitive("propose_transactions"))
                put("description", JsonPrimitive("Propose one or more structured transactions (expense, income, transfer) detected in receipts, QRIS, transfers, bills, or messages for user confirmation before persisting."))
                put("parameters", buildJsonObject {
                    put("type", JsonPrimitive("object"))
                    put("properties", buildJsonObject {
                        put("transactions", buildJsonObject {
                            put("type", JsonPrimitive("array"))
                            put("items", buildJsonObject {
                                put("type", JsonPrimitive("object"))
                                put("properties", buildJsonObject {
                                    put("title", buildJsonObject {
                                        put("type", JsonPrimitive("string"))
                                        put("description", JsonPrimitive("Merchant or transaction title"))
                                    })
                                    put("amount", buildJsonObject {
                                        put("type", JsonPrimitive("number"))
                                        put("description", JsonPrimitive("Nominal amount in standard currency units (e.g. 50000 for Rp 50.000)"))
                                    })
                                    put("type", buildJsonObject {
                                        put("type", JsonPrimitive("string"))
                                        put("enum", buildJsonArray {
                                            add(JsonPrimitive("EXPENSE"))
                                            add(JsonPrimitive("INCOME"))
                                            add(JsonPrimitive("TRANSFER"))
                                        })
                                    })
                                    put("date", buildJsonObject {
                                        put("type", JsonPrimitive("integer"))
                                        put("description", JsonPrimitive("Epoch timestamp in milliseconds"))
                                    })
                                    put("accountName", buildJsonObject {
                                        put("type", JsonPrimitive("string"))
                                        put("description", JsonPrimitive("Matched account name or 'Cash'"))
                                    })
                                    put("categoryName", buildJsonObject {
                                        put("type", JsonPrimitive("string"))
                                        put("description", JsonPrimitive("Matched category name"))
                                    })
                                    put("notes", buildJsonObject {
                                        put("type", JsonPrimitive("string"))
                                    })
                                    put("tags", buildJsonObject {
                                        put("type", JsonPrimitive("array"))
                                        put("items", buildJsonObject { put("type", JsonPrimitive("string")) })
                                    })
                                })
                                put("required", buildJsonArray {
                                    add(JsonPrimitive("title"))
                                    add(JsonPrimitive("amount"))
                                    add(JsonPrimitive("type"))
                                })
                            })
                        })
                    })
                    put("required", buildJsonArray { add(JsonPrimitive("transactions")) })
                })
            })
        })

        // 2. get_safe_to_spend
        add(buildJsonObject {
            put("type", JsonPrimitive("function"))
            put("function", buildJsonObject {
                put("name", JsonPrimitive("get_safe_to_spend"))
                put("description", JsonPrimitive("Get real-time safe-to-spend allowance, daily discretionary budget pacing, and cycle days remaining."))
                put("parameters", buildJsonObject {
                    put("type", JsonPrimitive("object"))
                    put("properties", buildJsonObject {})
                })
            })
        })

        // 3. get_installment_horizon
        add(buildJsonObject {
            put("type", JsonPrimitive("function"))
            put("function", buildJsonObject {
                put("name", JsonPrimitive("get_installment_horizon"))
                put("description", JsonPrimitive("Get active installments, debt commitments, monthly obligations, and debt liberation schedule."))
                put("parameters", buildJsonObject {
                    put("type", JsonPrimitive("object"))
                    put("properties", buildJsonObject {})
                })
            })
        })

        // 4. query_expenses
        add(buildJsonObject {
            put("type", JsonPrimitive("function"))
            put("function", buildJsonObject {
                put("name", JsonPrimitive("query_expenses"))
                put("description", JsonPrimitive("Query user's historical expense records from SQLite database by merchant keyword, category, or time range."))
                put("parameters", buildJsonObject {
                    put("type", JsonPrimitive("object"))
                    put("properties", buildJsonObject {
                        put("query", buildJsonObject {
                            put("type", JsonPrimitive("string"))
                            put("description", JsonPrimitive("Merchant or title search keyword"))
                        })
                        put("categoryName", buildJsonObject {
                            put("type", JsonPrimitive("string"))
                            put("description", JsonPrimitive("Category filter"))
                        })
                        put("daysAgo", buildJsonObject {
                            put("type", JsonPrimitive("integer"))
                            put("description", JsonPrimitive("Days back from today (default 30)"))
                        })
                        put("limit", buildJsonObject {
                            put("type", JsonPrimitive("integer"))
                            put("description", JsonPrimitive("Max results (default 10)"))
                        })
                    })
                })
            })
        })

        // 5. get_passive_income_summary
        add(buildJsonObject {
            put("type", JsonPrimitive("function"))
            put("function", buildJsonObject {
                put("name", JsonPrimitive("get_passive_income_summary"))
                put("description", JsonPrimitive("Get passive income breakdown from government bonds (SBN ORI/SR/PBS/ST/FR), dividends, and expense coverage."))
                put("parameters", buildJsonObject {
                    put("type", JsonPrimitive("object"))
                    put("properties", buildJsonObject {})
                })
            })
        })
    }

    private fun buildChatSystemPrompt(
        todayStr: String,
        currency: String,
        accountsFormatted: String,
        categoriesFormatted: String
    ): String = """
        You are SansAI, an elite personal wealth strategist and universal transaction copilot for Sans Finance (Indonesia).
        Current Date: $todayStr
        Base Currency: $currency

        User's Registered Accounts:
        $accountsFormatted

        User's Registered Categories:
        $categoriesFormatted

        Core Directives:
        1. NATURAL CONVERSATION & PROSE:
           - Always format your conversational responses in clean, beautiful Markdown in Bahasa Indonesia (or English if the user writes in English).
           - Do NOT output raw JSON in your text content! Prose must be readable, structured, and insightful.

        2. DATA GROUNDING & REAL-TIME TOOLS:
           - Ground your answers in the financial snapshot and query tools.
           - When asked about net worth, ALWAYS state the Total Net Worth = Total Assets (Cash + Portfolio) - Liabilities from the balance sheet.
           - When asked about safe-to-spend or daily allowance: call `get_safe_to_spend`.
           - When asked about active installments or debt payoff timeline: call `get_installment_horizon`.
           - When asked about specific past expenses or merchant spending: call `query_expenses`.
           - When asked about passive income, SBN coupons, or dividends: call `get_passive_income_summary`.

        3. UNIVERSAL TRANSACTION INGESTION:
           - When the user pastes or uploads ANY receipt, transfer proof (BCA, Mandiri, BRI, BNI, GoPay, OVO, ShopeePay), QRIS payment, invoice, bill, or note:
           - Call the `propose_transactions` tool with the structured transaction parameters.
           - If multiple transactions are present, include each one in the transactions array.
           - SBN coupons, bank interest, and stock dividends are ALWAYS type 'INCOME'.
           - Account transfers between user accounts are type 'TRANSFER'.
           - Accompany the tool call with a brief, friendly Markdown summary of the detected transactions.
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

