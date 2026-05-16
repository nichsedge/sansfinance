package com.sans.finance.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.BudgetRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.GoalRepository
import com.sans.finance.domain.repository.PortfolioRepository
import com.sans.finance.domain.model.Expense
import com.sans.finance.data.local.entity.BudgetEntity
import com.sans.finance.data.local.entity.GoalEntity
import com.sans.finance.data.local.entity.AccountTypeEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class DashboardState(
    val netWorth: Long = 0L,
    val totalAssets: Long = 0L,
    val totalLiabilities: Long = 0L,

    val upcomingBills: List<Expense> = emptyList(),
    val goals: List<DashboardGoal> = emptyList(),
    val projectedBalance30Days: Long = 0L,
    val wealthDistribution: Map<String, Long> = emptyMap(),
    val aiSuggestions: List<String> = emptyList(),
    val isLoading: Boolean = true,
    // Monthly cash flow
    val monthlyIncome: Long = 0L,
    val monthlyExpense: Long = 0L,
    val monthlyCashFlow: Long = 0L,
    val monthlySavingsRate: Float = 0f,
    // Global budget
    val globalBudget: Long = 0L,
    val globalSpent: Long = 0L,
    val currentCurrency: String = "USD",
    val last30DaysTrend: List<Long> = emptyList(),
    val daysLeftInMonth: Int = 0,
    val isPrivacyModeEnabled: Boolean = false,
    val wealthDistributionTab: WealthDistributionTab = WealthDistributionTab.CATEGORY,
    // Financial Freedom
    val annualExpense: Long = 0L,
    val financialFreedomYears: Double = 0.0,
    val financialFreedomScore: Float = 0f,
    val isFireManualEnabled: Boolean = false,
    val manualFireAnnualExpense: Long = 0L,
    val recentTransactions: List<Expense> = emptyList()
)

