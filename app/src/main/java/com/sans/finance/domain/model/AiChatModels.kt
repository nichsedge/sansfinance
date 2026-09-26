package com.sans.finance.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import java.util.UUID

@Immutable
@Serializable
data class AiTransactionProposal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amountInCents: Long,
    val type: String = "INCOME", // "INCOME", "EXPENSE", "TRANSFER"
    val date: Long = System.currentTimeMillis(),
    val accountId: Long = 1L,
    val accountName: String = "",
    val categoryId: Long = 1L,
    val categoryName: String = "",
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val isConfirmed: Boolean = false,
    val isRejected: Boolean = false
)

@Immutable
enum class ChatSender {
    USER,
    ASSISTANT
}

@Immutable
@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: ChatSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val proposal: AiTransactionProposal? = null,
    val proposals: List<AiTransactionProposal> = emptyList(),
    val isConfirmed: Boolean = false,
    val isRejected: Boolean = false,
    val isStreaming: Boolean = false
) {
    val allConfirmed: Boolean
        get() = (proposals.isNotEmpty() && proposals.all { it.isConfirmed }) || (proposal != null && isConfirmed)

    val hasPendingProposals: Boolean
        get() = proposals.any { !it.isConfirmed && !it.isRejected } || (proposal != null && !isConfirmed && !isRejected)
}

data class AccountSummary(
    val id: Long,
    val name: String,
    val type: String,
    val currency: String
)

data class CategorySummary(
    val id: Long,
    val name: String,
    val type: String
)

data class FinancialContextSnapshot(
    val monthLabel: String = "",
    val totalIncomeThisMonth: Long = 0L,
    val totalExpenseThisMonth: Long = 0L,
    val netCashflowThisMonth: Long = 0L,
    val savingsRatePercentage: Float = 0f,
    val topExpenseCategories: List<Pair<String, Long>> = emptyList(),
    val accountBalances: List<Pair<String, Long>> = emptyList(),
    val recentTransactions: List<String> = emptyList(),

    // Historical comparison (M-1 / Previous Month)
    val prevMonthLabel: String = "",
    val prevMonthIncome: Long = 0L,
    val prevMonthExpense: Long = 0L,
    val prevMonthSavingsRate: Float = 0f,
    val prevMonthTopCategories: List<Pair<String, Long>> = emptyList(),

    // 3-Month rolling baseline average expense
    val threeMonthAverageExpense: Long = 0L,

    // Big-ticket discrete expenses this month (e.g. "12 Sep: Kos (Rp 2.250.000) [Utility]")
    val topBigTicketExpenses: List<String> = emptyList(),

    // Real-time Balance Sheet & Net Worth Grounding
    val netWorth: Long = 0L,
    val totalAssets: Long = 0L,
    val liquidCashAssets: Long = 0L,
    val portfolioInvestmentValue: Long = 0L,
    val totalLiabilities: Long = 0L,
    val runwayMonths: Double = 0.0,
    val monthlyPassiveIncome: Long = 0L,
    val annualPassiveIncome: Long = 0L,
    val portfolioAssetClassBreakdown: List<Pair<String, Long>> = emptyList(),
    val topPortfolioHoldings: List<Pair<String, Long>> = emptyList(),
    val liabilityAccountBalances: List<Pair<String, Long>> = emptyList()
)

data class AiAssistantResponse(
    val reply: String,
    val proposal: AiTransactionProposal? = null,
    val proposals: List<AiTransactionProposal> = emptyList()
)

/** Events emitted during SSE streaming from the AI provider. */
sealed interface StreamEvent {
    /** A chunk of text content from the model. */
    data class TextDelta(val text: String) : StreamEvent
    /** Stream completed successfully; [fullText] contains the entire accumulated response. */
    data class Done(val fullText: String) : StreamEvent
    /** An error occurred during streaming. */
    data class Error(val message: String) : StreamEvent
}
