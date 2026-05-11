package com.sans.finance.presentation.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RecurringExpensesViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val localeManager: com.sans.finance.data.util.LocaleManager
) : ViewModel() {

    private val _expenses = expenseRepository.getAllExpenses()

    val state = combine(
        _expenses,
        expenseRepository.getAllCategories()
    ) { expenses, categories ->
        val recurringExpenses = expenses.filter { it.isRecurring }
        val totalMonthly = recurringExpenses.sumOf { 
            when (it.recurrenceInterval) {
                "DAILY" -> it.amount * 30
                "WEEKLY" -> it.amount * 4
                "MONTHLY" -> it.amount
                "YEARLY" -> it.amount / 12
                else -> it.amount
            }
        }
        RecurringExpensesState(
            recurringExpenses = recurringExpenses,
            categories = categories,
            totalMonthlyRecurring = totalMonthly,
            currentCurrency = localeManager.getCurrency()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecurringExpensesState()
    )
}

data class RecurringExpensesState(
    val recurringExpenses: List<Expense> = emptyList(),
    val categories: List<com.sans.finance.data.local.entity.CategoryEntity> = emptyList(),
    val totalMonthlyRecurring: Long = 0L,
    val currentCurrency: String = "USD"
)
