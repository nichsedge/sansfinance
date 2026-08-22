package com.sans.finance.domain.model

import com.sans.finance.data.local.entity.PortfolioHoldingEntity

data class ValuedHolding(
    val holding: PortfolioHoldingEntity,
    val baseCurrency: String,
    val currentValueInBase: Double,
    val historicalValueInBase: Double,
    val historicalFxRate: Double,
    val currentFxRate: Double,
    val priceGainInBase: Double,
    val fxGainInBase: Double,
    val totalGainInBase: Double,
    val totalGainPercentage: Double
)

data class MultiCurrencyPortfolioValuation(
    val baseCurrency: String,
    val totalValueInBase: Double,
    val totalHistoricalCostInBase: Double,
    val totalPriceGainInBase: Double,
    val totalFxGainInBase: Double,
    val totalGainInBase: Double,
    val totalGainPercentage: Double,
    val valuedHoldings: List<ValuedHolding>,
    val currencyBreakdowns: Map<String, CurrencyValuationSummary>
)

data class CurrencyValuationSummary(
    val currency: String,
    val count: Int,
    val totalInOriginalCurrency: Double,
    val totalInBaseCurrency: Double,
    val fxGainInBase: Double,
    val priceGainInBase: Double,
    val currentFxRate: Double,
    val historicalFxRate: Double
)
