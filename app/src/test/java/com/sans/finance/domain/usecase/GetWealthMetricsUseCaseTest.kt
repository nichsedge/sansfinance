package com.sans.finance.domain.usecase

import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.data.local.entity.AccountTypeEntity
import com.sans.finance.data.local.entity.ExchangeRateEntity
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.AccountTypeRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.PortfolioRepository
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetWealthMetricsUseCaseTest {

    private val accountRepository: AccountRepository = mockk()
    private val accountTypeRepository: AccountTypeRepository = mockk()
    private val portfolioRepository: PortfolioRepository = mockk()
    private val expenseRepository: ExpenseRepository = mockk()
    private val localeManager: LocaleManager = mockk()
    private val currencyDao: com.sans.finance.data.local.dao.CurrencyDao = mockk()

    private lateinit var useCase: GetWealthMetricsUseCase

    @Before
    fun setup() {
        useCase = GetWealthMetricsUseCase(
            accountRepository,
            accountTypeRepository,
            portfolioRepository,
            expenseRepository,
            localeManager,
            currencyDao
        )
    }

    @Test
    fun `invoke returns correct metrics`() = runTest {
        // Given
        every { localeManager.getCurrency() } returns "USD"
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(
            AccountEntity(id = 1, name = "Cash", balance = 300000, type = "Cash", currency = "USD")
        ))
        every { accountTypeRepository.getAllAccountTypes() } returns flowOf(listOf(
            AccountTypeEntity(id = 1, name = "Cash", icon = "", isLiability = false, createdAt = 0)
        ))
        every { portfolioRepository.getLatestSnapshot() } returns flowOf(listOf(
            PortfolioHoldingEntity(
                snapshotDate = 0,
                source = "Bibit",
                category = "SBN",
                asset = "Sukuk ST012",
                currency = "IDR",
                quantity = 1.0,
                price = 16000000.0,
                valueIdr = 16000000.0, // 1000 USD -> 100000 cents
                assetClass = "Bond",
                account = "Bibit",
                details = null
            )
        ))
        every { currencyDao.getAllRates() } returns flowOf(listOf(
            ExchangeRateEntity(code = "USD", rateToIdr = 16000.0)
        ))

        // 3-month expense: 1200 USD total -> 120,000 cents -> 1,920,000,000 cents-IDR
        // monthlyBurn = 40,000 cents (400 USD)
        every { expenseRepository.getTotalAmountByTypeBetween(any(), any(), "EXPENSE") } returns flowOf(1920000000L)

        // Current month income: 1000 USD -> 100,000 cents -> 1,600,000,000 cents-IDR
        every { expenseRepository.getTotalAmountByTypeBetween(any(), any(), "INCOME") } returns flowOf(1600000000L)

        // When
        useCase().test {
            val metrics = awaitItem()
            // Then
            assertEquals(300000L, metrics.cashAssets)
            assertEquals(100000L, metrics.portfolioValue)
            assertEquals(40000L, metrics.monthlyBurn)
            assertEquals(7.5, metrics.runwayMonths, 0.01) // 300000 / 40000 = 7.5
            assertEquals(100000L, metrics.monthlyIncome)
            assertEquals(120000L, metrics.monthlyExpense) // TotalAmountByTypeBetween returned 1920000000 IDR = 120,000 cents
            cancelAndIgnoreRemainingEvents()
        }
    }
}
