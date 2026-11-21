package com.sans.finance.domain.usecase

import app.cash.turbine.test
import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.data.local.entity.AccountTypeEntity
import com.sans.finance.data.local.entity.ExchangeRateEntity
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.model.UserPreferences
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.AccountTypeRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.PortfolioRepository
import com.sans.finance.domain.repository.UserPreferencesRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetFinancialFreedomStatsUseCaseTest {

    private val expenseRepository: ExpenseRepository = mockk()
    private val portfolioRepository: PortfolioRepository = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val accountTypeRepository: AccountTypeRepository = mockk()
    private val userPreferencesRepository: UserPreferencesRepository = mockk()
    private val localeManager: LocaleManager = mockk()
    private val currencyDao: com.sans.finance.data.local.dao.CurrencyDao = mockk()

    private lateinit var useCase: GetFinancialFreedomStatsUseCase

    @Before
    fun setup() {
        useCase = GetFinancialFreedomStatsUseCase(
            expenseRepository,
            portfolioRepository,
            accountRepository,
            accountTypeRepository,
            userPreferencesRepository,
            localeManager,
            currencyDao
        )
    }

    @Test
    fun `invoke returns correct stats when fire manual is disabled`() = runTest {
        // Given
        every { localeManager.getCurrency() } returns "USD"
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(
            AccountEntity(id = 1, name = "Cash", balance = 100000, type = "Cash", currency = "USD")
        ))
        every { accountTypeRepository.getAllAccountTypes() } returns flowOf(listOf(
            AccountTypeEntity(id = 1, name = "Cash", icon = "", isLiability = false, createdAt = 0)
        ))
        every { portfolioRepository.getTotalValueOverTime() } returns flowOf(listOf(
            SnapshotTotal(snapshot_date = 0, totalIdr = 16000000.0, totalUsd = 1000.0) // 1000 USD
        ))
        every { currencyDao.getAllRates() } returns flowOf(listOf(
            ExchangeRateEntity(code = "USD", rateToIdr = 16000.0)
        ))
        // Annual Expense: 1000 USD -> 100,000 cents -> 1,600,000,000 cents-IDR
        every { expenseRepository.getTotalAmountByTypeBetween(any(), any(), "EXPENSE") } returns flowOf(1600000000L)
        every { expenseRepository.getOldestExpenseDate() } returns flowOf(System.currentTimeMillis() - 366L * 24 * 60 * 60 * 1000)

        every { userPreferencesRepository.userPreferences } returns flowOf(UserPreferences(fireManualEnabled = false, manualFireAnnualExpense = 0L))

        // When
        useCase().test {
            val stats = awaitItem()
            // Then
            assertEquals(200000L, stats.totalAssets) // 100000 Cash + 100000 Portfolio
            assertEquals(100000L, stats.annualExpense)
            assertEquals(2.0, stats.yearsOfCover, 0.01)
            assertEquals(0.08f, stats.freedomScore, 0.01f)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
