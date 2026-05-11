package com.sans.finance.presentation.budgeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.data.local.entity.BudgetEntity
import com.sans.finance.domain.repository.BudgetRepository
import com.sans.finance.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class BudgetStatus(
    val budget: BudgetEntity,
    val spent: Long,
    val categoryName: String? = null
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _categories = expenseRepository.getAllCategories().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val categories = _categories

    val budgetStatuses = combine(
        budgetRepository.getAllBudgets(),
        _categories
    ) { budgets, categories ->
        val calendar = CalendarUtils.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val end = calendar.timeInMillis

        budgets.map { budget ->
            val spentFlow = if (budget.categoryId != null) {
                expenseRepository.getSpendingByCategoryBetween(start, end)
                    .map { categorySpents ->
                        categorySpents.find { it.categoryId == budget.categoryId }?.totalAmount
                            ?: 0L
                    }
            } else {
                expenseRepository.getTotalSpentBetween(start, end).map { it ?: 0L }
            }

            val spent =
                spentFlow.first() // This might be problematic in a combine block if it's not a cold flow that emits quickly

            BudgetStatus(
                budget = budget,
                spent = spent,
                categoryName = categories.find { it.id == budget.categoryId }?.name
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addBudget(amount: Long, categoryId: Long? = null, accountId: Long? = null) {
        viewModelScope.launch {
            budgetRepository.insertBudget(
                BudgetEntity(
                    amount = amount,
                    categoryId = categoryId,
                    accountId = accountId
                )
            )
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(budget)
        }
    }
}
