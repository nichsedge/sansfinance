package com.sans.finance.presentation.installments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.domain.model.Installment
import com.sans.finance.domain.repository.InstallmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InstallmentsState(
    val activeInstallments: List<Installment> = emptyList(),
    val historyInstallments: List<Installment> = emptyList(),
    val totalMonthlyDue: Long = 0L,
    val totalRemainingBalance: Long = 0L,
    val selectedTab: Int = 0, // 0 for Active, 1 for History
    val currentCurrency: String = "USD"
)

@HiltViewModel
class InstallmentsViewModel @Inject constructor(
    private val installmentRepository: InstallmentRepository,
    private val expenseRepository: com.sans.finance.domain.repository.ExpenseRepository,
    private val localeManager: com.sans.finance.data.util.LocaleManager
) : ViewModel() {

    private val _state = MutableStateFlow(InstallmentsState())
    val state: StateFlow<InstallmentsState> = _state

    init {
        viewModelScope.launch {
            combine(
                installmentRepository.getActiveInstallments(),
                installmentRepository.getCompletedInstallments()
            ) { active, history ->
                Pair(active, history)
            }.collect { (active, history) ->
                _state.update { currentState ->
                    currentState.copy(
                        activeInstallments = active,
                        historyInstallments = history,
                        totalMonthlyDue = active.sumOf { it.monthlyPayment },
                        totalRemainingBalance = active.sumOf { it.remainingBalance },
                        currentCurrency = localeManager.getCurrency()
                    )
                }
            }
        }
    }

    fun onTabSelected(index: Int) {
        _state.update { it.copy(selectedTab = index) }
    }

    fun onToggleStatus(itemId: Long, currentStatus: String) {
        viewModelScope.launch {
            val nextStatus = if (currentStatus == "Paid") "Pending" else "Paid"
            installmentRepository.updateInstallmentItemStatus(itemId, nextStatus)
        }
    }

    fun getItemsForInstallment(installmentId: Long) =
        installmentRepository.getInstallmentItems(installmentId)

    fun deleteInstallmentPlan(installment: Installment) {
        viewModelScope.launch {
            expenseRepository.getExpenseById(installment.expenseId)?.let { expense ->
                expenseRepository.deleteExpense(expense)
            }
        }
    }
}
