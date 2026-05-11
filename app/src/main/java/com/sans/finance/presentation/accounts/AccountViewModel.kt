package com.sans.finance.presentation.accounts


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.sans.finance.core.util.CalendarUtils
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

data class AccountScreenState(
    val assets: Long = 0L,
    val liabilities: Long = 0L,
    val total: Long = 0L,
    val accountsByType: Map<String, List<AccountEntity>> = emptyMap(),
    val history: List<Pair<String, Long>> = emptyList(),
    val isLoading: Boolean = true
)


@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    val state = combine(
        accountRepository.getAllAccounts(),
        expenseRepository.getExpensesBetween(0, Long.MAX_VALUE)
    ) { accountsList, expensesList ->
        val assets = accountsList.filter { it.type != "Credit Card" && it.type != "Loan" }.sumOf { it.balance }
        val liabilities = accountsList.filter { it.type == "Credit Card" || it.type == "Loan" }.sumOf { it.balance }
        val total = assets - liabilities

        val grouped = accountsList.groupBy { it.type }

        // Calculate 6 months history
        val historyList = mutableListOf<Pair<String, Long>>()
        val cal = CalendarUtils.getInstance()
        val monthFormat = SimpleDateFormat("MMM", Locale.US)

        var currentSimulatedNetWorth = total

        // We go backwards from current month to 5 months ago
        for (i in 0 until 6) {
            val monthEndCal = cal.clone() as Calendar
            monthEndCal.set(Calendar.DAY_OF_MONTH, monthEndCal.getActualMaximum(Calendar.DAY_OF_MONTH))
            monthEndCal.set(Calendar.HOUR_OF_DAY, 23)
            monthEndCal.set(Calendar.MINUTE, 59)
            monthEndCal.set(Calendar.SECOND, 59)
            val monthEnd = monthEndCal.timeInMillis

            val monthStartCal = cal.clone() as Calendar
            monthStartCal.set(Calendar.DAY_OF_MONTH, 1)
            monthStartCal.set(Calendar.HOUR_OF_DAY, 0)
            monthStartCal.set(Calendar.MINUTE, 0)
            monthStartCal.set(Calendar.SECOND, 0)
            val monthStart = monthStartCal.timeInMillis

            // For the current iteration, we record the net worth
            historyList.add(0, Pair(monthFormat.format(cal.time), currentSimulatedNetWorth))

            // To get the PREVIOUS month's ending net worth, we undo this month's net flow
            val monthlyExpenses = expensesList.filter { it.date in monthStart..monthEnd && (!it.isInstallment || it.isInstallmentPayment) }
            val monthlyIncome = monthlyExpenses.filter { it.type == "INCOME" }.sumOf { it.amount }
            val monthlyExpenseSum = monthlyExpenses.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val netFlow = monthlyIncome - monthlyExpenseSum

            currentSimulatedNetWorth -= netFlow

            cal.add(Calendar.MONTH, -1)
        }

        AccountScreenState(
            assets = assets,
            liabilities = liabilities,
            total = total,
            accountsByType = grouped,
            history = historyList,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountScreenState()
    )

    fun addAccount(name: String, type: String, initialBalance: Long, currency: String = "IDR") {
        viewModelScope.launch {
            accountRepository.insertAccount(
                AccountEntity(
                    name = name,
                    type = type,
                    balance = initialBalance,
                    currency = currency
                )
            )
        }
    }

    fun updateAccount(account: AccountEntity, newName: String, newType: String, newBalance: Long) {
        viewModelScope.launch {
            accountRepository.updateAccount(
                account.copy(
                    name = newName,
                    type = newType,
                    balance = newBalance,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            accountRepository.deleteAccountById(id)
        }
    }
}
