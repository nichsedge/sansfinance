package com.sans.finance.domain.usecase

import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.ExpenseRepository
import javax.inject.Inject

class PredictTransactionUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(note: String): TransactionPrediction? {
        val lastExpense = repository.getPredictionForNote(note) ?: return null
        return TransactionPrediction(
            categoryId = lastExpense.categoryId,
            accountId = lastExpense.accountId,
            type = lastExpense.type,
            tags = lastExpense.tags
        )
    }
}

data class TransactionPrediction(
    val categoryId: Long,
    val accountId: Long,
    val type: String,
    val tags: List<String>
)
