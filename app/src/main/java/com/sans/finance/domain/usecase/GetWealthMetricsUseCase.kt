package com.sans.finance.domain.usecase

import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.data.local.entity.AccountTypeEntity
import com.sans.finance.data.local.entity.InvestmentMetadataEntity
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.model.WealthMetrics
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.AccountTypeRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.InvestmentMetadataRepository
import com.sans.finance.domain.repository.PortfolioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import javax.inject.Inject

class GetWealthMetricsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val accountTypeRepository: AccountTypeRepository,
    private val portfolioRepository: PortfolioRepository,
    private val expenseRepository: ExpenseRepository,
    private val investmentMetadataRepository: InvestmentMetadataRepository,
    private val localeManager: LocaleManager,
    private val currencyDao: CurrencyDao
) {
    operator fun invoke(): Flow<WealthMetrics> {
        val now = System.currentTimeMillis()
        val ninetyDaysAgo = now - 90L * 24 * 60 * 60 * 1000
        val baseCurrency = localeManager.getCurrency()

        val cal = CalendarUtils.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val monthStart = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val nextMonthStart = cal.timeInMillis

        val financeFlow = combine(
            accountRepository.getAllAccounts(),
            accountTypeRepository.getAllAccountTypes(),
            portfolioRepository.getLatestSnapshot(),
            currencyDao.getAllRates()
        ) { accounts, types, holdings, rates ->
            FinanceData(accounts, types, holdings, rates)
        }

        val statsFlow = combine(
            expenseRepository.getTotalAmountByTypeBetween(ninetyDaysAgo, now + 86400000L, "EXPENSE"),
            expenseRepository.getTotalAmountByTypeBetween(monthStart, nextMonthStart, "INCOME"),
            expenseRepository.getTotalAmountByTypeBetween(monthStart, nextMonthStart, "EXPENSE"),
            investmentMetadataRepository.getAllMetadata()
        ) { total90dExpenseIdr, incomeIdr, expenseIdr, metadata ->
            StatsData(total90dExpenseIdr, incomeIdr, expenseIdr, metadata)
        }

        return combine(financeFlow, statsFlow) { finance, stats ->
            val ratesMap = finance.rates.associate { it.code to it.rateToIdr }
            val baseRate = if (baseCurrency == "IDR") 1.0 else ratesMap[baseCurrency] ?: 1.0

            fun convertToBase(amount: Long, from: String): Long {
                if (from == baseCurrency) return amount
                val fromRate = if (from == "IDR") 1.0 else ratesMap[from] ?: 1.0
                val toRate = baseRate
                if (toRate == 0.0) return amount
                return ((amount * fromRate) / toRate).toLong()
            }

            val liabilityTypeNames = finance.types.filter { it.isLiability }.map { it.name }.toSet()
            val investmentTypeNames = finance.types.filter { it.isInvestment }.map { it.name }.toSet()

            // Assets & Liabilities
            val cashAssets = finance.accounts
                .filter { it.type !in liabilityTypeNames && it.type !in investmentTypeNames }
                .sumOf { convertToBase(it.balance, it.currency) }
            val investmentAccountAssets = finance.accounts
                .filter { it.type in investmentTypeNames }
                .sumOf { convertToBase(it.balance, it.currency) }
            val liabilities = finance.accounts
                .filter { it.type in liabilityTypeNames }
                .sumOf { convertToBase(it.balance, it.currency) }

            val portfolioHoldingsValueIdr = finance.holdings.sumOf { it.valueIdr }
            val portfolioHoldingsValue = if (baseRate > 0) ((portfolioHoldingsValueIdr / baseRate) * 100).toLong() else 0L
            val portfolioValue = portfolioHoldingsValue + investmentAccountAssets

            // Monthly Burn (3-month avg)
            val monthlyBurn = if (baseRate > 0) (((stats.total90dExpenseIdr ?: 0L).toDouble() / baseRate) / 3.0).toLong() else 0L
            val runwayMonths = if (monthlyBurn > 0L) {
                (cashAssets.toDouble() / monthlyBurn.toDouble()).coerceAtMost(99.0)
            } else {
                if (cashAssets > 0L) 99.0 else 0.0
            }

            // Passive Income Calculation
            var totalMonthlyPassiveIdr = 0.0
            finance.holdings.forEach { holding ->
                val asset = holding.asset
                val cat = holding.category

                val meta = stats.metadata.find { asset.contains(it.code, ignoreCase = true) }
                val rate = holding.yieldRate?.takeIf { it > 0 }
                    ?: meta?.rate
                    ?: if (cat.contains("SBN", ignoreCase = true) || cat.contains("Bond", ignoreCase = true) || asset.contains("Sukuk", ignoreCase = true)) 0.0625 else 0.0

                if (rate > 0) {
                    val grossMonthly = (holding.valueIdr * rate) / 12.0
                    val netMonthly = grossMonthly * 0.90 // 10% tax for SBN/Sukuk in ID
                    totalMonthlyPassiveIdr += netMonthly
                }
            }
            val monthlyPassiveIncome = if (baseRate > 0) ((totalMonthlyPassiveIdr / baseRate) * 100).toLong() else 0L
            val annualPassiveIncome = monthlyPassiveIncome * 12

            // FI Coverage
            val fiCoveragePct = if (monthlyBurn > 0L) {
                ((monthlyPassiveIncome.toDouble() / monthlyBurn.toDouble()) * 100.0).coerceAtMost(100.0)
            } else {
                0.0
            }

            val (fiStage, fiNextStageGap) = when {
                fiCoveragePct >= 100.0 -> Pair("Full Financial Freedom (100%+)", 0L)
                fiCoveragePct >= 50.0 -> {
                    val gap = (monthlyBurn - monthlyPassiveIncome).coerceAtLeast(0L)
                    Pair("Lean FI Achieved (50%+)", gap)
                }
                fiCoveragePct >= 25.0 -> {
                    val halfBurn = (monthlyBurn * 0.50).toLong()
                    val gap = (halfBurn - monthlyPassiveIncome).coerceAtLeast(0L)
                    Pair("Emerging FI Buffer (25%+)", gap)
                }
                else -> {
                    val quarterBurn = (monthlyBurn * 0.25).toLong()
                    val gap = (quarterBurn - monthlyPassiveIncome).coerceAtLeast(0L)
                    Pair("Foundation Stage (<25%)", gap)
                }
            }

            val monthlyIncome = if (baseRate > 0) (((stats.incomeIdr ?: 0L).toDouble() / baseRate)).toLong() else 0L
            val monthlyExpense = if (baseRate > 0) (((stats.expenseIdr ?: 0L).toDouble() / baseRate)).toLong() else 0L
            val monthlySavings = (monthlyIncome - monthlyExpense).coerceAtLeast(0L)

            WealthMetrics(
                cashAssets = cashAssets,
                liabilities = liabilities,
                portfolioValue = portfolioValue,
                monthlyBurn = monthlyBurn,
                runwayMonths = runwayMonths,
                monthlyPassiveIncome = monthlyPassiveIncome,
                annualPassiveIncome = annualPassiveIncome,
                fiCoveragePct = fiCoveragePct,
                fiStage = fiStage,
                fiNextStageGap = fiNextStageGap,
                monthlyIncome = monthlyIncome,
                monthlyExpense = monthlyExpense,
                monthlySavings = monthlySavings,
                currencyCode = baseCurrency
            )
        }
    }

    private data class FinanceData(
        val accounts: List<AccountEntity>,
        val types: List<AccountTypeEntity>,
        val holdings: List<PortfolioHoldingEntity>,
        val rates: List<com.sans.finance.data.local.entity.ExchangeRateEntity>
    )

    private data class StatsData(
        val total90dExpenseIdr: Long?,
        val incomeIdr: Long?,
        val expenseIdr: Long?,
        val metadata: List<InvestmentMetadataEntity>
    )
}
