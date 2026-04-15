package com.sans.expensetracker.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable
    object ExpenseList : Screen()

    @Serializable
    object AddExpense : Screen()

    @Serializable
    data class ExpenseDetail(val expenseId: Long) : Screen()

    @Serializable
    data class EditExpense(val expenseId: Long) : Screen()

    @Serializable
    object Installments : Screen()

    @Serializable
    object Stats : Screen()

    @Serializable
    object Settings : Screen()

    @Serializable
    object RecurringExpenses : Screen()

    @Serializable
    object ScanReceipt : Screen()
}
