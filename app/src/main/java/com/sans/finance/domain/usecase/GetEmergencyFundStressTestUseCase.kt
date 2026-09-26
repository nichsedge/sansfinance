package com.sans.finance.domain.usecase

import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.model.EmergencyFundStressTest
import com.sans.finance.domain.model.SafetyBufferTier
import com.sans.finance.domain.model.StressTestScenario
import com.sans.finance.domain.model.StressTestScenarioType
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.AccountTypeRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.PortfolioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import javax.inject.Inject

class GetEmergencyFundStressTestUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val accountTypeRepository: AccountTypeRepository,
    private val portfolioRepository: PortfolioRepository,
    private val expenseRepository: ExpenseRepository,
    private val localeManager: LocaleManager,
    private val currencyDao: CurrencyDao
) {
    operator fun invoke(): Flow<EmergencyFundStressTest> {
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

        val accountsFlow = combine(
            accountRepository.getAllAccounts(),
            accountTypeRepository.getAllAccountTypes(),
            portfolioRepository.getLatestSnapshot(),
            currencyDao.getAllRates()
        ) { accounts, types, holdings, rates ->
            Tuple4(accounts, types, holdings, rates)
        }

        val expenseFlow = combine(
            expenseRepository.getTotalAmountByTypeBetween(ninetyDaysAgo, now + 86400000L, "EXPENSE"),
            expenseRepository.getTotalAmountByTypeBetween(monthStart, nextMonthStart, "INCOME"),
            expenseRepository.getTotalAmountByTypeBetween(monthStart, nextMonthStart, "EXPENSE")
        ) { total90dExpenseIdr, incomeIdr, expenseIdr ->
            Triple(total90dExpenseIdr, incomeIdr, expenseIdr)
        }

        return combine(accountsFlow, expenseFlow) { (accounts, types, holdings, rates), (total90dExp, currentInc, currentExp) ->
            val ratesMap = rates.associate { it.code to it.rateToIdr }
            val baseRate = if (baseCurrency == "IDR") 1.0 else ratesMap[baseCurrency] ?: 1.0

            fun convertToBase(amount: Long, from: String): Long {
                if (from == baseCurrency) return amount
                val fromRate = if (from == "IDR") 1.0 else ratesMap[from] ?: 1.0
                val toRate = baseRate
                if (toRate == 0.0) return amount
                return ((amount * fromRate) / toRate).toLong()
            }

            val liabilityTypeNames = types.filter { it.isLiability }.map { it.name }.toSet()
            val investmentTypeNames = types.filter { it.isInvestment }.map { it.name }.toSet()

            val liquidCash = accounts
                .filter { it.type !in liabilityTypeNames && it.type !in investmentTypeNames }
                .sumOf { convertToBase(it.balance, it.currency) }

            val portfolioValueIdr = holdings.sumOf { it.valueIdr }
            val portfolioValue = if (baseRate > 0) ((portfolioValueIdr / baseRate)).toLong() else 0L

            val currentMonthlyIncome = if (baseRate > 0) (((currentInc ?: 0L).toDouble() / baseRate)).toLong() else 0L
            val baselineMonthlyBurn = if (baseRate > 0) (((total90dExp ?: 0L).toDouble() / baseRate) / 3.0).toLong() else 0L

            val baselineRunwayMonths = if (baselineMonthlyBurn > 0L) {
                (liquidCash.toDouble() / baselineMonthlyBurn.toDouble()).coerceAtMost(99.0)
            } else {
                if (liquidCash > 0L) 99.0 else 0.0
            }
            val baselineTier = SafetyBufferTier.fromRunway(baselineRunwayMonths)

            // 1. Zero Income (Complete Job Loss)
            val zeroIncomeBurn = baselineMonthlyBurn
            val zeroIncomeRunway = if (zeroIncomeBurn > 0) (liquidCash.toDouble() / zeroIncomeBurn.toDouble()).coerceAtMost(99.0) else (if (liquidCash > 0) 99.0 else 0.0)
            val zeroIncomeScenario = StressTestScenario(
                type = StressTestScenarioType.ZERO_INCOME,
                title = "Total Income Halt (Job Loss)",
                description = "100% income loss; liquid reserves sustain baseline living expenses.",
                monthlyBurn = zeroIncomeBurn,
                netMonthlyCashFlow = -zeroIncomeBurn,
                runwayMonths = zeroIncomeRunway,
                tier = SafetyBufferTier.fromRunway(zeroIncomeRunway)
            )

            // 2. Partial Income (50% cut with 25% trimmed expenses)
            val partialIncome = (currentMonthlyIncome * 0.50).toLong()
            val trimmedBurn = (baselineMonthlyBurn * 0.75).toLong()
            val partialNetDeficit = (trimmedBurn - partialIncome).coerceAtLeast(0L)
            val partialRunway = if (partialNetDeficit > 0) {
                (liquidCash.toDouble() / partialNetDeficit.toDouble()).coerceAtMost(99.0)
            } else {
                99.0
            }
            val partialIncomeScenario = StressTestScenario(
                type = StressTestScenarioType.PARTIAL_INCOME,
                title = "50% Pay Cut & Essential Budget",
                description = "50% income drop with discretionary spending cut by 25%.",
                monthlyBurn = trimmedBurn,
                netMonthlyCashFlow = partialIncome - trimmedBurn,
                runwayMonths = partialRunway,
                tier = SafetyBufferTier.fromRunway(partialRunway)
            )

            // 3. Inflation & Emergency Surge (+25% burn)
            val surgeBurn = (baselineMonthlyBurn * 1.25).toLong()
            val surgeRunway = if (surgeBurn > 0) (liquidCash.toDouble() / surgeBurn.toDouble()).coerceAtMost(99.0) else (if (liquidCash > 0) 99.0 else 0.0)
            val surgeScenario = StressTestScenario(
                type = StressTestScenarioType.INFLATION_SURGE,
                title = "Emergency Surge (+25% Costs)",
                description = "Cost of living increase or sudden medical/home repair surge.",
                monthlyBurn = surgeBurn,
                netMonthlyCashFlow = currentMonthlyIncome - surgeBurn,
                runwayMonths = surgeRunway,
                tier = SafetyBufferTier.fromRunway(surgeRunway)
            )

            // 4. Market Drawdown (-30% portfolio liquidation buffer)
            val stressedPortfolioBuffer = (portfolioValue * 0.70).toLong()
            val totalStressedReserves = liquidCash + stressedPortfolioBuffer
            val marketDrawdownRunway = if (baselineMonthlyBurn > 0) (totalStressedReserves.toDouble() / baselineMonthlyBurn.toDouble()).coerceAtMost(99.0) else (if (totalStressedReserves > 0) 99.0 else 0.0)
            val marketDrawdownScenario = StressTestScenario(
                type = StressTestScenarioType.MARKET_DRAWDOWN,
                title = "Market Drawdown (-30% Portfolio)",
                description = "Total extended buffer if liquidating equities under a 30% crash.",
                monthlyBurn = baselineMonthlyBurn,
                netMonthlyCashFlow = currentMonthlyIncome - baselineMonthlyBurn,
                runwayMonths = marketDrawdownRunway,
                tier = SafetyBufferTier.fromRunway(marketDrawdownRunway)
            )

            EmergencyFundStressTest(
                liquidCashReserves = liquidCash,
                baselineMonthlyBurn = baselineMonthlyBurn,
                baselineRunwayMonths = baselineRunwayMonths,
                baselineTier = baselineTier,
                scenarios = listOf(zeroIncomeScenario, partialIncomeScenario, surgeScenario, marketDrawdownScenario),
                currencyCode = baseCurrency
            )
        }
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
