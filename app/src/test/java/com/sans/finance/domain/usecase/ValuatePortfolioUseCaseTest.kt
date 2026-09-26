package com.sans.finance.domain.usecase

import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.domain.repository.CurrencyRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ValuatePortfolioUseCaseTest {

    private lateinit var currencyRepository: CurrencyRepository
    private lateinit var useCase: ValuatePortfolioUseCase

    @Before
    fun setUp() {
        currencyRepository = mockk(relaxed = true)
        useCase = ValuatePortfolioUseCase(currencyRepository)
    }

    @Test
    fun `empty holdings returns zero valuation`() = runBlocking {
        val result = useCase(emptyList(), "IDR")
        assertEquals(0.0, result.totalValueInBase, 0.001)
        assertEquals(0.0, result.totalGainInBase, 0.001)
        assertEquals(0.0, result.totalGainPercentage, 0.001)
        assertTrue(result.valuedHoldings.isEmpty())
    }

    @Test
    fun `IDR holding with cost basis in details calculates price gain`() = runBlocking {
        val holding = PortfolioHoldingEntity(
            snapshotDate = 1000L,
            source = "KSEI",
            category = "Indo Stocks",
            asset = "BBCA",
            currency = "IDR",
            quantity = 10.0,
            price = 10000.0,
            valueIdr = 100000.0,
            assetClass = "Equities",
            account = "Stockbit",
            details = "Broker: Stockbit",
            costBasis = 80000.0
        )

        val result = useCase(listOf(holding), "IDR")

        // Value: 100,000 IDR, Cost: 80,000 IDR -> Gain: +20,000 IDR (+25%)
        assertEquals(100000.0, result.totalValueInBase, 0.01)
        assertEquals(80000.0, result.totalHistoricalCostInBase, 0.01)
        assertEquals(20000.0, result.totalPriceGainInBase, 0.01)
        assertEquals(0.0, result.totalFxGainInBase, 0.01)
        assertEquals(20000.0, result.totalGainInBase, 0.01)
        assertEquals(25.0, result.totalGainPercentage, 0.01)
        assertTrue(result.hasCostBasis)
        assertTrue(result.valuedHoldings.first().hasCostBasis)
    }

    @Test
    fun `holding without cost basis in details sets hasCostBasis to false`() = runBlocking {
        val holding = PortfolioHoldingEntity(
            snapshotDate = 1000L,
            source = "KSEI",
            category = "Indo Stocks",
            asset = "BBCA",
            currency = "IDR",
            quantity = 2900.0,
            price = 6700.0,
            valueIdr = 19430000.0,
            assetClass = "Equities",
            account = "Ajaib",
            details = "Stock: BBCA - BANK CENTRAL ASIA Tbk, Broker: PT AJAIB SEKURITAS ASIA"
        )

        val result = useCase(listOf(holding), "IDR")

        assertEquals(19430000.0, result.totalValueInBase, 0.01)
        assertEquals(0.0, result.totalPriceGainInBase, 0.001)
        assertEquals(0.0, result.totalGainInBase, 0.001)
        assertEquals(0.0, result.totalGainPercentage, 0.001)
        org.junit.Assert.assertFalse(result.hasCostBasis)
        org.junit.Assert.assertFalse(result.valuedHoldings.first().hasCostBasis)
    }

    @Test
    fun `USD holding calculates FX gain when exchange rate changes`() = runBlocking {
        val holding = PortfolioHoldingEntity(
            snapshotDate = 1000L,
            source = "Binance",
            category = "Spot",
            asset = "BTC",
            currency = "USD",
            quantity = 1.0,
            price = 100.0,
            valueIdr = 1500000.0,
            assetClass = "Crypto",
            account = "Binance",
            details = null
        )

        // Historical USD to IDR rate at snapshot was 15,000; today is 16,000
        coEvery { currencyRepository.getHistoricalRate("USD", "IDR", any<Long>()) } returns 16000.0
        coEvery { currencyRepository.getHistoricalRate("USD", "IDR", 1000L) } returns 15000.0
        coEvery { currencyRepository.getRateToIdr("USD") } returns 16000.0

        val result = useCase(listOf(holding), "IDR", snapshotDate = 1000L)

        // 100 USD nominal: Historical value = 1,500,000 IDR, Current value = 1,600,000 IDR -> FX Gain = +100,000 IDR
        assertEquals(1600000.0, result.totalValueInBase, 0.01)
        assertEquals(1500000.0, result.totalHistoricalCostInBase, 0.01)
        assertEquals(100000.0, result.totalFxGainInBase, 0.01)
        assertEquals(0.0, result.totalPriceGainInBase, 0.01)
        assertEquals(100000.0, result.totalGainInBase, 0.01)
        assertEquals(6.666, result.totalGainPercentage, 0.01)
        org.junit.Assert.assertFalse(result.hasCostBasis)
    }
}
