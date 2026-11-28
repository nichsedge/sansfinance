package com.sans.finance.domain.usecase

import com.sans.finance.core.util.RecurringOccurrenceCalculator
import com.sans.finance.data.local.dao.InstallmentDao
import com.sans.finance.domain.model.DailySafeToSpend
import com.sans.finance.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import javax.inject.Inject

class GetCashFlowPacingUseCase @Inject constructor(
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase,
    private val installmentDao: InstallmentDao,
    private val expenseRepository: ExpenseRepository,
    private val currencyDao: com.sans.finance.data.local.dao.CurrencyDao
) {
    operator fun invoke(): Flow<DailySafeToSpend> {
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val remainingDays = (maxDays - currentDay + 1).coerceAtLeast(1)

        cal.set(Calendar.DAY_OF_MONTH, maxDays)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val cycleEndMs = cal.timeInMillis
        val now = System.currentTimeMillis()

        return combine(
            getDashboardSummaryUseCase(),
            installmentDao.getAllPendingItems(),
            installmentDao.getActiveInstallments(),
            expenseRepository.getRecurringExpenses(),
            currencyDao.getAllRates()
        ) { summary, pendingItems, activeInstallments, recurringList, rates ->
            val remainingBudget = (summary.globalBudget - summary.globalSpent).coerceAtLeast(0L)
            val baseCurrency = summary.currentCurrency
            val ratesMap = rates.associate { it.code to it.rateToIdr }
            val baseRate = if (baseCurrency == "IDR") 1.0 else (ratesMap[baseCurrency] ?: 1.0)
            val installmentCurrencyMap = activeInstallments.associate { it.installment.id to it.expense.currency }

            fun convertToBase(amount: Long, from: String?): Long {
                if (from.isNullOrBlank() || from == baseCurrency) return amount
                val fromRate = if (from == "IDR") 1.0 else (ratesMap[from] ?: 1.0)
                val toRate = baseRate
                if (toRate == 0.0) return amount
                return ((amount * fromRate) / toRate).toLong()
            }

            // Find pending installment items due before cycleEndMs
            val committedInstallments = pendingItems
                .filter { it.dueDate <= cycleEndMs }
                .sumOf { convertToBase(it.amount, installmentCurrencyMap[it.installmentId]) }

            // Find pending recurring bills due between now and cycleEndMs
            val committedRecurring = recurringList
                .filter { it.type == "EXPENSE" }
                .sumOf { rule ->
                    val occurrences = RecurringOccurrenceCalculator.calculateOccurrences(
                        startDate = rule.date,
                        interval = rule.recurrenceInterval,
                        multiplier = rule.recurrenceIntervalMultiplier,
                        endType = rule.recurrenceEndType,
                        endDate = rule.recurrenceEndDate,
                        totalOccurrences = rule.recurrenceTotalOccurrences,
                        status = rule.recurrenceStatus,
                        since = now,
                        until = cycleEndMs + 1
                    )
                    val ruleTotal = occurrences.size.toLong() * rule.amount
                    convertToBase(ruleTotal, rule.currency)
                }

            val committedBills = committedInstallments + committedRecurring
            val netDiscretionary = (remainingBudget - committedBills).coerceAtLeast(0L)
            val dailyAllowance = netDiscretionary / remainingDays

            DailySafeToSpend(
                dailyAllowance = dailyAllowance,
                remainingDaysInCycle = remainingDays,
                remainingBudget = remainingBudget,
                committedUpcomingBills = committedBills,
                netDiscretionaryRemaining = netDiscretionary,
                pacingPcnt = summary.spendingVelocity,
                currencyCode = summary.currentCurrency
            )
        }
    }
}
