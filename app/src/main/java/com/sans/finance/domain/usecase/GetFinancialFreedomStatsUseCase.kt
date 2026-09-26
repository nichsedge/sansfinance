package com.sans.finance.domain.usecase

import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.model.FinancialFreedomStats
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.AccountTypeRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.PortfolioRepository
import com.sans.finance.domain.repository.UserPreferencesRepository
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.data.local.entity.AccountTypeEntity
import com.sans.finance.data.local.entity.ExchangeRateEntity
import com.sans.finance.data.local.dao.SnapshotTotal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetFinancialFreedomStatsUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val portfolioRepository: PortfolioRepository,
    private val accountRepository: AccountRepository,
    private val accountTypeRepository: AccountTypeRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val localeManager: LocaleManager,
    private val currencyDao: CurrencyDao
) {
    operator fun invoke(): Flow<FinancialFreedomStats> {
        val now = System.currentTimeMillis()
        val yearAgo = now - (365L * 24 * 60 * 60 * 1000)
        val baseCurrency = localeManager.getCurrency()

        val financeFlow = combine(
            accountRepository.getAllAccounts(),
            accountTypeRepository.getAllAccountTypes(),
            portfolioRepository.getTotalValueOverTime(),
            currencyDao.getAllRates()
        ) { accounts, types, history, rates ->
            FinanceData(accounts, types, history, rates)
        }

        val statsFlow = combine(
            expenseRepository.getTotalAmountByTypeBetween(yearAgo, now, "EXPENSE"),
            expenseRepository.getOldestExpenseDate(),
            userPreferencesRepository.userPreferences.map { it.fireManualEnabled },
            userPreferencesRepository.userPreferences.map { it.manualFireAnnualExpense }
        ) { annualExpenseIdr, oldestDate, isFireManual, manualFireExpense ->
            StatsData(annualExpenseIdr, oldestDate, isFireManual, manualFireExpense)
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

            val latestPortfolioIdr = finance.history.lastOrNull()?.totalIdr ?: 0.0
            val portfolioAssets = if (baseRate > 0) ((latestPortfolioIdr / baseRate) * 100).toLong() else 0L

            val liabilityTypeNames = finance.types.filter { it.isLiability }.map { it.name }.toSet()
            val investmentTypeNames = finance.types.filter { it.isInvestment }.map { it.name }.toSet()
            val accountAssets = finance.accounts
                .filter { it.type !in liabilityTypeNames && it.type !in investmentTypeNames }
                .sumOf { convertToBase(it.balance, it.currency) }
            val investmentAccountAssets = finance.accounts
                .filter { it.type in investmentTypeNames }
                .sumOf { convertToBase(it.balance, it.currency) }

            val totalAssets = portfolioAssets + accountAssets + investmentAccountAssets

            val annualExpenseInBase = if (baseRate > 0) (( (stats.annualExpenseIdr ?: 0L).toDouble() / baseRate)).toLong() else 0L

            val firstTxnDate = stats.oldestDate ?: now
            val daysOfData = ((now - firstTxnDate) / (24 * 60 * 60 * 1000)).coerceAtLeast(1L)

            val effectiveAnnualExpense = if (stats.isFireManual) {
                stats.manualFireExpense
            } else if (daysOfData < 365) {
                (annualExpenseInBase.toDouble() / daysOfData * 365).toLong()
            } else {
                annualExpenseInBase
            }

            val freedomYears = if (effectiveAnnualExpense > 0) {
                totalAssets.toDouble() / effectiveAnnualExpense.toDouble()
            } else {
                0.0
            }

            val freedomScore = if (effectiveAnnualExpense > 0) {
                (totalAssets.toDouble() / (effectiveAnnualExpense.toDouble() * 25.0)).toFloat()
                    .coerceIn(0f, 1f)
            } else {
                0f
            }

            FinancialFreedomStats(
                yearsOfCover = freedomYears,
                freedomScore = freedomScore,
                totalAssets = totalAssets,
                annualExpense = effectiveAnnualExpense,
                currencyCode = baseCurrency
            )
        }
    }

    private data class FinanceData(
        val accounts: List<AccountEntity>,
        val types: List<AccountTypeEntity>,
        val history: List<SnapshotTotal>,
        val rates: List<ExchangeRateEntity>
    )

    private data class StatsData(
        val annualExpenseIdr: Long?,
        val oldestDate: Long?,
        val isFireManual: Boolean,
        val manualFireExpense: Long
    )
}
