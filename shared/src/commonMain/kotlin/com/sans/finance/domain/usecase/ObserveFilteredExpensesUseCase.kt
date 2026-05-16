package com.sans.finance.domain.usecase

import com.sans.finance.domain.model.ExpenseFilter
import com.sans.finance.domain.model.FilteredExpensesData
import com.sans.finance.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveFilteredExpensesUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    operator fun invoke(filter: ExpenseFilter): Flow<FilteredExpensesData> {
        val expensesFlow = repository.getFilteredExpenses(
            query = filter.query,
            categoryIds = filter.categoryIds.toList(),
            accountIds = filter.accountIds.toList(),
            since = filter.since,
            until = filter.until,
            minAmount = filter.minAmount,
            maxAmount = filter.maxAmount,
            tags = filter.tags.toList(),
            types = filter.types.toList()
        )
        val dailySpendingFlow = repository.getDailySpendingBetween(filter.since, filter.until)
        return combine(expensesFlow, dailySpendingFlow) { expenses, dailySpending ->
            FilteredExpensesData(expenses = expenses, dailySpending = dailySpending)
        }
    }
}
