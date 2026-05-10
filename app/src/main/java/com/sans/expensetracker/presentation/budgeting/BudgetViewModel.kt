package com.sans.expensetracker.presentation.budgeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.expensetracker.data.local.entity.BudgetEntity
import com.sans.expensetracker.domain.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    val budgets = budgetRepository.getAllBudgets().stateIn(
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
