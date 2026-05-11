package com.sans.finance.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class AccountStatsState(
    val selectedDate: Calendar = CalendarUtils.getInstance(),
    val totalBalance: Long = 0L,
    val netWorthHistory: List<Pair<String, Long>> = emptyList(),
    val incomeExpenseHistory: List<Triple<String, Long, Long>> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AccountStatsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(CalendarUtils.getInstance())

    val state: StateFlow<AccountStatsState> = combine(
        _selectedDate,
        accountRepository.getAllAccounts(),
        expenseRepository.getExpensesBetween(0, Long.MAX_VALUE)
    ) { date, accounts, transactions ->
        val currentAssets = accounts.filter { it.type != "Credit Card" && it.type != "Loan" }.sumOf { it.balance }
        val currentLiabilities = accounts.filter { it.type == "Credit Card" || it.type == "Loan" }.sumOf { it.balance }
        val currentTotal = currentAssets - currentLiabilities

        val netWorthHistory = mutableListOf<Pair<String, Long>>()
        val incomeExpenseHistory = mutableListOf<Triple<String, Long, Long>>()
        
        val cal = date.clone() as Calendar
        val monthFormat = SimpleDateFormat("MMM", Locale.US)

        var runningNetWorth = currentTotal
        
        // If the selected date is in the future relative to "now", we might need to adjust.
        // But for simplicity, let's assume we calculate backwards from the selected date's month end.
        
        // Actually, let's calculate from CURRENT month backwards if selected date is current or past.
        // If selected date is in the past, we first need to "revert" from now to that date.
        
        val now = CalendarUtils.getInstance()
        val monthsToRevert = ((now.get(Calendar.YEAR) - date.get(Calendar.YEAR)) * 12) + (now.get(Calendar.MONTH) - date.get(Calendar.MONTH))
        
        var tempCal = now.clone() as Calendar
        var simulatedNetWorthAtSelectedDate = currentTotal
        
        for (i in 0 until monthsToRevert) {
            tempCal.set(Calendar.DAY_OF_MONTH, 1)
            tempCal.set(Calendar.HOUR_OF_DAY, 0)
            tempCal.set(Calendar.MINUTE, 0)
            tempCal.set(Calendar.SECOND, 0)
            val start = tempCal.timeInMillis
            tempCal.set(Calendar.DAY_OF_MONTH, tempCal.getActualMaximum(Calendar.DAY_OF_MONTH))
            tempCal.set(Calendar.HOUR_OF_DAY, 23)
            tempCal.set(Calendar.MINUTE, 59)
            val end = tempCal.timeInMillis
            
            val txns = transactions.filter { it.date in start..end && (!it.isInstallment || it.isInstallmentPayment) }
            val netFlow = txns.filter { it.type == "INCOME" }.sumOf { it.amount } - txns.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            
            simulatedNetWorthAtSelectedDate -= netFlow
            tempCal.add(Calendar.MONTH, -1)
        }

        var historyCal = date.clone() as Calendar
        var historyNetWorth = simulatedNetWorthAtSelectedDate

        for (i in 0 until 6) {
            historyCal.set(Calendar.DAY_OF_MONTH, 1)
            val start = historyCal.timeInMillis
            historyCal.set(Calendar.DAY_OF_MONTH, historyCal.getActualMaximum(Calendar.DAY_OF_MONTH))
            val end = historyCal.timeInMillis

            val txns = transactions.filter { it.date in start..end && (!it.isInstallment || it.isInstallmentPayment) }
            val income = txns.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = txns.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            
            netWorthHistory.add(0, Pair(monthFormat.format(historyCal.time), historyNetWorth))
            incomeExpenseHistory.add(0, Triple(monthFormat.format(historyCal.time), income, expense))
            
            val netFlow = income - expense
            historyNetWorth -= netFlow
            historyCal.add(Calendar.MONTH, -1)
        }

        AccountStatsState(
            selectedDate = date,
            totalBalance = simulatedNetWorthAtSelectedDate,
            netWorthHistory = netWorthHistory,
            incomeExpenseHistory = incomeExpenseHistory,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountStatsState()
    )

    fun onPreviousMonth() {
        _selectedDate.update {
            val newDate = it.clone() as Calendar
            newDate.add(Calendar.MONTH, -1)
            newDate
        }
    }

    fun onNextMonth() {
        _selectedDate.update {
            val newDate = it.clone() as Calendar
            newDate.add(Calendar.MONTH, 1)
            newDate
        }
    }
}
