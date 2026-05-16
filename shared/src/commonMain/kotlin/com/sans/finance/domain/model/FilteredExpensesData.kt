package com.sans.finance.domain.model

data class FilteredExpensesData(
    val expenses: List<Expense>,
    val dailySpending: List<DaySpent>
)
