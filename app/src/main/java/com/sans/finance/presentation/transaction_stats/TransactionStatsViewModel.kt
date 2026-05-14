package com.sans.finance.presentation.transaction_stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.domain.model.CategorySpent
import com.sans.finance.domain.model.DaySpent
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import java.util.Calendar
import javax.inject.Inject

enum class TransactionStatsPeriodType {
    WEEKLY, MONTHLY, ANNUALLY, CUSTOM
}

enum class TransactionType {
    INCOME, EXPENSE
}

data class TransactionStatsState(
    val selectedPeriodType: TransactionStatsPeriodType = TransactionStatsPeriodType.MONTHLY,
    val selectedTransactionType: TransactionType = TransactionType.EXPENSE,
    val currentPeriodDate: Calendar = CalendarUtils.getInstance(),
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val breakdown: List<CategorySpent> = emptyList(),
    val totalIncomeForPeriod: Long = 0L,
    val totalExpenseForPeriod: Long = 0L,
    val cashFlow: Long = 0L,
    val savingsRate: Double = 0.0,
    val selectedCategory: CategorySpent? = null,
    val categoryTrend: List<DaySpent> = emptyList(),
    val categoryTransactions: List<Expense> = emptyList(),
    val isLoading: Boolean = false,
    val currentCurrency: String = "USD"
)

