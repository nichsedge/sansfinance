package com.sans.finance.presentation.transaction_stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.domain.model.CategorySpent
import com.sans.finance.domain.model.DaySpent
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.Calendar
import javax.inject.Inject

enum class TransactionStatsPeriodType {
    WEEKLY, MONTHLY, ANNUALLY, CUSTOM
}

enum class TransactionType {
    INCOME, EXPENSE
}

enum class TrendTimeScope {
    IN_PERIOD, LAST_6_MONTHS, LAST_12_MONTHS, ALL_TIME
}

data class TransactionStatsState(
    val selectedPeriodType: TransactionStatsPeriodType = TransactionStatsPeriodType.MONTHLY,
    val selectedTransactionType: TransactionType = TransactionType.EXPENSE,
    val selectedTrendTimeScope: TrendTimeScope = TrendTimeScope.IN_PERIOD,
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
    val accountsMap: Map<Long, String> = emptyMap(),
    val isLoading: Boolean = false,
    val currentCurrency: String = "USD"
)

@HiltViewModel
class TransactionStatsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val accountRepository: AccountRepository,
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
        _state.update { it.copy(selectedCategory = category, selectedTrendTimeScope = TrendTimeScope.IN_PERIOD) }
        loadData()
    }

    fun onTrendTimeScopeSelected(scope: TrendTimeScope) {
        _state.update { it.copy(selectedTrendTimeScope = scope) }
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
        val expenseTotalFlow = expenseRepository.getTotalAmountByTypeBetween(since, until, "EXPENSE")
        val accountsFlow = accountRepository.getAllAccounts()

        val categoryDetailsFlow = if (currentState.selectedCategory != null) {
            val categoryId = currentState.selectedCategory.categoryId
            val trendFlow = when (currentState.selectedTrendTimeScope) {
                TrendTimeScope.IN_PERIOD -> {
                    if (currentState.selectedPeriodType == TransactionStatsPeriodType.ANNUALLY) {
                        expenseRepository.getMonthlyBreakdownByCategoryBetween(since, until, categoryId, type)
                            .map { raw -> zeroFillMonthly(raw, since, until) }
                    } else {
                        expenseRepository.getDailyBreakdownByCategoryBetween(since, until, categoryId, type)
                            .map { raw -> zeroFillDaily(raw, since, until) }
                    }
                }
                TrendTimeScope.LAST_6_MONTHS -> {
                    val sixMonthsCal = (currentState.currentPeriodDate.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        add(Calendar.MONTH, -5)
                    }
                    val start6m = sixMonthsCal.timeInMillis
                    expenseRepository.getMonthlyBreakdownByCategoryBetween(start6m, until, categoryId, type)
                        .map { raw -> zeroFillMonthly(raw, start6m, until) }
                }
                TrendTimeScope.LAST_12_MONTHS -> {
                    val twelveMonthsCal = (currentState.currentPeriodDate.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        add(Calendar.MONTH, -11)
                    }
                    val start12m = twelveMonthsCal.timeInMillis
                    expenseRepository.getMonthlyBreakdownByCategoryBetween(start12m, until, categoryId, type)
                        .map { raw -> zeroFillMonthly(raw, start12m, until) }
                }
                TrendTimeScope.ALL_TIME -> {
                    val now = System.currentTimeMillis()
                    expenseRepository.getMonthlyBreakdownByCategory(categoryId, type)
                        .map { raw ->
                            if (raw.isEmpty()) emptyList()
                            else {
                                val firstMonth = raw.first().day
                                zeroFillMonthly(raw, firstMonth, now)
                            }
                        }
                }
            }

            combine(
                expenseRepository.getFilteredExpenses(
                    categoryIds = listOf(categoryId),
                    since = since,
                    until = until,
                    types = listOf(type)
                ),
                trendFlow
            ) { txs, trend ->
                Pair(txs.filter { it.type == type }, trend)
            }
        } else {
            flowOf(Pair(emptyList<Expense>(), emptyList<DaySpent>()))
        }

        val totalsFlow = combine(incomeTotalFlow, expenseTotalFlow) { income, expense ->
            Pair(income ?: 0L, expense ?: 0L)
        }

        dataJob = combine(
            breakdownFlow,
            totalsFlow,
            categoryDetailsFlow,
            currencyDao.getAllRates(),
            accountsFlow
        ) { breakdown, totals, details, rates, accounts ->
            val ratesMap = rates.associate { it.code to it.rateToIdr }
            val baseCurrency = localeManager.getCurrency()
            val baseRate = if (baseCurrency == "IDR") 1.0 else ratesMap[baseCurrency] ?: 1.0

            fun convertFromIdr(amount: Long): Long {
                if (baseRate == 0.0) return amount
                return (amount / baseRate).toLong()
            }

            val incomeConverted = convertFromIdr(totals.first)
            val expenseConverted = convertFromIdr(totals.second)
            val cashFlow = incomeConverted - expenseConverted
            val savingsRate = if (incomeConverted > 0) {
                (incomeConverted - expenseConverted).toDouble() / incomeConverted.toDouble()
            } else if (expenseConverted > 0) {
                -1.0
            } else {
                0.0
            }

            val accountsMap = accounts.associate { it.id to it.name }

            _state.update {
                it.copy(
                    breakdown = breakdown.map { b -> b.copy(totalAmount = convertFromIdr(b.totalAmount)) },
                    totalIncomeForPeriod = incomeConverted,
                    totalExpenseForPeriod = expenseConverted,
                    cashFlow = cashFlow,
                    savingsRate = savingsRate,
                    categoryTransactions = details.first,
                    categoryTrend = details.second.map { d -> d.copy(amount = convertFromIdr(d.amount)) },
                    accountsMap = accountsMap,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)

    }

    private fun zeroFillDaily(data: List<DaySpent>, since: Long, until: Long): List<DaySpent> {
        val map = data.associateBy {
            val cal = CalendarUtils.getInstance().apply {
                timeInMillis = it.day
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        }
        val result = mutableListOf<DaySpent>()
        val current = CalendarUtils.getInstance().apply {
            timeInMillis = since
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = CalendarUtils.getInstance().apply {
            timeInMillis = until
        }
        while (current.timeInMillis < endCal.timeInMillis) {
            val dayMs = current.timeInMillis
            result.add(DaySpent(day = dayMs, amount = map[dayMs]?.amount ?: 0L))
            current.add(Calendar.DAY_OF_MONTH, 1)
        }
        return if (result.isEmpty() && data.isNotEmpty()) data else result
    }

    private fun zeroFillMonthly(data: List<DaySpent>, since: Long, until: Long): List<DaySpent> {
        val map = data.associateBy {
            val cal = CalendarUtils.getInstance().apply {
                timeInMillis = it.day
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        }
        val result = mutableListOf<DaySpent>()
        val current = CalendarUtils.getInstance().apply {
            timeInMillis = since
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = CalendarUtils.getInstance().apply {
            timeInMillis = until
        }
        while (current.timeInMillis < endCal.timeInMillis) {
            val monthMs = current.timeInMillis
            result.add(DaySpent(day = monthMs, amount = map[monthMs]?.amount ?: 0L))
            current.add(Calendar.MONTH, 1)
        }
        return if (result.isEmpty() && data.isNotEmpty()) data else result
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

