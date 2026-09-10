package com.sans.finance.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.domain.model.CategoryBudgetProgress
import com.sans.finance.domain.model.DashboardSummary
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.model.Goal
import com.sans.finance.domain.model.UserPreferences
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.GoalRepository
import com.sans.finance.domain.repository.PortfolioRepository
import com.sans.finance.domain.repository.UserPreferencesRepository
import com.sans.finance.domain.usecase.GetDashboardSummaryUseCase
import com.sans.finance.domain.usecase.GetNetWorthTrendUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class DashboardState(
    val netWorth: Long = 0L,
    val totalAssets: Long = 0L,
    val totalLiabilities: Long = 0L,
    val upcomingBills: List<Expense> = emptyList(),
    val goals: List<DashboardGoal> = emptyList(),
    val projectedBalance30Days: Long = 0L,
    val aiSuggestions: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val monthlyIncome: Long = 0L,
    val monthlyExpense: Long = 0L,
    val monthlyCashFlow: Long = 0L,
    val monthlySavingsRate: Float = 0f,
    val globalBudget: Long = 0L,
    val globalSpent: Long = 0L,
    val currentCurrency: String = "USD",
    val last30DaysTrend: List<Long> = emptyList(),
    val daysLeftInMonth: Int = 0,
    val isPrivacyModeEnabled: Boolean = false,
    val recentTransactions: List<Expense> = emptyList(),
    val spendingVelocity: Float = 0f,
    val categoryBudgets: List<CategoryBudgetProgress> = emptyList()
)

@Immutable
data class DashboardGoal(
    val name: String,
    val progress: Float
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val goalRepository: GoalRepository,
    private val portfolioRepository: PortfolioRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase,
    private val getNetWorthTrendUseCase: GetNetWorthTrendUseCase
) : ViewModel() {

    private val summaryFlow = getDashboardSummaryUseCase()
    private val prefsFlow = userPreferencesRepository.userPreferences
    private val trendFlow = getNetWorthTrendUseCase(30)

    val state: StateFlow<DashboardState> = combine(
        summaryFlow,
        prefsFlow,
        trendFlow,
        portfolioRepository.getTotalValueOverTime(),
        expenseRepository.getExpensesBetween(0, Long.MAX_VALUE),
        expenseRepository.getRecurringExpenses(),
        goalRepository.getAllGoals()
    ) { args ->
        val summary = args[0] as DashboardSummary
        val prefs = args[1] as UserPreferences
        @Suppress("UNCHECKED_CAST")
        val trend = args[2] as List<Long>
        @Suppress("UNCHECKED_CAST")
        val portfolioHistory = args[3] as List<SnapshotTotal>
        @Suppress("UNCHECKED_CAST")
        val allExpenses = args[4] as List<Expense>
        @Suppress("UNCHECKED_CAST")
        val recurring = args[5] as List<Expense>
        @Suppress("UNCHECKED_CAST")
        val goals = args[6] as List<Goal>

        val now = System.currentTimeMillis()
        val suggestions = generateAiSuggestions(summary, recurring, goals)

        DashboardState(
            netWorth = summary.netWorth,
            totalAssets = summary.totalAssets,
            totalLiabilities = summary.totalLiabilities,
            upcomingBills = (recurring + allExpenses.filter { it.isInstallmentPayment && it.status == "Pending" && it.date >= now })
                .sortedBy { if (it.isInstallmentPayment) it.date else it.nextDueDate ?: Long.MAX_VALUE }
                .take(3),
            goals = goals.map { goal ->
                val currentAmountIdr = when (goal.targetType) {
                    "TOTAL" -> portfolioHistory.lastOrNull()?.totalIdr ?: 0.0
                    else -> 0.0
                }
                DashboardGoal(name = goal.name, progress = (currentAmountIdr / goal.targetAmount).toFloat().coerceIn(0f, 1f))
            }.take(2),
            projectedBalance30Days = summary.netWorth,
            aiSuggestions = suggestions,
            isLoading = false,
            monthlyIncome = summary.monthlyIncome,
            monthlyExpense = summary.monthlyExpense,
            monthlyCashFlow = summary.monthlyCashFlow,
            monthlySavingsRate = summary.monthlySavingsRate,
            globalBudget = summary.globalBudget,
            globalSpent = summary.globalSpent,
            currentCurrency = summary.currentCurrency,
            last30DaysTrend = trend,
            daysLeftInMonth = summary.daysLeftInMonth,
            isPrivacyModeEnabled = prefs.isPrivacyModeEnabled,
            recentTransactions = allExpenses.filter { it.date <= now }.sortedByDescending { it.date }.take(5),
            spendingVelocity = summary.spendingVelocity,
            categoryBudgets = summary.categoryBudgetProgress
        )
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )

    private fun generateAiSuggestions(summary: DashboardSummary, recurring: List<Expense>, goals: List<Goal>): List<String> {
        val suggestions = mutableListOf<String>()

        val recurringNet = recurring.sumOf { if (it.type == "INCOME") it.amount else -it.amount }
        if (recurringNet < 0) suggestions.add("Your recurring expenses exceed your recurring income. Consider reviewing subscriptions.")
        if (summary.totalAssets > 0 && goals.isEmpty()) suggestions.add("You have healthy assets but no active goals. Why not set a new savings target?")
        if (summary.monthlyIncome > 0 && summary.monthlySavingsRate < 0.1f) suggestions.add("You're saving less than 10% of your income this month. Try to reduce discretionary spending.")
        if (summary.monthlyExpense > summary.monthlyIncome && summary.monthlyIncome > 0) suggestions.add("⚠️ You're spending more than you earn this month. Review your expenses.")

        return suggestions
    }

    fun togglePrivacyMode() {
        viewModelScope.launch {
            userPreferencesRepository.setPrivacyModeEnabled(!state.value.isPrivacyModeEnabled)
        }
    }
}

