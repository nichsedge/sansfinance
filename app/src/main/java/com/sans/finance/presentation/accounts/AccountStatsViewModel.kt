package com.sans.finance.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.core.util.groupByParent
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

data class AccountBreakdownItem(
    val name: String,
    val type: String,
    val rolledUpBalance: Long,
    val subaccounts: List<Pair<String, Long>> = emptyList(),
    val isLiability: Boolean = false
)

data class AccountStatsState(
    val selectedDate: Calendar = CalendarUtils.getInstance(),
    val totalBalance: Long = 0L,
    val balanceHistory: List<Pair<String, Long>> = emptyList(),
    val incomeExpenseHistory: List<Triple<String, Long, Long>> = emptyList(),
    val accountBreakdown: List<AccountBreakdownItem> = emptyList(),
    val currentCurrency: String = "USD",
    val isLoading: Boolean = true
)

@HiltViewModel
class AccountStatsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val expenseRepository: ExpenseRepository,
    private val accountTypeRepository: com.sans.finance.domain.repository.AccountTypeRepository,
    private val currencyDao: com.sans.finance.data.local.dao.CurrencyDao,
    private val localeManager: com.sans.finance.data.util.LocaleManager
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(CalendarUtils.getInstance())

    val state: StateFlow<AccountStatsState> = combine(
        _selectedDate,
        accountRepository.getAllAccounts(),
        expenseRepository.getExpensesBetween(0, Long.MAX_VALUE),
        accountTypeRepository.getAllAccountTypes(),
        currencyDao.getAllRates()
    ) { date, accounts, transactions, types, rates ->
        val baseCurrency = localeManager.getCurrency()
        val ratesMap = rates.associate { it.code to it.rateToIdr }
        val baseRate = if (baseCurrency == "IDR") 1.0 else (ratesMap[baseCurrency] ?: 1.0)

        fun convertToBase(amount: Long, from: String): Long {
            if (from == baseCurrency) return amount
            val fromRate = if (from == "IDR") 1.0 else (ratesMap[from] ?: 1.0)
            val toRate = baseRate
            if (toRate == 0.0) return amount
            return ((amount * fromRate) / toRate).toLong()
        }

        val liabilityTypeNames = types.filter { it.isLiability }.map { it.name }.toSet()
        val currentAssets =
            accounts.filter { it.type !in liabilityTypeNames }.sumOf { convertToBase(it.balance, it.currency) }
        val currentLiabilities =
            accounts.filter { it.type in liabilityTypeNames }.sumOf { convertToBase(it.balance, it.currency) }
        val currentTotal = currentAssets - currentLiabilities

        val balanceHistory = mutableListOf<Pair<String, Long>>()
        val incomeExpenseHistory = mutableListOf<Triple<String, Long, Long>>()

        val cal = date.clone() as Calendar
        val monthFormat = SimpleDateFormat("MMM", Locale.US)

        var runningBalance = currentTotal

        // If the selected date is in the future relative to "now", we might need to adjust.
        // But for simplicity, let's assume we calculate backwards from the selected date's month end.

        // Actually, let's calculate from CURRENT month backwards if selected date is current or past.
        // If selected date is in the past, we first need to "revert" from now to that date.

        val now = CalendarUtils.getInstance()
        val monthsToRevert =
            ((now.get(Calendar.YEAR) - date.get(Calendar.YEAR)) * 12) + (now.get(Calendar.MONTH) - date.get(
                Calendar.MONTH
            ))

        var tempCal = now.clone() as Calendar
        var simulatedBalanceAtSelectedDate = currentTotal

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

            val txns =
                transactions.filter { it.date in start..end && (!it.isInstallment || it.isInstallmentPayment) }
            val netFlow = txns.filter { it.type == "INCOME" }
                .sumOf { convertToBase(it.amount, it.currency) } - txns.filter { it.type == "EXPENSE" }.sumOf { convertToBase(it.amount, it.currency) }

            simulatedBalanceAtSelectedDate -= netFlow
            tempCal.add(Calendar.MONTH, -1)
        }

        var historyCal = date.clone() as Calendar
        var historyBalance = simulatedBalanceAtSelectedDate

        for (i in 0 until 6) {
            historyCal.set(Calendar.DAY_OF_MONTH, 1)
            val start = historyCal.timeInMillis
            historyCal.set(
                Calendar.DAY_OF_MONTH,
                historyCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            )
            val end = historyCal.timeInMillis

            val txns =
                transactions.filter { it.date in start..end && (!it.isInstallment || it.isInstallmentPayment) }
            val income = txns.filter { it.type == "INCOME" }.sumOf { convertToBase(it.amount, it.currency) }
            val expense = txns.filter { it.type == "EXPENSE" }.sumOf { convertToBase(it.amount, it.currency) }

            balanceHistory.add(0, Pair(monthFormat.format(historyCal.time), historyBalance))
            incomeExpenseHistory.add(
                0,
                Triple(monthFormat.format(historyCal.time), income, expense)
            )

            val netFlow = income - expense
            historyBalance -= netFlow
            historyCal.add(Calendar.MONTH, -1)
        }

        val hierarchy = accounts.groupByParent()
        val childrenByParent = hierarchy.childrenByParent
        val accountBreakdown = hierarchy.topLevel
            .map { parent ->
                val isLiability = parent.type in liabilityTypeNames
                val children = childrenByParent[parent.id].orEmpty()
                val parentOwn = convertToBase(parent.balance, parent.currency)
                val childPairs = children.map { child ->
                    "${child.name} (${child.currency})" to convertToBase(child.balance, child.currency)
                }
                val rolledUp = parentOwn + childPairs.sumOf { it.second }
                AccountBreakdownItem(
                    name = parent.name,
                    type = parent.type,
                    rolledUpBalance = if (isLiability) -rolledUp else rolledUp,
                    subaccounts = childPairs.map { (n, v) -> n to (if (isLiability) -v else v) },
                    isLiability = isLiability
                )
            }
            .sortedByDescending { kotlin.math.abs(it.rolledUpBalance) }

        AccountStatsState(
            selectedDate = date,
            totalBalance = simulatedBalanceAtSelectedDate,
            balanceHistory = balanceHistory,
            incomeExpenseHistory = incomeExpenseHistory,
            accountBreakdown = accountBreakdown,
            currentCurrency = baseCurrency,
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
