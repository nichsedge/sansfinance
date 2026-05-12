package com.sans.finance.presentation.forecasting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.PortfolioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.pow

data class ForecastingState(
    val currentNetWorth: Long = 0L,
    val monthlySavings: Long = 0L,
    val expectedRoi: Float = 0.07f, // 7% default
    val projectionYears: Int = 20,
    val projections: List<ProjectionPoint> = emptyList(),
    val isLoading: Boolean = true,
    val currentCurrency: String = "USD"
)

data class ProjectionPoint(
    val year: Int,
    val value: Long
)

@HiltViewModel
class WealthForecastingViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val portfolioRepository: PortfolioRepository,
    private val localeManager: com.sans.finance.data.util.LocaleManager
) : ViewModel() {

    private val _expectedRoi = MutableStateFlow(0.07f)
    private val _projectionYears = MutableStateFlow(25) // Show up to 25 years

    val state = combine(
        portfolioRepository.getTotalValueOverTime(),
        expenseRepository.getExpensesBetween(0, Long.MAX_VALUE),
        _expectedRoi,
        _projectionYears
    ) { portfolioHistory, transactions, roi, years ->
        val latestPortfolioIdr = portfolioHistory.lastOrNull()?.totalIdr ?: 0.0
        val currentNetWorth = (latestPortfolioIdr * 100).toLong()

        val cal = CalendarUtils.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val monthStart = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val nextMonthStart = cal.timeInMillis

        val monthlyTxns = transactions.filter {
            it.date >= monthStart && it.date < nextMonthStart && (!it.isInstallment || it.isInstallmentPayment)
        }
        val monthlyIncome = monthlyTxns.filter { it.type == "INCOME" }.sumOf { it.amount }
        val monthlyExpense = monthlyTxns.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val monthlySavings = (monthlyIncome - monthlyExpense).coerceAtLeast(0)

        val projections = calculateProjections(currentNetWorth, monthlySavings, roi, years)

        ForecastingState(
            currentNetWorth = currentNetWorth,
            monthlySavings = monthlySavings,
            expectedRoi = roi,
            projectionYears = years,
            projections = projections,
            isLoading = false,
            currentCurrency = localeManager.getCurrency()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ForecastingState()
    )

    fun updateRoi(roi: Float) {
        _expectedRoi.value = roi
    }

    private fun calculateProjections(
        initialValue: Long,
        monthlyContribution: Long,
        annualRoi: Float,
        years: Int
    ): List<ProjectionPoint> {
        val points = mutableListOf<ProjectionPoint>()
        val monthlyRoi = (1.0 + annualRoi).pow(1.0 / 12.0) - 1.0

        points.add(ProjectionPoint(0, initialValue))

        var currentValue = initialValue.toDouble()
        for (year in 1..years) {
            for (month in 1..12) {
                currentValue = (currentValue + monthlyContribution) * (1.0 + monthlyRoi)
            }
            points.add(ProjectionPoint(year, currentValue.toLong()))
        }
        return points
    }
}
