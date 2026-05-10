package com.sans.finance.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardState(
    val netWorth: Long = 0L,
    val totalAssets: Long = 0L,
    val totalLiabilities: Long = 0L,
    val recentTransactions: List<com.sans.finance.domain.model.Expense> = emptyList(),
    val upcomingBills: List<com.sans.finance.domain.model.Expense> = emptyList(),
    val goals: List<com.sans.finance.data.local.entity.GoalEntity> = emptyList(),
    val projectedBalance30Days: Long = 0L,
    val wealthDistribution: Map<String, Long> = emptyMap(),
    val aiSuggestions: List<String> = emptyList(),
    val isLoading: Boolean = true,
    // Monthly cash flow
    val monthlyIncome: Long = 0L,
    val monthlyExpense: Long = 0L,
    val monthlySavingsRate: Float = 0f
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val expenseRepository: ExpenseRepository,
    private val goalRepository: GoalRepository
) : ViewModel() {

    val state = combine(
        accountRepository.getAllAccounts(),
        expenseRepository.getExpensesBetween(0, Long.MAX_VALUE),
        expenseRepository.getRecurringExpenses(),
        goalRepository.getAllGoals()
    ) { accounts, transactions, recurring, goals ->
        val assets = accounts.filter { it.type != "Credit Card" && it.type != "Loan" }.sumOf { it.balance }
        val liabilities = accounts.filter { it.type == "Credit Card" || it.type == "Loan" }.sumOf { it.balance }
        
        val recurringNet = recurring.sumOf { 
            if (it.type == "INCOME") it.amount else -it.amount
        }
        
        val distribution = accounts.groupBy { it.type }
            .mapValues { entry -> entry.value.sumOf { it.balance } }

        // Compute this-month cash flow
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val monthStart = cal.timeInMillis
        val monthlyTxns = transactions.filter { it.date >= monthStart }
        val monthlyIncome = monthlyTxns.filter { it.type == "INCOME" }.sumOf { it.amount }
        val monthlyExpense = monthlyTxns.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val savingsRate = if (monthlyIncome > 0) ((monthlyIncome - monthlyExpense).toFloat() / monthlyIncome.toFloat()).coerceIn(0f, 1f) else 0f

        val suggestions = mutableListOf<String>()
        if (recurringNet < 0) suggestions.add("Your recurring expenses exceed your recurring income. Consider reviewing subscriptions.")
        if (liabilities > assets * 0.5) suggestions.add("Your debt-to-asset ratio is high. Prioritize paying off high-interest debt.")
        if (assets > 0 && goals.isEmpty()) suggestions.add("You have healthy assets but no active goals. Why not set a new savings target?")
        if (monthlyIncome > 0 && savingsRate < 0.1f) suggestions.add("You're saving less than 10% of your income this month. Try to reduce discretionary spending.")
        if (monthlyExpense > monthlyIncome && monthlyIncome > 0) suggestions.add("⚠️ You're spending more than you earn this month. Review your expenses.")
        
        DashboardState(
            netWorth = assets - liabilities,
            totalAssets = assets,
            totalLiabilities = liabilities,
            recentTransactions = transactions.take(5),
            upcomingBills = recurring.take(3),
            goals = goals.take(2),
            projectedBalance30Days = (assets - liabilities) + recurringNet,
            wealthDistribution = distribution,
            aiSuggestions = suggestions,
            isLoading = false,
            monthlyIncome = monthlyIncome,
            monthlyExpense = monthlyExpense,
            monthlySavingsRate = savingsRate
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )
}
