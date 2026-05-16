package com.sans.finance.presentation.forecasting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.data.local.dao.CurrencyDao
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
import kotlin.math.roundToLong

data class ForecastingState(
    val currentNetWorth: Long = 0L,
    val monthlySavings: Long = 0L,
    val monthlyExpenses: Long = 0L,
    val expectedRoi: Float = 0.07f, // 7% default
    val projectionYears: Int = 20,
    val projections: List<ProjectionPoint> = emptyList(),
    val isLoading: Boolean = true,
    val currentCurrency: String = "USD",
    val fireNumber: Long = 0L,
    val yearsToFire: Int? = null,
    val emergencyFundTarget: Long = 0L,
    val emergencyFundMonths: Int = 6,
    val currentEmergencyFund: Long = 0L
)

data class ProjectionPoint(
    val year: Int,
    val value: Long
)

private data class ForecastingData(
    val portfolioHistory: List<com.sans.finance.data.local.dao.SnapshotTotal>,
    val transactions: List<com.sans.finance.domain.model.Expense>,
    val accounts: List<com.sans.finance.data.local.entity.AccountEntity>,
    val accountTypes: List<com.sans.finance.data.local.entity.AccountTypeEntity>,
    val rates: List<com.sans.finance.data.local.entity.ExchangeRateEntity>
)

@HiltViewModel
class WealthForecastingViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val portfolioRepository: PortfolioRepository,
    private val accountRepository: com.sans.finance.domain.repository.AccountRepository,
    private val accountTypeRepository: com.sans.finance.domain.repository.AccountTypeRepository,
    private val currencyDao: CurrencyDao,
    private val localeManager: com.sans.finance.data.util.LocaleManager
) : ViewModel() {

    private val _expectedRoi = MutableStateFlow(0.07f)
    private val _projectionYears = MutableStateFlow(25) // Show up to 25 years
    private val _emergencyFundMonths = MutableStateFlow(6)

    val state = combine(
        combine(
            portfolioRepository.getTotalValueOverTime(),
            expenseRepository.getExpensesBetween(0, Long.MAX_VALUE),
            accountRepository.getAllAccounts(),
            accountTypeRepository.getAllAccountTypes(),
            currencyDao.getAllRates()
        ) { history, txns, accs, types, rates ->
            ForecastingData(history, txns, accs, types, rates)
        },
        _expectedRoi,
        _projectionYears,
        _emergencyFundMonths
    ) { data, roi, years, efMonths ->
        val history = data.portfolioHistory
        val txns = data.transactions
        val accs = data.accounts
        val liabilityTypeNames = data.accountTypes.filter { it.isLiability }.map { it.name }.toSet()
        val ratesMap = data.rates.associate { it.code to it.rateToIdr }
        val includedAccountCashIdr = accs
            .filter { it.type !in liabilityTypeNames && it.type != "Investment" }
            .sumOf { account ->
                val amount = account.balance / 100.0
                val rateToIdr = if (account.currency == "IDR") 1.0 else (ratesMap[account.currency] ?: 1.0)
                amount * rateToIdr
            }

        val latestPortfolioIdr = history.lastOrNull()?.totalIdr ?: 0.0
        val currentNetWorth = ((latestPortfolioIdr + includedAccountCashIdr) * 100).roundToLong()

        val cal = CalendarUtils.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val monthStart = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val nextMonthStart = cal.timeInMillis

        // Calculate average monthly expenses over last 3 months
        val threeMonthsAgo = CalendarUtils.getInstance().apply { add(Calendar.MONTH, -3) }.timeInMillis
        val recentTransactions = txns.filter { it.date >= threeMonthsAgo }
        val totalRecentExpenses = recentTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val avgMonthlyExpense = if (totalRecentExpenses > 0) totalRecentExpenses / 3 else 0L

        val monthlyTxns = txns.filter {
            it.date >= monthStart && it.date < nextMonthStart && (!it.isInstallment || it.isInstallmentPayment)
        }
        val monthlyIncome = monthlyTxns.filter { it.type == "INCOME" }.sumOf { it.amount }
        val monthlyExpense = monthlyTxns.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val monthlySavings = (monthlyIncome - monthlyExpense).coerceAtLeast(0)

        val projections = calculateProjections(currentNetWorth, monthlySavings, roi, years)

        val fireNumber = avgMonthlyExpense * 12 * 25
        val yearsToFire = projections.find { it.value >= fireNumber }?.year

        val emergencyFundTarget = avgMonthlyExpense * efMonths
        val currentEmergencyFund = (includedAccountCashIdr * 100).roundToLong()

        ForecastingState(
            currentNetWorth = currentNetWorth,
            monthlySavings = monthlySavings,
            monthlyExpenses = avgMonthlyExpense,
            expectedRoi = roi,
            projectionYears = years,
            projections = projections,
            isLoading = false,
            currentCurrency = localeManager.getCurrency(),
            fireNumber = fireNumber,
            yearsToFire = yearsToFire,
            emergencyFundTarget = emergencyFundTarget,
            emergencyFundMonths = efMonths,
            currentEmergencyFund = currentEmergencyFund
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
