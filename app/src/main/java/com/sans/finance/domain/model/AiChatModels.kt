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
    val isRejected: Boolean = false
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

data class AiAssistantResponse(
    val reply: String,
    val proposal: AiTransactionProposal? = null,
    val proposals: List<AiTransactionProposal> = emptyList()
)
