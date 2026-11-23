package com.sans.finance.presentation.dashboard

import app.cash.turbine.test
import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.domain.model.CategoryBudgetProgress
import com.sans.finance.domain.model.DashboardSummary
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.model.Goal
import com.sans.finance.domain.model.UserPreferences
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.GoalRepository
import com.sans.finance.domain.repository.InstallmentRepository
import com.sans.finance.domain.repository.PortfolioRepository
import com.sans.finance.domain.repository.UserPreferencesRepository
import com.sans.finance.domain.usecase.AddTransactionUseCase
import com.sans.finance.domain.usecase.GetDashboardSummaryUseCase
import com.sans.finance.domain.usecase.GetNetWorthTrendUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var goalRepository: GoalRepository
    private lateinit var portfolioRepository: PortfolioRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var installmentRepository: InstallmentRepository
    private lateinit var addTransactionUseCase: AddTransactionUseCase
    private lateinit var getDashboardSummaryUseCase: GetDashboardSummaryUseCase
    private lateinit var getNetWorthTrendUseCase: GetNetWorthTrendUseCase

    private val prefsFlow = MutableStateFlow(UserPreferences(isPrivacyModeEnabled = false))
    private val summaryFlow = MutableStateFlow(
        DashboardSummary(
            netWorth = 120_000_000L,
            totalAssets = 130_000_000L,
            totalLiabilities = 10_000_000L,
            monthlyIncome = 20_000_000L,
            monthlyExpense = 12_000_000L,
            monthlyCashFlow = 8_000_000L,
            monthlySavingsRate = 0.40f,
            globalBudget = 15_000_000L,
            globalSpent = 12_000_000L,
            currentCurrency = "IDR",
            daysLeftInMonth = 10,
            spendingVelocity = 0.85f,
            categoryBudgetProgress = listOf(
                CategoryBudgetProgress(
                    categoryId = 1,
                    categoryName = "Food",
                    categoryIcon = "🍔",
                    budgetAmount = 5_000_000L,
                    spentAmount = 3_500_000L,
                    progress = 0.70f
                )
            )
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        expenseRepository = mockk(relaxed = true)
        every { expenseRepository.getExpensesBetween(any(), any()) } returns flowOf(
            listOf(
                Expense(id = 1, title = "Coffee", amount = 35_000L, date = 1700000000000L, categoryId = 1, type = "EXPENSE"),
                Expense(id = 2, title = "Salary", amount = 20_000_000L, date = 1700000000000L, categoryId = 2, type = "INCOME")
            )
        )
        every { expenseRepository.getRecurringExpenses() } returns flowOf(
            listOf(
                Expense(id = 3, title = "WiFi", amount = 450_000L, date = 1700000000000L, isRecurring = true, nextDueDate = 1750000000000L, categoryId = 3, type = "EXPENSE")
            )
        )

        goalRepository = mockk(relaxed = true)
        every { goalRepository.getAllGoals() } returns flowOf(
            listOf(Goal(id = 1, name = "Trip", targetAmount = 50_000_000L, targetType = "TOTAL", targetName = null, currency = "IDR"))
        )

        portfolioRepository = mockk(relaxed = true)
        every { portfolioRepository.getTotalValueOverTime() } returns flowOf(
            listOf(SnapshotTotal(snapshot_date = 1700000000000L, totalIdr = 25_000_000.0, totalUsd = 1600.0))
        )

        userPreferencesRepository = mockk(relaxed = true)
        every { userPreferencesRepository.userPreferences } returns prefsFlow

        installmentRepository = mockk(relaxed = true)
        addTransactionUseCase = mockk(relaxed = true)

        getDashboardSummaryUseCase = mockk()
        every { getDashboardSummaryUseCase.invoke() } returns summaryFlow

        getNetWorthTrendUseCase = mockk()
        every { getNetWorthTrendUseCase.invoke(30) } returns flowOf(listOf(100_000_000L, 120_000_000L))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = DashboardViewModel(
        expenseRepository = expenseRepository,
        goalRepository = goalRepository,
        portfolioRepository = portfolioRepository,
        userPreferencesRepository = userPreferencesRepository,
        installmentRepository = installmentRepository,
        addTransactionUseCase = addTransactionUseCase,
        getDashboardSummaryUseCase = getDashboardSummaryUseCase,
        getNetWorthTrendUseCase = getNetWorthTrendUseCase
    )

    @Test
    fun `initial state loads operational summary, cash flow, budget, bills, and recent transactions`() = runTest {
        val viewModel = createViewModel()
        viewModel.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()

            assertEquals(120_000_000L, state.netWorth)
            assertEquals(130_000_000L, state.totalAssets)
            assertEquals(10_000_000L, state.totalLiabilities)
            assertEquals(20_000_000L, state.monthlyIncome)
            assertEquals(12_000_000L, state.monthlyExpense)
            assertEquals(8_000_000L, state.monthlyCashFlow)
            assertEquals(0.40f, state.monthlySavingsRate)
            assertEquals(15_000_000L, state.globalBudget)
            assertEquals(1, state.upcomingBills.size)
            assertEquals(2, state.recentTransactions.size)
            assertEquals(1, state.categoryBudgets.size)
            assertFalse(state.isPrivacyModeEnabled)
        }
    }

    @Test
    fun `togglePrivacyMode toggles privacy mode preference in repository`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.togglePrivacyMode()
        advanceUntilIdle()

        coVerify { userPreferencesRepository.setPrivacyModeEnabled(true) }
    }
}
