package com.sans.finance.data.ai

import com.sans.finance.domain.model.AccountSummary
import com.sans.finance.domain.model.AiAssistantResponse
import com.sans.finance.domain.model.CategorySummary
import com.sans.finance.domain.model.ChatMessage
import com.sans.finance.domain.model.FinancialContextSnapshot
import com.sans.finance.domain.model.StreamEvent
import kotlinx.coroutines.flow.Flow

interface AiProvider {
    suspend fun generateMonthlyReview(input: MonthlyReviewInput): MonthlyReviewResult
    suspend fun generatePortfolioAnalysis(input: PortfolioAnalysisInput): PortfolioAnalysisResult

    /**
     * Stream a portfolio analysis via SSE or fallback. Emits [StreamEvent.TextDelta] for each token,
     * [StreamEvent.Done] when complete, or [StreamEvent.Error] on failure.
     * Default implementation falls back to non-streaming [generatePortfolioAnalysis].
     */
    fun streamPortfolioAnalysis(
        input: PortfolioAnalysisInput
    ): Flow<StreamEvent> = kotlinx.coroutines.flow.flow {
        val response = generatePortfolioAnalysis(input)
        val text = response.rawText ?: (response.summary + "\n\n" + response.insights.joinToString("\n\n") {
            "• **${it.title}** (${it.importance})\n  *Observasi:* ${it.observation}\n  *Saran:* ${it.suggestion}"
        })
        emit(StreamEvent.TextDelta(text))
        emit(StreamEvent.Done(text))
    }

    suspend fun parseReceiptOrChat(
        userMessage: String,
        accounts: List<AccountSummary>,
        categories: List<CategorySummary>,
        currency: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        financialContext: FinancialContextSnapshot? = null
    ): AiAssistantResponse

    /**
     * Stream a chat response via SSE. Emits [StreamEvent.TextDelta] for each token,
     * [StreamEvent.Done] when complete, or [StreamEvent.Error] on failure.
     * Default implementation falls back to non-streaming [parseReceiptOrChat].
     */
    fun streamChat(
        userMessage: String,
        accounts: List<AccountSummary>,
        categories: List<CategorySummary>,
        currency: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        financialContext: FinancialContextSnapshot? = null
    ): Flow<StreamEvent> = kotlinx.coroutines.flow.flow {
        val response = parseReceiptOrChat(
            userMessage, accounts, categories, currency, conversationHistory, financialContext
        )
        if (response.proposals.isNotEmpty()) {
            val jsonProposals = org.json.JSONArray().apply {
                response.proposals.forEach { p ->
                    put(org.json.JSONObject().apply {
                        put("id", p.id)
                        put("title", p.title)
                        put("amount", p.amountInCents / 100.0)
                        put("type", p.type)
                        put("date", p.date)
                        put("accountId", p.accountId)
                        put("accountName", p.accountName)
                        put("categoryId", p.categoryId)
                        put("categoryName", p.categoryName)
                        put("notes", p.notes)
                        put("tags", org.json.JSONArray(p.tags))
                    })
                }
            }
            val fullPayload = org.json.JSONObject().apply {
                put("reply", response.reply)
                put("proposals", jsonProposals)
            }.toString()
            emit(StreamEvent.Done(fullPayload))
        } else {
            emit(StreamEvent.Done(response.reply))
        }
    }
}

data class MonthlyReviewInput(
    val monthLabel: String,
    val baseCurrency: String,
    val income: Long,
    val expense: Long,
    val topCategories: List<Pair<String, Long>>,
    val notes: String
)

