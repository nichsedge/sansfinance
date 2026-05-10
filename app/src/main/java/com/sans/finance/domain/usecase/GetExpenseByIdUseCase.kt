package com.sans.finance.domain.usecase

import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.ExpenseRepository
import javax.inject.Inject

class GetExpenseByIdUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(id: Long): Expense? {
        return repository.getExpenseById(id)
    }
}
