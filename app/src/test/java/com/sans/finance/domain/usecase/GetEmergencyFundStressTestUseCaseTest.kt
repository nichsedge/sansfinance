package com.sans.finance.domain.usecase

import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.data.local.entity.AccountTypeEntity
import com.sans.finance.data.local.entity.ExchangeRateEntity
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.model.SafetyBufferTier
import com.sans.finance.domain.model.StressTestScenarioType
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.AccountTypeRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.PortfolioRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetEmergencyFundStressTestUseCaseTest {

    private lateinit var accountRepository: AccountRepository
    private lateinit var accountTypeRepository: AccountTypeRepository
    private lateinit var portfolioRepository: PortfolioRepository
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var localeManager: LocaleManager
    private lateinit var currencyDao: CurrencyDao
    private lateinit var useCase: GetEmergencyFundStressTestUseCase

    @Before
    fun setUp() {
        accountRepository = mockk()
        accountTypeRepository = mockk()
        portfolioRepository = mockk()
        expenseRepository = mockk()
        localeManager = mockk()
        currencyDao = mockk()

        every { localeManager.getCurrency() } returns "IDR"
        every { currencyDao.getAllRates() } returns flowOf(listOf(ExchangeRateEntity("IDR", 1.0, System.currentTimeMillis())))

        useCase = GetEmergencyFundStressTestUseCase(
            accountRepository = accountRepository,
            accountTypeRepository = accountTypeRepository,
            portfolioRepository = portfolioRepository,
            expenseRepository = expenseRepository,
            localeManager = localeManager,
            currencyDao = currencyDao
        )
    }

    @Test
    fun `test emergency runway and stress scenarios with healthy liquid buffer`() = runBlocking {
        val accounts = listOf(
            AccountEntity(id = 1, name = "BCA Checking", balance = 60_000_000L, type = "Bank", currency = "IDR"),
            AccountEntity(id = 2, name = "Credit Card", balance = 5_000_000L, type = "Credit Card", currency = "IDR")
        )
        val accountTypes = listOf(
            AccountTypeEntity(name = "Bank", icon = "AccountBalance", isLiability = false),
            AccountTypeEntity(name = "Credit Card", icon = "CreditCard", isLiability = true)
        )
        val holdings = listOf(
            PortfolioHoldingEntity(
                snapshotDate = 1000L,
                source = "KSEI",
                category = "Indo Stocks",
                asset = "BBCA",
                currency = "IDR",
                quantity = 100.0,
                price = 10000.0,
                valueIdr = 100_000_000.0,
                assetClass = "Equities",
                account = "Stockbit",
                details = null
            )
        )

        every { accountRepository.getAllAccounts() } returns flowOf(accounts)
        every { accountTypeRepository.getAllAccountTypes() } returns flowOf(accountTypes)
        every { portfolioRepository.getLatestSnapshot() } returns flowOf(holdings)

        // 90-day total expense = 30,000,000 IDR -> 10,000,000 IDR / month
        every { expenseRepository.getTotalAmountByTypeBetween(any(), any(), "EXPENSE") } returns flowOf(30_000_000L)
        every { expenseRepository.getTotalAmountByTypeBetween(any(), any(), "INCOME") } returns flowOf(20_000_000L)

        val result = useCase().first()

        assertEquals(60_000_000L, result.liquidCashReserves)
        assertEquals(10_000_000L, result.baselineMonthlyBurn)
        assertEquals(6.0, result.baselineRunwayMonths, 0.01)
        assertEquals(SafetyBufferTier.STRONG, result.baselineTier)

        assertEquals(4, result.scenarios.size)
        val jobLoss = result.scenarios.first { it.type == StressTestScenarioType.ZERO_INCOME }
        assertEquals(6.0, jobLoss.runwayMonths, 0.01)

        val inflationSurge = result.scenarios.first { it.type == StressTestScenarioType.INFLATION_SURGE }
        assertEquals(12_500_000L, inflationSurge.monthlyBurn)
        assertEquals(4.8, inflationSurge.runwayMonths, 0.01)

        val marketDrawdown = result.scenarios.first { it.type == StressTestScenarioType.MARKET_DRAWDOWN }
        // 60M cash + 70M stressed portfolio = 130M / 10M = 13.0 months
        assertEquals(13.0, marketDrawdown.runwayMonths, 0.01)
        assertEquals(SafetyBufferTier.ANTIFRAGILE, marketDrawdown.tier)
    }

    @Test
    fun `test zero expenses handles runway gracefully`() = runBlocking {
        every { accountRepository.getAllAccounts() } returns flowOf(emptyList())
        every { accountTypeRepository.getAllAccountTypes() } returns flowOf(emptyList())
        every { portfolioRepository.getLatestSnapshot() } returns flowOf(emptyList())
        every { expenseRepository.getTotalAmountByTypeBetween(any(), any(), "EXPENSE") } returns flowOf(0L)
        every { expenseRepository.getTotalAmountByTypeBetween(any(), any(), "INCOME") } returns flowOf(0L)

        val result = useCase().first()

        assertEquals(0L, result.liquidCashReserves)
        assertEquals(0L, result.baselineMonthlyBurn)
        assertEquals(0.0, result.baselineRunwayMonths, 0.01)
        assertEquals(SafetyBufferTier.CRITICAL, result.baselineTier)
    }
}
