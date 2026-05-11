package com.sans.finance.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable
    object Main : Screen()

    @Serializable
    object Dashboard : Screen()

    @Serializable
    object ExpenseList : Screen()

    @Serializable
    object AddTransaction : Screen()

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
    object CategorySettings : Screen()

    @Serializable
    object TagSettings : Screen()

    @Serializable
    object RecurringExpenses : Screen()

    @Serializable
    object Accounts : Screen()

    @Serializable
    object AccountStats : Screen()

    @Serializable
    object Portfolio : Screen()

    @Serializable
    object Goals : Screen()

    @Serializable
    object Budgets : Screen()

//    @Serializable
//    object ScanReceipt : Screen()
}
