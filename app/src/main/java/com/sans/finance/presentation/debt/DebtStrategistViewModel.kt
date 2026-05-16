package com.sans.finance.presentation.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.pow

data class DebtStrategyResult(
    val strategyName: String,
    val monthsToDebtFree: Int,
    val totalInterestPaid: Double,
    val repaymentPlan: List<MonthlyPayment>
)

data class MonthlyPayment(
    val month: Int,
    val remainingBalance: Double,
    val interestPaid: Double
)

data class DebtStrategistState(
    val loans: List<AccountEntity> = emptyList(),
    val additionalMonthlyPayment: Long = 0L,
    val snowballResult: DebtStrategyResult? = null,
    val avalancheResult: DebtStrategyResult? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class DebtStrategistViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val accountTypeRepository: com.sans.finance.domain.repository.AccountTypeRepository
) : ViewModel() {

    private val _additionalPayment = MutableStateFlow(0L)

    val state = combine(
        accountRepository.getAllAccounts(),
        accountTypeRepository.getAllAccountTypes(),
        _additionalPayment
    ) { accounts, types, additional ->
        val liabilityTypeNames = types.filter { it.isLiability }.map { it.name }.toSet()
        val loans = accounts.filter { it.type in liabilityTypeNames }
            .filter { it.balance > 0 } // Assuming liability balances are positive in the DB for loans/CC
            .map { it.copy() } // Just work with it

        if (loans.isEmpty()) {
            return@combine DebtStrategistState(loans = emptyList(), isLoading = false)
        }

        val snowball = calculateStrategy(loans, additional, "Snowball") // By balance
        val avalanche = calculateStrategy(loans, additional, "Avalanche") // By interest rate

        DebtStrategistState(
            loans = loans,
            additionalMonthlyPayment = additional,
            snowballResult = snowball,
            avalancheResult = avalanche,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DebtStrategistState()
    )

    fun updateAdditionalPayment(amount: Long) {
        _additionalPayment.value = amount
    }

    private fun calculateStrategy(
        initialLoans: List<AccountEntity>,
        extraPayment: Long,
        type: String
    ): DebtStrategyResult {
        val sortedLoans = when (type) {
            "Snowball" -> initialLoans.sortedBy { it.balance }
            "Avalanche" -> initialLoans.sortedByDescending { it.interestRate }
            else -> initialLoans
        }.map { LoanState(it.name, it.balance.toDouble(), it.interestRate, it.minPayment.toDouble()) }

        var months = 0
        var totalInterest = 0.0
        val history = mutableListOf<MonthlyPayment>()

        val currentLoans = sortedLoans.toMutableList()

        while (currentLoans.any { it.balance > 0 } && months < 360) { // Max 30 years
            months++
            var monthlyInterest = 0.0
            var availableExtra = extraPayment.toDouble()

            // 1. Pay minimums and accrue interest
            currentLoans.forEach { loan ->
                if (loan.balance > 0) {
                    val interest = (loan.balance * (loan.interestRate / 100.0)) / 12.0
                    loan.balance += interest
                    monthlyInterest += interest
                    totalInterest += interest

                    val payment = loan.minPayment.coerceAtMost(loan.balance)
                    loan.balance -= payment
                    // Extra payment logic will be applied to the target loan later
                }
            }

            // 2. Apply extra payment to the first loan in sorted list that still has balance
            val targetLoan = currentLoans.find { it.balance > 0 }
            if (targetLoan != null) {
                val extra = availableExtra.coerceAtMost(targetLoan.balance)
                targetLoan.balance -= extra
            }

            val totalRemaining = currentLoans.sumOf { it.balance }
            history.add(MonthlyPayment(months, totalRemaining, monthlyInterest))
        }

        return DebtStrategyResult(type, months, totalInterest, history)
    }

    private class LoanState(val name: String, var balance: Double, val interestRate: Double, val minPayment: Double)
}