@HiltViewModel
class TransactionStatsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val localeManager: com.sans.finance.data.util.LocaleManager,
    private val currencyDao: com.sans.finance.data.local.dao.CurrencyDao
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionStatsState())
    val state: StateFlow<TransactionStatsState> = _state.asStateFlow()

    private var dataJob: Job? = null

    init {
        _state.update { it.copy(currentCurrency = localeManager.getCurrency()) }
        loadData()
    }

    fun onPeriodTypeSelected(type: TransactionStatsPeriodType) {
        _state.update { it.copy(selectedPeriodType = type, selectedCategory = null) }
        loadData()
    }

    fun onTransactionTypeSelected(type: TransactionType) {
        _state.update { it.copy(selectedTransactionType = type, selectedCategory = null) }
        loadData()
    }

    fun onCategorySelected(category: CategorySpent?) {
        _state.update { it.copy(selectedCategory = category) }
        loadData()
    }

    fun onNextPeriod() {
        _state.update {
            val nextDate = it.currentPeriodDate.clone() as Calendar
            when (it.selectedPeriodType) {
                TransactionStatsPeriodType.WEEKLY -> nextDate.add(Calendar.WEEK_OF_YEAR, 1)
                TransactionStatsPeriodType.MONTHLY -> nextDate.add(Calendar.MONTH, 1)
                TransactionStatsPeriodType.ANNUALLY -> nextDate.add(Calendar.YEAR, 1)
                TransactionStatsPeriodType.CUSTOM -> {}
            }
            it.copy(currentPeriodDate = nextDate)
        }
        loadData()
    }

    fun onPreviousPeriod() {
        _state.update {
            val prevDate = it.currentPeriodDate.clone() as Calendar
            when (it.selectedPeriodType) {
                TransactionStatsPeriodType.WEEKLY -> prevDate.add(Calendar.WEEK_OF_YEAR, -1)
                TransactionStatsPeriodType.MONTHLY -> prevDate.add(Calendar.MONTH, -1)
                TransactionStatsPeriodType.ANNUALLY -> prevDate.add(Calendar.YEAR, -1)
                TransactionStatsPeriodType.CUSTOM -> {}
            }
            it.copy(currentPeriodDate = prevDate)
        }
        loadData()
    }

    fun onCustomDateRangeSelected(start: Long, end: Long) {
        _state.update {
            it.copy(
                selectedPeriodType = TransactionStatsPeriodType.CUSTOM,
                customStartDate = start,
                customEndDate = end,
                selectedCategory = null
            )
        }
        loadData()
    }

    fun onDateSelected(month: Int, year: Int) {
        _state.update {
            val newDate = it.currentPeriodDate.clone() as Calendar
            newDate.set(Calendar.YEAR, year)
            newDate.set(Calendar.MONTH, month)
            newDate.set(Calendar.DAY_OF_MONTH, 1)
            it.copy(currentPeriodDate = newDate, selectedCategory = null)
        }
        loadData()
    }

    private fun loadData() {
        dataJob?.cancel()

        val currentState = _state.value
        val (since, until) = getRange(currentState)
        val type = currentState.selectedTransactionType.name

        _state.update { it.copy(isLoading = true) }

        val breakdownFlow = expenseRepository.getBreakdownByCategoryBetween(since, until, type)
        val incomeTotalFlow = expenseRepository.getTotalAmountByTypeBetween(since, until, "INCOME")
        val expenseTotalFlow =
            expenseRepository.getTotalAmountByTypeBetween(since, until, "EXPENSE")

        val categoryDetailsFlow = if (currentState.selectedCategory != null) {
            combine(
                expenseRepository.getFilteredExpenses(
                    categoryIds = listOf(currentState.selectedCategory.categoryId),
                    since = since,
                    until = until,
                    types = listOf(type)
                ),
                expenseRepository.getMonthlyBreakdownByCategory(
                    currentState.selectedCategory.categoryId,
                    type
                )
            ) { txs, trend ->
                Pair(txs.filter { it.type == type }, trend)
            }
        } else {
            flowOf(Pair(emptyList<Expense>(), emptyList<DaySpent>()))
        }

        dataJob = combine(
            breakdownFlow,
            incomeTotalFlow,
            expenseTotalFlow,
            categoryDetailsFlow,
            currencyDao.getAllRates()
        ) { breakdown, income, expense, details, rates ->
            val ratesMap = rates.associate { it.code to it.rateToIdr }
            val baseCurrency = localeManager.getCurrency()
            val baseRate = if (baseCurrency == "IDR") 1.0 else ratesMap[baseCurrency] ?: 1.0

            fun convertFromIdr(amount: Long): Long {
                if (baseRate == 0.0) return amount
                return (amount / baseRate).toLong()
            }

            val incomeConverted = convertFromIdr(income ?: 0L)
            val expenseConverted = convertFromIdr(expense ?: 0L)
            val cashFlow = incomeConverted - expenseConverted
            val savingsRate = if (incomeConverted > 0) {
                (incomeConverted - expenseConverted).toDouble() / incomeConverted.toDouble()
            } else if (expenseConverted > 0) {
                -1.0
            } else {
                0.0
            }

            _state.update {
                it.copy(
                    breakdown = breakdown.map { b -> b.copy(totalAmount = convertFromIdr(b.totalAmount)) },
                    totalIncomeForPeriod = incomeConverted,
                    totalExpenseForPeriod = expenseConverted,
                    cashFlow = cashFlow,
                    savingsRate = savingsRate,
                    categoryTransactions = details.first,
                    categoryTrend = details.second.map { d -> d.copy(amount = convertFromIdr(d.amount)) },
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun getRange(state: TransactionStatsState): Pair<Long, Long> {
        val cal = state.currentPeriodDate.clone() as Calendar
        return when (state.selectedPeriodType) {
            TransactionStatsPeriodType.WEEKLY -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val since = cal.timeInMillis
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                val until = cal.timeInMillis
                Pair(since, until)
            }

            TransactionStatsPeriodType.MONTHLY -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val since = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                val until = cal.timeInMillis
                Pair(since, until)
            }

            TransactionStatsPeriodType.ANNUALLY -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val since = cal.timeInMillis
                cal.add(Calendar.YEAR, 1)
                val until = cal.timeInMillis
                Pair(since, until)
            }

            TransactionStatsPeriodType.CUSTOM -> {
                Pair(state.customStartDate ?: 0L, state.customEndDate ?: Long.MAX_VALUE)
            }
        }
    }
}
