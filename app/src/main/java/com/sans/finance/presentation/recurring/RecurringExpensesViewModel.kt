package com.sans.finance.presentation.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.domain.model.Category
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecurringExpensesViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val localeManager: com.sans.finance.data.util.LocaleManager
) : ViewModel() {

    private val _expenses = expenseRepository.getAllExpenses()

    private val _viewMode = kotlinx.coroutines.flow.MutableStateFlow(RecurringViewMode.MONTHLY)

    val state = combine(
        _expenses,
        expenseRepository.getAllCategories(),
        _viewMode
    ) { expenses, categories, viewMode ->
        val recurringExpenses = expenses.filter { it.isRecurring }
        val totalMonthly = recurringExpenses.sumOf {
            calculateMonthlyAmount(it)
        }

        RecurringExpensesState(
            recurringExpenses = recurringExpenses,
            categories = categories,
            totalMonthlyRecurring = totalMonthly,
            currentCurrency = localeManager.getCurrency(),
            viewMode = viewMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecurringExpensesState()
    )

    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            RecurringViewMode.MONTHLY -> RecurringViewMode.ANNUAL
            RecurringViewMode.ANNUAL -> RecurringViewMode.OPPORTUNITY_COST_10Y
            RecurringViewMode.OPPORTUNITY_COST_10Y -> RecurringViewMode.MONTHLY
        }
    }

    private fun calculateMonthlyAmount(expense: Expense): Long {
        return when (expense.recurrenceInterval) {
            "DAILY" -> expense.amount * 30
            "WEEKLY" -> expense.amount * 4
            "MONTHLY" -> expense.amount
            "YEARLY" -> expense.amount / 12
            else -> expense.amount
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
        }
    }
}

enum class RecurringViewMode {
    MONTHLY, ANNUAL, OPPORTUNITY_COST_10Y
}

data class RecurringExpensesState(
    val recurringExpenses: List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val totalMonthlyRecurring: Long = 0L,
    val currentCurrency: String = "USD",
    val viewMode: RecurringViewMode = RecurringViewMode.MONTHLY
)
