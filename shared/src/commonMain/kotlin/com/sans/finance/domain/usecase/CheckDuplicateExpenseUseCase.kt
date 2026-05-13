package com.sans.finance.domain.usecase

import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.ExpenseRepository
import javax.inject.Inject

class CheckDuplicateExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(title: String, amount: Long, date: Long, accountId: Long): Expense? {
        if (title.isBlank()) return null
        return repository.findPotentialDuplicate(title.trim(), amount, date, accountId)
    }
}
