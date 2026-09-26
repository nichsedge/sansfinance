package com.sans.finance.data.ai

import com.sans.finance.domain.model.AccountSummary
import com.sans.finance.domain.model.AiAssistantResponse
import com.sans.finance.domain.model.AiTransactionProposal
import com.sans.finance.domain.model.CategorySummary
import com.sans.finance.domain.model.ChatMessage
import com.sans.finance.domain.model.FinancialContextSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
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

class OpenAiResponsesProvider(
    rawClient: OkHttpClient,
    rawApiKey: String,
    rawModel: String,
) : AiProvider {

    private val apiKey = rawApiKey.filter { it > ' ' && it.code < 127 }
    private val model = rawModel.filter { it > ' ' && it.code < 127 }

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

    private fun extractJson(raw: String): String {
        val trimmed = raw.trim()
        val fencedMatch = Regex("""```(?:json)?\s*([\s\S]*?)\s*```""").find(trimmed)
        if (fencedMatch != null) {
            val inner = fencedMatch.groupValues[1].trim()
            val start = inner.indexOf('{')
            val end = inner.lastIndexOf('}')
            if (start != -1 && end != -1 && end > start) {
                return inner.substring(start, end + 1)
            }
            return inner
        }
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            return trimmed.substring(start, end + 1)
        }
        return trimmed
    }

    private fun parseJsonResult(text: String): MonthlyReviewResult? {
        val clean = extractJson(text)
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
                "instructions",
                JsonPrimitive(
                    """
                    You are an expert wealth management advisor. 
                    Analyze the user's portfolio and return a concise JSON object.
                    Schema: { "summary": string, "insights": [{ "title": string, "observation": string, "suggestion": string, "importance": "LOW"|"MEDIUM"|"HIGH" }] }
                    """.trimIndent()
                )
            )
            put(
                "input",
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
                val parsed = outputText?.let { parsePortfolioJsonResult(it) }
                parsed ?: PortfolioAnalysisResult(
                    summary = "Portfolio Analysis",
                    insights = emptyList(),
                    rawText = outputText ?: raw
                )
            }
        }
    }

    private fun parsePortfolioJsonResult(text: String): PortfolioAnalysisResult? {
        val clean = extractJson(text)
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
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val accountsFormatted = accounts.joinToString(separator = "\n") {
            "- ID: ${it.id} | Name: '${it.name}' | Type: ${it.type} | Currency: ${it.currency}"
        }
        val categoriesFormatted = categories.joinToString(separator = "\n") {
            "- ID: ${it.id} | Name: '${it.name}' | Type: ${it.type}"
        }

        val instructions = """
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
               - Grounding Invariant: Thoroughly inspect the Real-time Financial Context provided in the input. Never say historical data is unavailable when previous month (M-1) or 3-month baseline is present. Always cite exact numbers, percentages, and deltas between months.
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
        """.trimIndent()

        val financialContextFormatted = buildFinancialContextString(financialContext, currency)
        val finalInput = if (financialContextFormatted.isNotBlank()) {
            "Context: current_timestamp=${System.currentTimeMillis()}, date=$todayStr\n\n$financialContextFormatted\n\nUser Message: $userMessage"
        } else {
            userMessage
        }

        val bodyJson = buildJsonObject {
            put("model", JsonPrimitive(model))
            put("instructions", JsonPrimitive(instructions))
            put("input", JsonPrimitive(finalInput))
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

                val rawOutput = outputText ?: raw
                AiJsonParser.parseAssistantResponse(rawOutput, accounts, categories)
            }
        }
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

