package com.sans.finance.data.util

import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.data.local.entity.AccountTypeEntity
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PortfolioAssetExportTest {

    private val sampleHolding = PortfolioHoldingEntity(
        id = 1L,
        snapshotDate = 1700000000000L,
        source = "KSEI",
        category = "Stock",
        asset = "BBCA",
        currency = "IDR",
        quantity = 100.0,
        price = 10000.0,
        valueIdr = 1_000_000.0,
        assetClass = "Equities",
        account = "Stockbit",
        details = null,
        yieldRate = 3.5
    )

    private val sampleAccounts = listOf(
        AccountEntity(id = 1L, name = "Wallet", type = "Cash", balance = 50_000_00L, currency = "IDR"),
        AccountEntity(id = 2L, name = "USD Savings", type = "Bank", balance = 100_00L, currency = "USD"),
        AccountEntity(id = 3L, name = "Credit Card", type = "Credit Card", balance = 20_000_00L, currency = "IDR")
    )

    private val sampleTypes = listOf(
        AccountTypeEntity(id = 1L, name = "Cash", icon = "Wallet", isLiability = false),
        AccountTypeEntity(id = 2L, name = "Bank", icon = "AccountBalance", isLiability = false),
        AccountTypeEntity(id = 3L, name = "Credit Card", icon = "CreditCard", isLiability = true)
    )

    private val ratesMap = mapOf("USD" to 16_000.0)

    @Test
    fun jsonExportIncludesAccountsAndTotalsAndRoundTripsHoldingsOnly() {
        val jsonStr = PortfolioJsonExporter.toSnapshotJson(
            date = 1700000000000L,
            holdings = listOf(sampleHolding),
            accounts = sampleAccounts,
            accountTypes = sampleTypes,
            ratesMap = ratesMap
        )

        assertTrue(jsonStr.contains("liquid_cash_accounts"))
        assertTrue(jsonStr.contains("USD Savings"))
        assertTrue(jsonStr.contains("Credit Card"))

        val reimported = PortfolioJsonImporter.parseContent(jsonStr)
        assertEquals(1, reimported.holdings.size)
        assertEquals("BBCA", reimported.holdings.first().asset)
        assertEquals(3.5, reimported.holdings.first().yieldRate ?: 0.0, 0.001)
    }

    @Test
    fun csvExportIncludesAccountsAndRoundTripsHoldingsOnly() {
        val csvStr = PortfolioCsvExporter.toCsv(
            date = 1700000000000L,
            holdings = listOf(sampleHolding),
            accounts = sampleAccounts,
            accountTypes = sampleTypes,
            ratesMap = ratesMap
        )

        assertTrue(csvStr.contains("SansFinance,Cash,Wallet,IDR,50000.0,1.0,50000.0"))
        assertTrue(csvStr.contains("SansFinance,Bank,USD Savings,USD,100.0,16000.0,1600000.0"))
        assertTrue(csvStr.contains("SansFinance,Credit Card,Credit Card,IDR,-20000.0,1.0,-20000.0"))

        val (_, reimportedHoldings, _) = PortfolioCsvParser.parse(csvStr)
        assertEquals(1, reimportedHoldings.size)
        assertEquals("BBCA", reimportedHoldings.first().asset)
    }
}
