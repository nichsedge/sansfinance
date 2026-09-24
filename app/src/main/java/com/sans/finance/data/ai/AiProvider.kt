package com.sans.finance.data.ai

import com.sans.finance.domain.model.AccountSummary
import com.sans.finance.domain.model.AiAssistantResponse
import com.sans.finance.domain.model.CategorySummary
import com.sans.finance.domain.model.ChatMessage

interface AiProvider {
    suspend fun generateMonthlyReview(input: MonthlyReviewInput): MonthlyReviewResult
    suspend fun generatePortfolioAnalysis(input: PortfolioAnalysisInput): PortfolioAnalysisResult
    suspend fun parseReceiptOrChat(
        userMessage: String,
        accounts: List<AccountSummary>,
        categories: List<CategorySummary>,
        currency: String,
        conversationHistory: List<ChatMessage> = emptyList()
    ): AiAssistantResponse
}

data class MonthlyReviewInput(
    val monthLabel: String,
    val baseCurrency: String,
    val income: Long,
    val expense: Long,
    val topCategories: List<Pair<String, Long>>,
    val notes: String
)

