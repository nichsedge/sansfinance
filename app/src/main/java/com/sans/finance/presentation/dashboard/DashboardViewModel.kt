package com.sans.finance.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.BudgetRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.sans.finance.domain.repository.PortfolioRepository
import com.sans.finance.data.local.dao.SnapshotTotal
import java.util.Calendar
import javax.inject.Inject
import com.sans.finance.data.local.entity.PortfolioHoldingEntity

data class DashboardState(
    val netWorth: Long = 0L,
    val totalAssets: Long = 0L,
    val totalLiabilities: Long = 0L,

    val upcomingBills: List<com.sans.finance.domain.model.Expense> = emptyList(),
    val goals: List<com.sans.finance.data.local.entity.GoalEntity> = emptyList(),
    val projectedBalance30Days: Long = 0L,
    val wealthDistribution: Map<String, Long> = emptyMap(),
    val aiSuggestions: List<String> = emptyList(),
    val isLoading: Boolean = true,
    // Monthly cash flow
    val monthlyIncome: Long = 0L,
    val monthlyExpense: Long = 0L,
    val monthlySavingsRate: Float = 0f,
    // Global budget
    val globalBudget: Long = 0L,
    val globalSpent: Long = 0L,
    val currentCurrency: String = "USD",
    val last30DaysTrend: List<Long> = emptyList(),
    val daysLeftInMonth: Int = 0,
    val isPrivacyModeEnabled: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val expenseRepository: ExpenseRepository,
    private val goalRepository: GoalRepository,
    private val budgetRepository: BudgetRepository,
    private val portfolioRepository: PortfolioRepository,
    private val localeManager: com.sans.finance.data.util.LocaleManager
) : ViewModel() {

    val state = combine(
        accountRepository.getAllAccounts(),
        expenseRepository.getExpensesBetween(0, Long.MAX_VALUE),
        expenseRepository.getRecurringExpenses(),
        goalRepository.getAllGoals(),
        budgetRepository.getAllBudgets(),
        portfolioRepository.getTotalValueOverTime(),
        portfolioRepository.getLatestSnapshot(),
        localeManager.privacyMode
    ) { flows: Array<Any?> ->
        val accounts = flows[0] as List<com.sans.finance.data.local.entity.AccountEntity>
        val transactions = flows[1] as List<com.sans.finance.domain.model.Expense>
        val recurring = flows[2] as List<com.sans.finance.domain.model.Expense>
        val goals = flows[3] as List<com.sans.finance.data.local.entity.GoalEntity>
        val budgets = flows[4] as List<com.sans.finance.data.local.entity.BudgetEntity>
        val portfolioHistory = flows[5] as List<SnapshotTotal>
        val latestHoldings = flows[6] as List<PortfolioHoldingEntity>
        val privacyMode = flows[7] as Boolean

        val latestPortfolioIdr = portfolioHistory.lastOrNull()?.totalIdr ?: 0.0
        val portfolioAssets = (latestPortfolioIdr * 100).toLong()

        val accountAssets =
            accounts.filter { it.type != "Credit Card" && it.type != "Loan" }.sumOf { it.balance }
        val assets = accountAssets + portfolioAssets
        val liabilities =
            accounts.filter { it.type == "Credit Card" || it.type == "Loan" }.sumOf { it.balance }

        val recurringNet = recurring.sumOf {
            if (it.type == "INCOME") it.amount else -it.amount
        }

        val accountDistribution = accounts.groupBy { it.type }
            .mapValues { entry -> entry.value.sumOf { it.balance } }
            
        val portfolioDistribution = latestHoldings.groupBy { it.category }
            .mapValues { entry -> (entry.value.sumOf { it.valueIdr } * 100).toLong() }
            
        val distribution = accountDistribution.toMutableMap()
        portfolioDistribution.forEach { (cat, value) ->
            distribution[cat] = (distribution[cat] ?: 0L) + value
        }

        // Compute this-month cash flow
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
        val savingsRate =
            if (monthlyIncome > 0) ((monthlyIncome - monthlyExpense).toFloat() / monthlyIncome.toFloat()).coerceIn(
                0f,
                1f
            ) else 0f

        val suggestions = mutableListOf<String>()
        if (recurringNet < 0) suggestions.add("Your recurring expenses exceed your recurring income. Consider reviewing subscriptions.")
        if (liabilities > assets * 0.5) suggestions.add("Your debt-to-asset ratio is high. Prioritize paying off high-interest debt.")
        if (assets > 0 && goals.isEmpty()) suggestions.add("You have healthy assets but no active goals. Why not set a new savings target?")
        if (monthlyIncome > 0 && savingsRate < 0.1f) suggestions.add("You're saving less than 10% of your income this month. Try to reduce discretionary spending.")
        if (monthlyExpense > monthlyIncome && monthlyIncome > 0) suggestions.add("⚠️ You're spending more than you earn this month. Review your expenses.")

        // 30-day trend
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
        val trendTxns = transactions.filter { it.date >= thirtyDaysAgo }
        val trend = mutableListOf<Long>()
        var runningNW = assets - liabilities
        trend.add(runningNW)
        for (i in 1..29) {
            val dayStart = now - (i.toLong() * 24 * 60 * 60 * 1000)
            val dayEnd = now - ((i - 1).toLong() * 24 * 60 * 60 * 1000)
            val dayNet = trendTxns.filter { it.date >= dayStart && it.date < dayEnd }
                .sumOf { if (it.type == "INCOME") it.amount else -it.amount }
            runningNW -= dayNet
            trend.add(runningNW)
        }

        val todayCal = CalendarUtils.getInstance()
        val daysLeft = todayCal.getActualMaximum(Calendar.DAY_OF_MONTH) - todayCal.get(Calendar.DAY_OF_MONTH)

        DashboardState(
            netWorth = assets - liabilities,
            totalAssets = assets,
            totalLiabilities = liabilities,
            upcomingBills = recurring.take(3),
            goals = goals.take(2),
            projectedBalance30Days = (assets - liabilities) + recurringNet,
            wealthDistribution = distribution,
            aiSuggestions = suggestions,
            isLoading = false,
            monthlyIncome = monthlyIncome,
            monthlyExpense = monthlyExpense,
            monthlySavingsRate = savingsRate,
            globalBudget = budgets.find { b -> b.categoryId == null }?.amount ?: 0L,
            globalSpent = monthlyExpense,
            currentCurrency = localeManager.getCurrency(),
            last30DaysTrend = trend.reversed(),
            daysLeftInMonth = daysLeft,
            isPrivacyModeEnabled = privacyMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )
    
    fun togglePrivacyMode() {
        localeManager.setPrivacyModeEnabled(!localeManager.isPrivacyModeEnabled())
    }
}
