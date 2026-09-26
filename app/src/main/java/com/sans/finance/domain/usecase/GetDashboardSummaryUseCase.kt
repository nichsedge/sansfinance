package com.sans.finance.domain.usecase

import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.model.DashboardSummary
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.AccountTypeRepository
import com.sans.finance.domain.repository.BudgetRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.PortfolioRepository
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.data.local.entity.AccountTypeEntity
import com.sans.finance.data.local.entity.ExchangeRateEntity
import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

class GetDashboardSummaryUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val portfolioRepository: PortfolioRepository,
    private val accountRepository: AccountRepository,
    private val accountTypeRepository: AccountTypeRepository,
    private val budgetRepository: BudgetRepository,
    private val localeManager: LocaleManager,
    private val currencyDao: CurrencyDao
) {
    operator fun invoke(): Flow<DashboardSummary> {
        val baseCurrency = localeManager.getCurrency()

        val cal = CalendarUtils.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val monthStart = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val nextMonthStart = cal.timeInMillis

        val todayCal = CalendarUtils.getInstance()
        val daysLeft = todayCal.getActualMaximum(Calendar.DAY_OF_MONTH) - todayCal.get(Calendar.DAY_OF_MONTH)

        val financeFlow = combine(
            accountRepository.getAllAccounts(),
            accountTypeRepository.getAllAccountTypes(),
            portfolioRepository.getTotalValueOverTime(),
            currencyDao.getAllRates()
        ) { accounts, types, history, rates ->
            FinanceData(accounts, types, history, rates)
        }

        val monthlyFlow = combine(
            expenseRepository.getTotalAmountByTypeBetween(monthStart, nextMonthStart, "INCOME"),
            expenseRepository.getTotalAmountByTypeBetween(monthStart, nextMonthStart, "EXPENSE"),
            budgetRepository.getAllBudgets(),
            expenseRepository.getSpendingByCategoryBetween(monthStart, nextMonthStart)
        ) { incomeIdr, expenseIdr, budgets, categorySpents ->
            MonthlyData(incomeIdr, expenseIdr, budgets, categorySpents)
        }

        return combine(financeFlow, monthlyFlow) { finance, monthly ->
            val ratesMap = finance.rates.associate { it.code to it.rateToIdr }
            val baseRate = if (baseCurrency == "IDR") 1.0 else ratesMap[baseCurrency] ?: 1.0

            fun convertToBase(amount: Long, from: String): Long {
                if (from == baseCurrency) return amount
                val fromRate = if (from == "IDR") 1.0 else ratesMap[from] ?: 1.0
                val toRate = baseRate
                if (toRate == 0.0) return amount
                return ((amount * fromRate) / toRate).toLong()
            }

            // Assets & Liabilities
            val latestPortfolioIdr = finance.history.lastOrNull()?.totalIdr ?: 0.0
            val portfolioAssets = if (baseRate > 0) ((latestPortfolioIdr / baseRate) * 100).toLong() else 0L

            val liabilityTypeNames = finance.types.filter { it.isLiability }.map { it.name }.toSet()
            val investmentTypeNames = finance.types.filter { it.isInvestment }.map { it.name }.toSet()
            val accountAssets = finance.accounts
                .filter { it.type !in liabilityTypeNames && it.type !in investmentTypeNames }
                .sumOf { convertToBase(it.balance, it.currency) }
            val investmentAccountAssets = finance.accounts
                .filter { it.type in investmentTypeNames }
                .sumOf { convertToBase(it.balance, it.currency) }
            val accountLiabilities = finance.accounts
                .filter { it.type in liabilityTypeNames }
                .sumOf { convertToBase(it.balance, it.currency) }

            val totalAssets = portfolioAssets + accountAssets + investmentAccountAssets
            val totalLiabilities = accountLiabilities

            // Monthly Stats (converted from IDR aggregation)
            val monthlyIncome = if (baseRate > 0) ((monthly.incomeIdr ?: 0L).toDouble() / baseRate).toLong() else 0L
            val monthlyExpense = if (baseRate > 0) ((monthly.expenseIdr ?: 0L).toDouble() / baseRate).toLong() else 0L
            val monthlyCashFlow = monthlyIncome - monthlyExpense
            val savingsRate = if (monthlyIncome > 0) ((monthlyIncome - monthlyExpense).toFloat() / monthlyIncome.toFloat())
                else if (monthlyExpense > 0) -1f else 0f

            val globalBudget = monthly.budgets.find { it.categoryId == null }?.amount ?: 0L

            // Spending Velocity Calculation
            val totalDays = todayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val currentDay = todayCal.get(Calendar.DAY_OF_MONTH)
            val expectedProgress = currentDay.toFloat() / totalDays.toFloat()
            val actualProgress = if (globalBudget > 0) monthlyExpense.toFloat() / globalBudget.toFloat() else 0f
            val velocity = if (expectedProgress > 0) actualProgress / expectedProgress else 0f

            // Category Budget Progress
            val categoryBudgets = monthly.budgets.filter { it.categoryId != null }.mapNotNull { budget ->
                val spentIdr = monthly.categorySpents.find { it.categoryId == budget.categoryId }?.totalAmount ?: 0L
                val spentInBase = if (baseRate > 0) (spentIdr.toDouble() / baseRate).toLong() else 0L
                val category = monthly.categorySpents.find { it.categoryId == budget.categoryId } // Reuse for icon/name

                com.sans.finance.domain.model.CategoryBudgetProgress(
                    categoryId = budget.categoryId!!,
                    categoryName = category?.categoryName ?: "Category",
                    categoryIcon = category?.categoryIcon,
                    budgetAmount = budget.amount,
                    spentAmount = spentInBase,
                    progress = if (budget.amount > 0) spentInBase.toFloat() / budget.amount.toFloat() else 0f
                )
            }.sortedByDescending { it.spentAmount }

            DashboardSummary(
                netWorth = totalAssets - totalLiabilities,
                totalAssets = totalAssets,
                totalLiabilities = totalLiabilities,
                monthlyIncome = monthlyIncome,
                monthlyExpense = monthlyExpense,
                monthlyCashFlow = monthlyCashFlow,
                monthlySavingsRate = savingsRate,
                globalBudget = globalBudget,
                globalSpent = monthlyExpense,
                currentCurrency = baseCurrency,
                daysLeftInMonth = daysLeft,
                spendingVelocity = velocity,
                categoryBudgetProgress = categoryBudgets
            )
        }
    }

    private data class FinanceData(
        val accounts: List<AccountEntity>,
        val types: List<AccountTypeEntity>,
        val history: List<SnapshotTotal>,
        val rates: List<ExchangeRateEntity>
    )

    private data class MonthlyData(
        val incomeIdr: Long?,
        val expenseIdr: Long?,
        val budgets: List<BudgetEntity>,
        val categorySpents: List<com.sans.finance.domain.model.CategorySpent>
    )
}
