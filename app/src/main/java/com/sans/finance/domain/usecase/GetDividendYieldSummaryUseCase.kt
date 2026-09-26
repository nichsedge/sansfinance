package com.sans.finance.domain.usecase

import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.data.local.dao.InvestmentMetadataDao
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.model.DividendYieldSummary
import com.sans.finance.domain.model.HoldingYield
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.PortfolioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetDividendYieldSummaryUseCase @Inject constructor(
    private val portfolioRepository: PortfolioRepository,
    private val investmentMetadataDao: InvestmentMetadataDao,
    private val expenseRepository: ExpenseRepository,
    private val currencyDao: CurrencyDao,
    private val localeManager: LocaleManager
) {
    operator fun invoke(): Flow<DividendYieldSummary> {
        val now = System.currentTimeMillis()
        val yearAgo = now - (365L * 24 * 60 * 60 * 1000)
        val baseCurrency = localeManager.getCurrency()

        return combine(
            portfolioRepository.getLatestSnapshot(),
            investmentMetadataDao.getAllMetadata(),
            expenseRepository.getTotalAmountByTypeBetween(yearAgo, now, "EXPENSE"),
            currencyDao.getAllRates()
        ) { holdings, metadataList, annualExpenseIdr, rates ->
            val metadataMap = metadataList.associateBy { it.code.uppercase() }
            val ratesMap = rates.associate { it.code to it.rateToIdr }
            val baseRate = if (baseCurrency == "IDR") 1.0 else ratesMap[baseCurrency] ?: 1.0

            val holdingsYieldList = mutableListOf<HoldingYield>()
            var totalAnnualIncomeInBase = 0.0
            var totalPortfolioValueInBase = 0.0

            holdings.forEach { holding ->
                val code = holding.asset.uppercase()
                val meta = metadataMap[code]

                // Real yield hierarchy:
                // 1. Holding's own yieldRate (enriched from upstream R2 snapshot)
                // 2. Metadata rate from investment_metadata table
                // 3. Fallback rule based on asset class
                val yieldRate = holding.yieldRate?.takeIf { it > 0 }
                    ?: meta?.rate
                    ?: when {
                        holding.assetClass.contains("Bond", ignoreCase = true) ||
                        holding.assetClass.contains("Fixed Income", ignoreCase = true) -> 0.0625
                        holding.assetClass.contains("Dividend", ignoreCase = true) -> 0.045
                        else -> 0.0
                    }

                val holdingValueInIdr = holding.valueIdr
                val currentValueInBase = if (baseRate > 0) holdingValueInIdr / baseRate else holding.valueIdr
                totalPortfolioValueInBase += currentValueInBase

                if (yieldRate > 0) {
                    val annualIncome = currentValueInBase * yieldRate
                    val monthlyIncome = annualIncome / 12.0
                    totalAnnualIncomeInBase += annualIncome

                    holdingsYieldList.add(
                        HoldingYield(
                            code = holding.asset,
                            name = holding.details ?: holding.asset,
                            assetClass = holding.assetClass,
                            currentValueInBase = currentValueInBase,
                            annualYieldRate = yieldRate,
                            estimatedAnnualIncomeInBase = annualIncome,
                            monthlyIncomeInBase = monthlyIncome
                        )
                    )
                }
            }

            val weightedYieldOnCost = if (totalPortfolioValueInBase > 0) {
                (totalAnnualIncomeInBase / totalPortfolioValueInBase) * 100.0
            } else {
                0.0
            }

            val annualExpenseInBase = if (baseRate > 0) {
                ((annualExpenseIdr ?: 0L).toDouble() / baseRate)
            } else {
                0.0
            }

            val coveragePercentage = if (annualExpenseInBase > 0) {
                (totalAnnualIncomeInBase / annualExpenseInBase) * 100.0
            } else {
                0.0
            }

            DividendYieldSummary(
                totalAnnualIncomeInBase = totalAnnualIncomeInBase,
                totalMonthlyIncomeInBase = totalAnnualIncomeInBase / 12.0,
                portfolioYieldOnCost = weightedYieldOnCost,
                expenseCoveragePercentage = coveragePercentage,
                holdingsWithYield = holdingsYieldList.sortedByDescending { it.estimatedAnnualIncomeInBase },
                baseCurrency = baseCurrency
            )
        }
    }
}
