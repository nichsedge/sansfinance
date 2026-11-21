package com.sans.finance.domain.usecase

import app.cash.turbine.test
import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.data.local.entity.AccountTypeEntity
import com.sans.finance.data.local.entity.ExchangeRateEntity
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.AccountTypeRepository
import com.sans.finance.domain.repository.BudgetRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.PortfolioRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetDashboardSummaryUseCaseTest {

    private val expenseRepository: ExpenseRepository = mockk()
    private val portfolioRepository: PortfolioRepository = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val accountTypeRepository: AccountTypeRepository = mockk()
    private val budgetRepository: BudgetRepository = mockk()
    private val localeManager: LocaleManager = mockk()
    private val currencyDao: com.sans.finance.data.local.dao.CurrencyDao = mockk()

    private lateinit var useCase: GetDashboardSummaryUseCase

    @Before
    fun setup() {
        useCase = GetDashboardSummaryUseCase(
            expenseRepository,
            portfolioRepository,
            accountRepository,
            accountTypeRepository,
            budgetRepository,
            localeManager,
            currencyDao
        )
    }

    @Test
    fun `invoke returns correct summary`() = runTest {
        // Given
        every { localeManager.getCurrency() } returns "USD"
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(
            AccountEntity(id = 1, name = "Bank", balance = 50000, type = "Bank", currency = "USD")
        ))
        every { accountTypeRepository.getAllAccountTypes() } returns flowOf(listOf(
            AccountTypeEntity(id = 1, name = "Bank", icon = "", isLiability = false, createdAt = 0)
        ))
        every { portfolioRepository.getTotalValueOverTime() } returns flowOf(listOf(
            SnapshotTotal(snapshot_date = 0, totalIdr = 8000000.0, totalUsd = 500.0) // 500 USD -> 50000 cents
        ))
        every { currencyDao.getAllRates() } returns flowOf(listOf(
            ExchangeRateEntity(code = "USD", rateToIdr = 16000.0)
        ))
        // Income: 2000 USD -> 200,000 cents -> 3,200,000,000 cents-IDR
        every { expenseRepository.getTotalAmountByTypeBetween(any(), any(), "INCOME") } returns flowOf(3200000000L)
        // Expense: 1000 USD -> 100,000 cents -> 1,600,000,000 cents-IDR
        every { expenseRepository.getTotalAmountByTypeBetween(any(), any(), "EXPENSE") } returns flowOf(1600000000L)

        every { budgetRepository.getAllBudgets() } returns flowOf(emptyList())
        every { expenseRepository.getSpendingByCategoryBetween(any(), any()) } returns flowOf(emptyList())

        // When
        useCase().test {
            val summary = awaitItem()
            // Then
            assertEquals(100000L, summary.totalAssets) // 50000 Bank + 50000 Portfolio
            assertEquals(200000L, summary.monthlyIncome)
            assertEquals(100000L, summary.monthlyExpense)
            assertEquals(0.5f, summary.monthlySavingsRate, 0.01f)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
