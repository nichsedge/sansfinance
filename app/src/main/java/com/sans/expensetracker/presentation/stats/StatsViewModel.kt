package com.sans.expensetracker.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.expensetracker.domain.repository.ExpenseRepository
import com.sans.expensetracker.domain.repository.InstallmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import com.sans.expensetracker.core.util.CalendarUtils
import java.util.Calendar
import javax.inject.Inject

enum class TrendPeriod {
    DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
}

data class StatsState(
    val thisMonthSpent: Long = 0L,
    val lastMonthSpent: Long = 0L,
    val thisYearSpent: Long = 0L,
    val lastYearSpent: Long = 0L,
    val spendingByCategory: List<com.sans.expensetracker.data.local.entity.CategorySpent> = emptyList(),
    val dailySpending: List<com.sans.expensetracker.data.local.entity.DaySpent> = emptyList(),
    val trendSpending: List<com.sans.expensetracker.data.local.entity.DaySpent> = emptyList(),
    val selectedTrendPeriod: TrendPeriod = TrendPeriod.DAILY,
    val isLoading: Boolean = true
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val installmentRepository: InstallmentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatsState())
    val state: StateFlow<StatsState> = _state.asStateFlow()

    private val _selectedTrendPeriod = MutableStateFlow(TrendPeriod.DAILY)

    init {
        loadStats()
        observeTrendPeriod()
    }

    private fun observeTrendPeriod() {
        _selectedTrendPeriod
            .onEach { period ->
                _state.update { it.copy(selectedTrendPeriod = period) }
                updateTrendData(period)
            }
            .launchIn(viewModelScope)
    }

    fun onTrendPeriodSelected(period: TrendPeriod) {
        _selectedTrendPeriod.value = period
    }

    private fun updateTrendData(period: TrendPeriod) {
        val now = CalendarUtils.getInstance()
        val since = when (period) {
            TrendPeriod.DAILY -> (now.clone() as Calendar).apply {
                add(
                    Calendar.DAY_OF_YEAR,
                    -30
                )
            }.timeInMillis

            TrendPeriod.WEEKLY -> (now.clone() as Calendar).apply {
                add(
                    Calendar.WEEK_OF_YEAR,
                    -12
                ); set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            }.timeInMillis

            TrendPeriod.MONTHLY -> (now.clone() as Calendar).apply {
                add(Calendar.MONTH, -12); set(
                Calendar.DAY_OF_MONTH,
                1
            )
            }.timeInMillis

            TrendPeriod.QUARTERLY -> (now.clone() as Calendar).apply {
                add(
                    Calendar.MONTH,
                    -24
                ); set(Calendar.DAY_OF_MONTH, 1)
            }.timeInMillis // 2 years = 8 quarters
            TrendPeriod.YEARLY -> 0L // All time
        }

        expenseRepository.getDailySpendingBetween(since, now.timeInMillis)
            .map { daily ->
                groupSpendingByPeriod(daily, period)
            }
            .onEach { trend ->
                _state.update { it.copy(trendSpending = trend) }
            }
            .launchIn(viewModelScope)
    }

    private fun groupSpendingByPeriod(
        daily: List<com.sans.expensetracker.data.local.entity.DaySpent>,
        period: TrendPeriod
    ): List<com.sans.expensetracker.data.local.entity.DaySpent> {
        val calendar = CalendarUtils.getInstance()

        // 1. Group by the start of the period
        val groupedByPeriod = daily.groupBy { item ->
            calendar.timeInMillis = item.day
            when (period) {
                TrendPeriod.WEEKLY -> calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                TrendPeriod.MONTHLY -> calendar.set(Calendar.DAY_OF_MONTH, 1)
                TrendPeriod.QUARTERLY -> {
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.MONTH, (calendar.get(Calendar.MONTH) / 3) * 3)
                }

                TrendPeriod.YEARLY -> calendar.set(Calendar.DAY_OF_YEAR, 1)
                TrendPeriod.DAILY -> {}
            }
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }

        // 2. Sum the amounts for each period
        val summedMap = groupedByPeriod.mapValues { entry ->
            entry.value.sumOf { it.amount }
        }

        val result = mutableListOf<com.sans.expensetracker.data.local.entity.DaySpent>()

        // 3. Fill gaps and generate result
        val startCal = CalendarUtils.getInstance().apply {
            val firstItem = daily.minByOrNull { it.day }?.day ?: System.currentTimeMillis()
            timeInMillis = firstItem
            // Normalize start based on period
            when (period) {
                TrendPeriod.WEEKLY -> set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                TrendPeriod.MONTHLY -> set(Calendar.DAY_OF_MONTH, 1)
                TrendPeriod.QUARTERLY -> {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.MONTH, (get(Calendar.MONTH) / 3) * 3)
                }

                TrendPeriod.YEARLY -> set(Calendar.DAY_OF_YEAR, 1)
                TrendPeriod.DAILY -> {}
            }
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCal = CalendarUtils.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        while (startCal.timeInMillis <= endCal.timeInMillis) {
            val periodStart = startCal.timeInMillis
            result.add(
                com.sans.expensetracker.data.local.entity.DaySpent(
                    periodStart,
                    summedMap[periodStart] ?: 0L
                )
            )

            // Increment based on period
            when (period) {
                TrendPeriod.DAILY -> startCal.add(Calendar.DAY_OF_YEAR, 1)
                TrendPeriod.WEEKLY -> startCal.add(Calendar.WEEK_OF_YEAR, 1)
                TrendPeriod.MONTHLY -> startCal.add(Calendar.MONTH, 1)
                TrendPeriod.QUARTERLY -> startCal.add(Calendar.MONTH, 3)
                TrendPeriod.YEARLY -> startCal.add(Calendar.YEAR, 1)
            }
        }

        return result
    }

    private fun loadStats() {
        val now = CalendarUtils.getInstance()

        // This Month
        val thisMonthStart = getStartOfMonth(now)

        // Last Month
        val lastMonthStart = (thisMonthStart.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val lastMonthEnd = thisMonthStart

        // This Year
        val thisYearStart = (thisMonthStart.clone() as Calendar).apply {
            set(
                Calendar.MONTH,
                0
            ); set(Calendar.DAY_OF_YEAR, 1)
        }

        // Last Year
        val lastYearStart = (thisYearStart.clone() as Calendar).apply { add(Calendar.YEAR, -1) }
        val lastYearEnd = thisYearStart

        val thisMonthEnd = (thisMonthStart.clone() as Calendar).apply { add(Calendar.MONTH, 1) }

        val thisMonthExpFlow =
            expenseRepository.getTotalSpentBetween(thisMonthStart.timeInMillis, thisMonthEnd.timeInMillis)
        val thisMonthInstFlow = installmentRepository.getTotalPaidAmountBetween(
            thisMonthStart.timeInMillis,
            thisMonthEnd.timeInMillis
        )

        val lastMonthExpFlow = expenseRepository.getTotalSpentBetween(
            lastMonthStart.timeInMillis,
            lastMonthEnd.timeInMillis
        )
        val lastMonthInstFlow = installmentRepository.getTotalPaidAmountBetween(
            lastMonthStart.timeInMillis,
            lastMonthEnd.timeInMillis
        )

        val thisYearEnd = (thisYearStart.clone() as Calendar).apply { add(Calendar.YEAR, 1) }
        val thisYearExpFlow =
            expenseRepository.getTotalSpentBetween(thisYearStart.timeInMillis, thisYearEnd.timeInMillis)
        val thisYearInstFlow = installmentRepository.getTotalPaidAmountBetween(
            thisYearStart.timeInMillis,
            thisYearEnd.timeInMillis
        )

        val lastYearExpFlow = expenseRepository.getTotalSpentBetween(
            lastYearStart.timeInMillis,
            lastYearEnd.timeInMillis
        )
        val lastYearInstFlow = installmentRepository.getTotalPaidAmountBetween(
            lastYearStart.timeInMillis,
            lastYearEnd.timeInMillis
        )

        val spendingByCategoryFlow = expenseRepository.getSpendingByCategoryBetween(
            thisMonthStart.timeInMillis,
            thisMonthEnd.timeInMillis
        )
        val dailySpendingFlow =
            expenseRepository.getDailySpendingBetween(thisMonthStart.timeInMillis, thisMonthEnd.timeInMillis)

        combine(
            combine(thisMonthExpFlow, thisMonthInstFlow) { e, i -> (e ?: 0L) + (i ?: 0L) },
            combine(lastMonthExpFlow, lastMonthInstFlow) { e, i -> (e ?: 0L) + (i ?: 0L) },
            combine(thisYearExpFlow, thisYearInstFlow) { e, i -> (e ?: 0L) + (i ?: 0L) },
            combine(lastYearExpFlow, lastYearInstFlow) { e, i -> (e ?: 0L) + (i ?: 0L) },
            spendingByCategoryFlow,
            dailySpendingFlow
        ) { values ->
            val tm = values[0] as Long
            val lm = values[1] as Long
            val ty = values[2] as Long
            val ly = values[3] as Long

            @Suppress("UNCHECKED_CAST")
            val sbc = values[4] as List<com.sans.expensetracker.data.local.entity.CategorySpent>

            @Suppress("UNCHECKED_CAST")
            val ds = values[5] as List<com.sans.expensetracker.data.local.entity.DaySpent>

            _state.value.copy(
                thisMonthSpent = tm,
                lastMonthSpent = lm,
                thisYearSpent = ty,
                lastYearSpent = ly,
                spendingByCategory = sbc,
                dailySpending = ds,
                isLoading = false
            )
        }.onEach { newState ->
            _state.value = newState
        }.launchIn(viewModelScope)
    }

    private fun getStartOfMonth(cal: Calendar): Calendar {
        return (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