enum class WealthDistributionTab {
    CURRENCY, ASSET_CLASS, CATEGORY
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val expenseRepository: ExpenseRepository,
    private val goalRepository: GoalRepository,
    private val budgetRepository: BudgetRepository,
    private val portfolioRepository: PortfolioRepository,
    private val accountTypeRepository: com.sans.finance.domain.repository.AccountTypeRepository,
    private val localeManager: com.sans.finance.data.util.LocaleManager,
    private val currencyDao: com.sans.finance.data.local.dao.CurrencyDao
) : ViewModel() {

    private val _wealthDistributionTab =
        kotlinx.coroutines.flow.MutableStateFlow(WealthDistributionTab.CATEGORY)

    // Intermediate Contexts for better type safety and modularity
    private val financeContext = combine(
        combine(
            accountRepository.getAllAccounts(),
            expenseRepository.getExpensesBetween(0, Long.MAX_VALUE),
            expenseRepository.getRecurringExpenses(),
            budgetRepository.getAllBudgets(),
            goalRepository.getAllGoals()
        ) { accounts, txns, recurring, budgets, goals ->
            FinanceContextPartial(accounts, txns, recurring, budgets, goals, emptyList())
        },
        accountTypeRepository.getAllAccountTypes(),
        currencyDao.getAllRates()
    ) { partial, types, rates ->
        val ratesMap = rates.associate { it.code to it.rateToIdr }
        FinanceContext(
            accounts = partial.accounts,
            transactions = partial.transactions,
            recurring = partial.recurring,
            budgets = partial.budgets,
            goals = partial.goals,
            accountTypes = types,
            rates = ratesMap
        )
    }

    private val portfolioContext = combine(
        portfolioRepository.getTotalValueOverTime(),
        portfolioRepository.getLatestSnapshot()
    ) { history, holdings ->
        PortfolioContext(history, holdings)
    }

    private val settingsContext = combine(
        localeManager.privacyMode,
        _wealthDistributionTab,
        localeManager.fireManualEnabled,
        localeManager.manualFireAnnualExpense
    ) { privacy, tab, fireManual, fireAmount ->
        SettingsContext(privacy, tab, fireManual, fireAmount)
    }


    val state = combine(
        financeContext,
        portfolioContext,
        settingsContext
    ) { finance, portfolio, settings ->
        val transactions = finance.transactions
        val recurring = finance.recurring
        val goals = finance.goals
        val budgets = finance.budgets
        val rates = finance.rates

        val portfolioHistory = portfolio.history
        val latestHoldings = portfolio.holdings

        val privacyMode = settings.isPrivacyModeEnabled
        val selectedTab = settings.wealthDistributionTab
        val isFireManual = settings.isFireManualEnabled
        val manualFireExpense = settings.manualFireAnnualExpense
        val baseCurrency = localeManager.getCurrency()

        fun convertToBase(amount: Long, from: String): Long {
            if (from == baseCurrency) return amount
            val fromRate = if (from == "IDR") 1.0 else rates[from] ?: 1.0
            val toRate = if (baseCurrency == "IDR") 1.0 else rates[baseCurrency] ?: 1.0
            if (toRate == 0.0) return amount
            return ((amount * fromRate) / toRate).toLong()
        }

        val latestPortfolioIdr = portfolioHistory.lastOrNull()?.totalIdr ?: 0.0
        // portfolioAssets in base currency
        val baseRate = if (baseCurrency == "IDR") 1.0 else rates[baseCurrency] ?: 1.0
        val portfolioAssets =
            if (baseRate > 0) ((latestPortfolioIdr / baseRate) * 100).toLong() else (latestPortfolioIdr * 100).toLong()

        val liabilityTypeNames = finance.accountTypes.filter { it.isLiability }.map { it.name }.toSet()

        val accountAssets = finance.accounts
            .filter { it.type !in liabilityTypeNames && it.type != "Investment" }
            .sumOf { convertToBase(it.balance, it.currency) }
        val accountLiabilities = finance.accounts
            .filter { it.type in liabilityTypeNames }
            .sumOf { convertToBase(it.balance, it.currency) }

        val assets = portfolioAssets + accountAssets
        val liabilities = accountLiabilities

        val recurringNet = recurring.sumOf {
            val amt = convertToBase(it.amount, it.currency)
            if (it.type == "INCOME") amt else -amt
        }

        val distribution = when (selectedTab) {
            WealthDistributionTab.CURRENCY -> {
                latestHoldings.groupBy { it.currency }
                    .mapValues { entry ->
                        val idrValue = entry.value.sumOf { it.valueIdr }
                        if (baseRate > 0) ((idrValue / baseRate) * 100).toLong() else (idrValue * 100).toLong()
                    }
            }

            WealthDistributionTab.ASSET_CLASS -> {
                latestHoldings.groupBy { it.assetClass }
                    .mapValues { entry ->
                        val idrValue = entry.value.sumOf { it.valueIdr }
                        if (baseRate > 0) ((idrValue / baseRate) * 100).toLong() else (idrValue * 100).toLong()
                    }
            }

            WealthDistributionTab.CATEGORY -> {
                latestHoldings.groupBy { it.category }
                    .mapValues { entry ->
                        val idrValue = entry.value.sumOf { it.valueIdr }
                        if (baseRate > 0) ((idrValue / baseRate) * 100).toLong() else (idrValue * 100).toLong()
                    }
            }
        }.toList()
            .sortedByDescending { kotlin.math.abs(it.second) }
            .toMap()

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
        val monthlyIncome = monthlyTxns.filter { it.type == "INCOME" }
            .sumOf { convertToBase(it.amount, it.currency) }
        val monthlyExpense = monthlyTxns.filter { it.type == "EXPENSE" }
            .sumOf { convertToBase(it.amount, it.currency) }
        val monthlyCashFlow = monthlyIncome - monthlyExpense
        val savingsRate =
            if (monthlyIncome > 0) ((monthlyIncome - monthlyExpense).toFloat() / monthlyIncome.toFloat())
            else if (monthlyExpense > 0) -1f // 100% negative if there are expenses but no income
            else 0f

        val now = System.currentTimeMillis()
        val yearAgo = now - (365L * 24 * 60 * 60 * 1000)
        val annualExpense = transactions.filter {
            it.date >= yearAgo && it.date <= now && it.type == "EXPENSE" && (!it.isInstallment || it.isInstallmentPayment)
        }.sumOf { convertToBase(it.amount, it.currency) }

        val firstTxnDate =
            transactions.filter { it.type == "EXPENSE" }.minOfOrNull { it.date } ?: now
        val daysOfData = ((now - firstTxnDate) / (24 * 60 * 60 * 1000)).coerceAtLeast(1L)
        val effectiveAnnualExpense = if (isFireManual) {
            manualFireExpense
        } else if (daysOfData < 365) {
            (annualExpense.toDouble() / daysOfData * 365).toLong()
        } else {
            annualExpense
        }

        val freedomYears = if (effectiveAnnualExpense > 0) {
            assets.toDouble() / effectiveAnnualExpense.toDouble()
        } else {
            0.0
        }

        val freedomScore = if (effectiveAnnualExpense > 0) {
            (assets.toDouble() / (effectiveAnnualExpense.toDouble() * 25.0)).toFloat()
                .coerceIn(0f, 1f)
        } else {
            0f
        }

        val suggestions = mutableListOf<String>()
        if (freedomYears > 0 && freedomYears < 1.0) suggestions.add("You have less than a year of financial cover. Focus on building an emergency fund.")
        if (freedomYears >= 25.0) suggestions.add("🌟 Congratulations! You've reached financial independence (25x expenses).")
        else if (freedomYears >= 10.0) suggestions.add("Great progress! You have over a decade of freedom secured.")

        if (recurringNet < 0) suggestions.add("Your recurring expenses exceed your recurring income. Consider reviewing subscriptions.")
        if (assets > 0 && goals.isEmpty()) suggestions.add("You have healthy assets but no active goals. Why not set a new savings target?")
        if (monthlyIncome > 0 && savingsRate < 0.1f) suggestions.add("You're saving less than 10% of your income this month. Try to reduce discretionary spending.")
        if (monthlyExpense > monthlyIncome && monthlyIncome > 0) suggestions.add("⚠️ You're spending more than you earn this month. Review your expenses.")



        if (daysOfData < 30 && annualExpense > 0) {
            suggestions.add("💡 Tracking more expenses will improve the accuracy of your Financial Freedom score.")
        } else if (annualExpense == 0L && assets > 0) {
            suggestions.add("💡 Start logging your daily expenses to see your Financial Freedom progress.")
        }

        val trend = mutableListOf<Long>()
        for (i in 0..29) {
            val dayStart = now - (i.toLong() * 24 * 60 * 60 * 1000)
            val snapshotAtDay = portfolioHistory.filter { it.snapshot_date <= dayStart }
                .maxByOrNull { it.snapshot_date }
            val dayValueIdr = snapshotAtDay?.totalIdr ?: 0.0
            val dayValue =
                if (baseRate > 0) ((dayValueIdr / baseRate) * 100).toLong() else (dayValueIdr * 100).toLong()
            trend.add(dayValue)
        }

        val todayCal = CalendarUtils.getInstance()
        val daysLeft =
            todayCal.getActualMaximum(Calendar.DAY_OF_MONTH) - todayCal.get(Calendar.DAY_OF_MONTH)

        DashboardState(
            netWorth = assets - liabilities,
            totalAssets = assets,
            totalLiabilities = liabilities,
            upcomingBills = (recurring + transactions.filter { it.isInstallmentPayment && it.status == "Pending" && it.date >= now })
                .sortedBy {
                    if (it.isInstallmentPayment) it.date else it.nextDueDate ?: Long.MAX_VALUE
                }
                .take(3),
            goals = goals.map { goal ->
                val currentAmountIdr = when (goal.targetType) {
                    "TOTAL" -> latestPortfolioIdr
                    "CATEGORY" -> latestHoldings.filter { it.category == goal.targetName }
                        .sumOf { it.valueIdr }

                    "ASSET_CLASS" -> latestHoldings.filter { it.assetClass == goal.targetName }
                        .sumOf { it.valueIdr }

                    else -> 0.0
                }
                DashboardGoal(
                    name = goal.name,
                    progress = (currentAmountIdr / goal.targetAmount).toFloat().coerceIn(0f, 1f)
                )
            }.take(2),
            projectedBalance30Days = assets - liabilities,
            wealthDistribution = distribution,
            aiSuggestions = suggestions,
            isLoading = false,
            monthlyIncome = monthlyIncome,
            monthlyExpense = monthlyExpense,
            monthlyCashFlow = monthlyCashFlow,
            monthlySavingsRate = savingsRate,
            globalBudget = budgets.find { b -> b.categoryId == null }?.amount ?: 0L,
            globalSpent = monthlyExpense,
            currentCurrency = localeManager.getCurrency(),
            last30DaysTrend = trend.reversed(),
            daysLeftInMonth = daysLeft,
            isPrivacyModeEnabled = privacyMode,
            wealthDistributionTab = selectedTab,
            annualExpense = effectiveAnnualExpense,
            financialFreedomYears = freedomYears,
            financialFreedomScore = freedomScore,
            isFireManualEnabled = isFireManual,
            manualFireAnnualExpense = manualFireExpense,
            recentTransactions = transactions.filter { it.date <= now }
                .sortedByDescending { it.date }.take(5)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )

    fun togglePrivacyMode() {
        localeManager.setPrivacyModeEnabled(!localeManager.isPrivacyModeEnabled())
    }

    fun setWealthDistributionTab(tab: WealthDistributionTab) {
        _wealthDistributionTab.value = tab
    }

    fun setFireManualEnabled(enabled: Boolean) {
        localeManager.setFireManualEnabled(enabled)
    }

    fun setManualFireAnnualExpense(amount: Long) {
        localeManager.setManualFireAnnualExpense(amount)
    }
}

// Type-safe Context wrappers
private data class FinanceContext(
    val accounts: List<AccountEntity>,
    val transactions: List<Expense>,
    val recurring: List<Expense>,
    val budgets: List<BudgetEntity>,
    val goals: List<GoalEntity>,
    val accountTypes: List<AccountTypeEntity>,
    val rates: Map<String, Double>
)

private data class FinanceContextPartial(
    val accounts: List<AccountEntity>,
    val transactions: List<Expense>,
    val recurring: List<Expense>,
    val budgets: List<BudgetEntity>,
    val goals: List<GoalEntity>,
    val accountTypes: List<AccountTypeEntity>
)

private data class PortfolioContext(
    val history: List<SnapshotTotal>,
    val holdings: List<PortfolioHoldingEntity>
)

private data class SettingsContext(
    val isPrivacyModeEnabled: Boolean,
    val wealthDistributionTab: WealthDistributionTab,
    val isFireManualEnabled: Boolean,
    val manualFireAnnualExpense: Long
)

data class DashboardGoal(
    val name: String,
    val progress: Float
)
