package com.sans.finance.domain.usecase

import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.domain.model.CurrencyValuationSummary
import com.sans.finance.domain.model.MultiCurrencyPortfolioValuation
import com.sans.finance.domain.model.ValuedHolding
import com.sans.finance.domain.repository.CurrencyRepository
import javax.inject.Inject

class ValuatePortfolioUseCase @Inject constructor(
    private val currencyRepository: CurrencyRepository
) {
    suspend operator fun invoke(
        holdings: List<PortfolioHoldingEntity>,
        baseCurrency: String,
        snapshotDate: Long = System.currentTimeMillis()
    ): MultiCurrencyPortfolioValuation {
        if (holdings.isEmpty()) {
            return MultiCurrencyPortfolioValuation(
                baseCurrency = baseCurrency,
                totalValueInBase = 0.0,
                totalHistoricalCostInBase = 0.0,
                totalPriceGainInBase = 0.0,
                totalFxGainInBase = 0.0,
                totalGainInBase = 0.0,
                totalGainPercentage = 0.0,
                valuedHoldings = emptyList(),
                currencyBreakdowns = emptyMap()
            )
        }

        // Cache rates per currency and holding date
        val historicalRates = mutableMapOf<Pair<String, Long>, Double>()
        val currentRates = mutableMapOf<String, Double>()

        val todayMillis = System.currentTimeMillis()

        val distinctCurrencies = (holdings.map { it.currency } + listOf(baseCurrency, "IDR", "USD")).distinct()
        for (curr in distinctCurrencies) {
            if (curr.equals(baseCurrency, ignoreCase = true)) {
                currentRates[curr] = 1.0
            } else {
                val currRate = currencyRepository.getHistoricalRate(curr, baseCurrency, todayMillis)
                    ?: fallbackConversion(curr, baseCurrency)
                currentRates[curr] = currRate
            }
        }

        for (holding in holdings) {
            val curr = holding.currency
            val itemDate = if (holding.snapshotDate > 0) holding.snapshotDate else snapshotDate
            val key = curr to itemDate
            if (!historicalRates.containsKey(key)) {
                val histRate = if (curr.equals(baseCurrency, ignoreCase = true)) {
                    1.0
                } else {
                    currencyRepository.getHistoricalRate(curr, baseCurrency, itemDate)
                        ?: fallbackConversion(curr, baseCurrency)
                }
                historicalRates[key] = histRate
            }
        }

        val valuedHoldings = holdings.map { holding ->
            val curr = holding.currency
            val itemDate = if (holding.snapshotDate > 0) holding.snapshotDate else snapshotDate
            val histFx = historicalRates[curr to itemDate] ?: 1.0
            val currFx = currentRates[curr] ?: 1.0

            // Determine nominal amount in original currency
            val originalNominal = if (holding.quantity > 0 && holding.price != null) {
                holding.quantity * holding.price
            } else if (curr.equals("IDR", ignoreCase = true)) {
                holding.valueIdr
            } else {
                // If valueIdr is available and original price wasn't specified, estimate original currency value
                val idrRateAtSnapshot = historicalRates["IDR" to itemDate] ?: 1.0
                if (idrRateAtSnapshot > 0 && histFx > 0) {
                    holding.valueIdr / (1.0 / idrRateAtSnapshot * histFx)
                } else {
                    holding.valueIdr
                }
            }

            val histValueInBase = originalNominal * histFx
            val currValueInBase = originalNominal * currFx

            // FX gain is the change in value purely due to currency exchange rate movement
            val fxGainInBase = originalNominal * (currFx - histFx)
            val priceGainInBase = 0.0 // Default 0 when using snapshot holding price
            val totalGainInBase = (currValueInBase - histValueInBase)
            val totalGainPercent = if (histValueInBase > 0) (totalGainInBase / histValueInBase) * 100.0 else 0.0

            ValuedHolding(
                holding = holding,
                baseCurrency = baseCurrency,
                currentValueInBase = currValueInBase,
                historicalValueInBase = histValueInBase,
                historicalFxRate = histFx,
                currentFxRate = currFx,
                priceGainInBase = priceGainInBase,
                fxGainInBase = fxGainInBase,
                totalGainInBase = totalGainInBase,
                totalGainPercentage = totalGainPercent
            )
        }

        val totalValueInBase = valuedHoldings.sumOf { it.currentValueInBase }
        val totalHistCostInBase = valuedHoldings.sumOf { it.historicalValueInBase }
        val totalPriceGain = valuedHoldings.sumOf { it.priceGainInBase }
        val totalFxGain = valuedHoldings.sumOf { it.fxGainInBase }
        val totalGain = totalValueInBase - totalHistCostInBase
        val totalGainPct = if (totalHistCostInBase > 0) (totalGain / totalHistCostInBase) * 100.0 else 0.0

        val currencyBreakdowns = valuedHoldings.groupBy { it.holding.currency }
            .mapValues { (currency, items) ->
                val totalOriginal = items.sumOf {
                    if (it.holding.quantity > 0 && it.holding.price != null) it.holding.quantity * it.holding.price else it.holding.valueIdr
                }
                val totalBase = items.sumOf { it.currentValueInBase }
                val fxGain = items.sumOf { it.fxGainInBase }
                val priceGain = items.sumOf { it.priceGainInBase }
                val currFx = currentRates[currency] ?: 1.0
                val histFx = items.map { it.historicalFxRate }.average().takeIf { !it.isNaN() } ?: 1.0

                CurrencyValuationSummary(
                    currency = currency,
                    count = items.size,
                    totalInOriginalCurrency = totalOriginal,
                    totalInBaseCurrency = totalBase,
                    fxGainInBase = fxGain,
                    priceGainInBase = priceGain,
                    currentFxRate = currFx,
                    historicalFxRate = histFx
                )
            }

        return MultiCurrencyPortfolioValuation(
            baseCurrency = baseCurrency,
            totalValueInBase = totalValueInBase,
            totalHistoricalCostInBase = totalHistCostInBase,
            totalPriceGainInBase = totalPriceGain,
            totalFxGainInBase = totalFxGain,
            totalGainInBase = totalGain,
            totalGainPercentage = totalGainPct,
            valuedHoldings = valuedHoldings,
            currencyBreakdowns = currencyBreakdowns
        )
    }

    private suspend fun fallbackConversion(from: String, to: String): Double {
        if (from.equals(to, ignoreCase = true)) return 1.0
        val fromToIdr = if (from.equals("IDR", ignoreCase = true)) 1.0 else currencyRepository.getRateToIdr(from) ?: 1.0
        val toToIdr = if (to.equals("IDR", ignoreCase = true)) 1.0 else currencyRepository.getRateToIdr(to) ?: 1.0
        if (toToIdr == 0.0) return 1.0
        return fromToIdr / toToIdr
    }
}
