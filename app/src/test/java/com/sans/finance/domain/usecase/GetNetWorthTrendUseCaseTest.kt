package com.sans.finance.domain.usecase

import app.cash.turbine.test
import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.repository.PortfolioRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetNetWorthTrendUseCaseTest {

    private lateinit var portfolioRepository: PortfolioRepository
    private lateinit var currencyDao: CurrencyDao
    private lateinit var localeManager: LocaleManager
    private lateinit var useCase: GetNetWorthTrendUseCase

    @Before
    fun setUp() {
        portfolioRepository = mockk()
        currencyDao = mockk()
        localeManager = mockk()
        useCase = GetNetWorthTrendUseCase(portfolioRepository, currencyDao, localeManager)
    }

    @Test
    fun `invoke calculates trend correctly for 30 days`() = runTest {
        val now = System.currentTimeMillis()
        val history = listOf(
            SnapshotTotal(now - 10 * 24 * 60 * 60 * 1000, 1000.0, 0.0), // 10 days ago
            SnapshotTotal(now - 2 * 24 * 60 * 60 * 1000, 2000.0, 0.0)   // 2 days ago
        )

        every { portfolioRepository.getTotalValueOverTime() } returns flowOf(history)
        every { currencyDao.getAllRates() } returns flowOf(emptyList())
        every { localeManager.getCurrency() } returns "IDR"

        useCase.invoke(30).test {
            val trend = awaitItem()
            assertEquals(30, trend.size)
            // Last item should be most recent (now), which is 2000.0 IDR -> 200000 cents
            assertEquals(200000L, trend.last())
            // First item should be 30 days ago, which is 0.0 since no data before 10 days ago
            assertEquals(0L, trend.first())
            // 5 days ago should be 1000.0 -> 100000 cents
            assertEquals(100000L, trend[25]) // index 25 is 30-25=5 days ago
            cancelAndIgnoreRemainingEvents()
        }
    }
}
