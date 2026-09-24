package com.sans.finance.presentation.wealth

import app.cash.turbine.test
import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.data.local.entity.AccountTypeEntity
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.data.local.entity.PortfolioSnapshotHeaderEntity
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.model.EmergencyFundStressTest
import com.sans.finance.domain.model.FinancialFreedomStats
import com.sans.finance.domain.model.Goal
import com.sans.finance.domain.model.MomentumTrend
import com.sans.finance.domain.model.SafetyBufferTier
import com.sans.finance.domain.model.SavingsRateVelocitySummary
import com.sans.finance.domain.model.UserPreferences
import com.sans.finance.domain.model.WealthDistributionTab
import com.sans.finance.domain.model.WealthMetrics
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.AccountTypeRepository
import com.sans.finance.domain.repository.GoalRepository
import com.sans.finance.domain.repository.PortfolioRepository
import com.sans.finance.domain.repository.UserPreferencesRepository
import com.sans.finance.domain.usecase.GetEmergencyFundStressTestUseCase
import com.sans.finance.domain.usecase.GetFinancialFreedomStatsUseCase
import com.sans.finance.domain.usecase.GetSavingsRateVelocityUseCase
import com.sans.finance.domain.usecase.GetWealthDistributionUseCase
import com.sans.finance.domain.usecase.GetWealthMetricsUseCase
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
class WealthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var portfolioRepository: PortfolioRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var accountTypeRepository: AccountTypeRepository
    private lateinit var goalRepository: GoalRepository
    private lateinit var getWealthMetricsUseCase: GetWealthMetricsUseCase
    private lateinit var getWealthDistributionUseCase: GetWealthDistributionUseCase
    private lateinit var getFinancialFreedomStatsUseCase: GetFinancialFreedomStatsUseCase
    private lateinit var getEmergencyFundStressTestUseCase: GetEmergencyFundStressTestUseCase
    private lateinit var getSavingsRateVelocityUseCase: GetSavingsRateVelocityUseCase
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var localeManager: LocaleManager

    private val prefsFlow = MutableStateFlow(UserPreferences(isPrivacyModeEnabled = false))
    private val metricsFlow = MutableStateFlow(
        WealthMetrics(
            cashAssets = 50_000_000L,
            liabilities = 10_000_000L,
            portfolioValue = 100_000_000L,
            monthlyBurn = 5_000_000L,
            runwayMonths = 10.0,
            monthlyPassiveIncome = 1_000_000L,
            annualPassiveIncome = 12_000_000L,
            fiCoveragePct = 20.0,
            fiStage = "Foundation Stage (<25%)",
            fiNextStageGap = 250_000L,
            monthlyIncome = 20_000_000L,
            monthlyExpense = 5_000_000L,
            monthlySavings = 15_000_000L,
            currencyCode = "IDR"
        )
    )
    private val freedomFlow = MutableStateFlow(
        FinancialFreedomStats(
            yearsOfCover = 30.0,
            freedomScore = 1.0f,
            totalAssets = 150_000_000L,
            annualExpense = 5_000_000L,
            currencyCode = "IDR"
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        portfolioRepository = mockk(relaxed = true)
        every { portfolioRepository.getLatestSnapshotHeader() } returns flowOf(
            PortfolioSnapshotHeaderEntity(
                snapshotDate = 1700000000000L,
                exchangeRateUsd = 16000.0,
                totalValueIdr = 100_000_000.0,
                totalValueUsd = 6250.0
            )
        )
        every { portfolioRepository.getLatestSnapshot() } returns flowOf(
            listOf(
                PortfolioHoldingEntity(
                    id = 1,
                    snapshotDate = 1700000000000L,
                    source = "Bibit",
                    asset = "SBN ORI024",
                    category = "Bonds",
                    assetClass = "Fixed Income",
                    currency = "IDR",
                    quantity = 100.0,
                    price = 1_000_000.0,
                    valueIdr = 100_000_000.0,
                    account = "Bibit",
                    details = null
                )
            )
        )
        every { portfolioRepository.getTotalValueOverTime() } returns flowOf(
            listOf(SnapshotTotal(snapshot_date = 1700000000000L, totalIdr = 100_000_000.0, totalUsd = 6250.0))
        )

        accountRepository = mockk(relaxed = true)
        every { accountRepository.getAllAccounts() } returns flowOf(
            listOf(
                AccountEntity(id = 1, name = "BCA", type = "Bank", balance = 50_000_000L, currency = "IDR")
            )
        )

        accountTypeRepository = mockk(relaxed = true)
        every { accountTypeRepository.getAllAccountTypes() } returns flowOf(
            listOf(
                AccountTypeEntity(id = 1, name = "Bank", icon = "AccountBalance", isLiability = false)
            )
        )

        goalRepository = mockk(relaxed = true)
        every { goalRepository.getAllGoals() } returns flowOf(
            listOf(
                Goal(id = 1, name = "Emergency Fund", targetAmount = 100_000_000L, targetType = "TOTAL", targetName = null, currency = "IDR")
            )
        )

        getWealthMetricsUseCase = mockk()
        every { getWealthMetricsUseCase() } returns metricsFlow

        getFinancialFreedomStatsUseCase = mockk()
        every { getFinancialFreedomStatsUseCase() } returns freedomFlow

        getEmergencyFundStressTestUseCase = mockk()
        every { getEmergencyFundStressTestUseCase() } returns flowOf(
            EmergencyFundStressTest(
                liquidCashReserves = 50_000_000L,
                baselineMonthlyBurn = 5_000_000L,
                baselineRunwayMonths = 10.0,
                baselineTier = SafetyBufferTier.STRONG,
                scenarios = emptyList(),
                currencyCode = "IDR"
            )
        )

        getSavingsRateVelocityUseCase = mockk()
        every { getSavingsRateVelocityUseCase() } returns flowOf(
            SavingsRateVelocitySummary(
                currentMonthSavingsRatePct = 50.0,
                threeMonthAvgSavingsRatePct = 45.0,
                sixMonthAvgSavingsRatePct = 40.0,
                monthlyNetWorthVelocity = 10_000_000L,
                momentumTrend = MomentumTrend.ACCELERATING,
                history = emptyList(),
                currencyCode = "IDR"
            )
        )

        getWealthDistributionUseCase = mockk()
        every { getWealthDistributionUseCase(any()) } returns flowOf(
            mapOf("Fixed Income" to 100_000_000L, "Cash" to 50_000_000L)
        )

        userPreferencesRepository = mockk(relaxed = true)
        every { userPreferencesRepository.userPreferences } returns prefsFlow

        localeManager = mockk(relaxed = true)
        every { localeManager.getCurrency() } returns "IDR"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = WealthViewModel(
        portfolioRepository = portfolioRepository,
        accountRepository = accountRepository,
        accountTypeRepository = accountTypeRepository,
        goalRepository = goalRepository,
        getWealthMetricsUseCase = getWealthMetricsUseCase,
        getWealthDistributionUseCase = getWealthDistributionUseCase,
        getFinancialFreedomStatsUseCase = getFinancialFreedomStatsUseCase,
        getEmergencyFundStressTestUseCase = getEmergencyFundStressTestUseCase,
        getSavingsRateVelocityUseCase = getSavingsRateVelocityUseCase,
        userPreferencesRepository = userPreferencesRepository,
        localeManager = localeManager
    )

    @Test
    fun `initial state aggregates metrics, assets, net worth, and hub counts`() = runTest {
        val viewModel = createViewModel()
        viewModel.state.test {
            var state = awaitItem()
            while (state.totalAssets == 0L) state = awaitItem()

            assertEquals(150_000_000L, state.totalAssets)
            assertEquals(140_000_000L, state.netWorth)
            assertEquals(50_000_000L, state.cashAssets)
            assertEquals(100_000_000L, state.portfolioValue)
            assertEquals(10_000_000L, state.liabilities)
            assertEquals(1, state.accountsCount)
            assertEquals(1, state.goalsCount)
            assertEquals(1.0f, state.avgGoalProgress)
            assertEquals(2, state.wealthDistribution.size)
            assertFalse(state.isPrivacyModeEnabled)
        }
    }

    @Test
    fun `switching wealth distribution tab updates state`() = runTest {
        val viewModel = createViewModel()
        viewModel.state.test {
            awaitItem()
            viewModel.setWealthDistributionTab(WealthDistributionTab.CURRENCY)
            var state = awaitItem()
            while (state.wealthDistributionTab != WealthDistributionTab.CURRENCY) state = awaitItem()

            assertEquals(WealthDistributionTab.CURRENCY, state.wealthDistributionTab)
        }
    }

    @Test
    fun `toggling privacy mode calls repository`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.togglePrivacyMode()
        advanceUntilIdle()

        coVerify { userPreferencesRepository.setPrivacyModeEnabled(true) }
    }

    @Test
    fun `setFireManualEnabled calls userPreferencesRepository`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setFireManualEnabled(true)
        advanceUntilIdle()

        coVerify { userPreferencesRepository.setFireManualEnabled(true) }
    }

    @Test
    fun `setManualFireAnnualExpense calls userPreferencesRepository`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setManualFireAnnualExpense(60_000_000L)
        advanceUntilIdle()

        coVerify { userPreferencesRepository.setManualFireAnnualExpense(60_000_000L) }
    }
}
