package com.sans.finance.domain.usecase

import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.core.util.DateFormatterUtils
import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.model.MomentumTrend
import com.sans.finance.domain.model.MonthlySavingsPoint
import com.sans.finance.domain.model.SavingsRateVelocitySummary
import com.sans.finance.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

class GetSavingsRateVelocityUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val localeManager: LocaleManager,
    private val currencyDao: CurrencyDao
) {
    operator fun invoke(): Flow<SavingsRateVelocitySummary> {
        val baseCurrency = localeManager.getCurrency()

        // Generate timestamps for last 6 full/current months
        val cal = CalendarUtils.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val currentMonthStart = cal.timeInMillis
        cal.add(Calendar.MONTH, -5)
        val sixMonthsAgoStart = cal.timeInMillis

        cal.timeInMillis = currentMonthStart
        cal.add(Calendar.MONTH, 1)
        val nextMonthStart = cal.timeInMillis

        return combine(
            expenseRepository.getExpensesBetween(sixMonthsAgoStart, nextMonthStart),
            currencyDao.getAllRates()
        ) { expenses, rates ->
            val ratesMap = rates.associate { it.code to it.rateToIdr }
            val baseRate = if (baseCurrency == "IDR") 1.0 else ratesMap[baseCurrency] ?: 1.0

            fun convertToBase(amount: Long, from: String): Long {
                if (from == baseCurrency || baseRate == 0.0) return amount
                val fromRate = if (from == "IDR") 1.0 else ratesMap[from] ?: 1.0
                return ((amount * fromRate) / baseRate).toLong()
            }

            val monthPoints = mutableListOf<MonthlySavingsPoint>()
            val monthCal = CalendarUtils.getInstance()

            for (i in 5 downTo 0) {
                monthCal.timeInMillis = currentMonthStart
                monthCal.add(Calendar.MONTH, -i)
                val mStart = monthCal.timeInMillis
                val mLabel = DateFormatterUtils.formatMonthYear(mStart)

                monthCal.add(Calendar.MONTH, 1)
                val mEnd = monthCal.timeInMillis

                val monthExpenses = expenses.filter { it.date in mStart until mEnd }
                val income = monthExpenses
                    .filter { it.type == "INCOME" }
                    .sumOf { convertToBase(it.amount, it.currency) }
                val expense = monthExpenses
                    .filter { it.type == "EXPENSE" }
                    .sumOf { convertToBase(it.amount, it.currency) }
                val savings = income - expense
                val ratePct = if (income > 0) {
                    ((savings.toDouble() / income.toDouble()) * 100.0).coerceIn(-100.0, 100.0)
                } else {
                    if (expense > 0) -100.0 else 0.0
                }

                monthPoints.add(
                    MonthlySavingsPoint(
                        monthLabel = mLabel,
                        yearMonthTimestamp = mStart,
                        income = income,
                        expense = expense,
                        savings = savings,
                        savingsRatePct = ratePct
                    )
                )
            }

            val currentMonthPoint = monthPoints.lastOrNull()
            val currentRate = currentMonthPoint?.savingsRatePct ?: 0.0

            val last3 = monthPoints.takeLast(3)
            val threeMonthAvgRate = if (last3.isNotEmpty()) last3.map { it.savingsRatePct }.average() else 0.0
            val sixMonthAvgRate = if (monthPoints.isNotEmpty()) monthPoints.map { it.savingsRatePct }.average() else 0.0

            val monthlyVelocity = if (last3.isNotEmpty()) (last3.map { it.savings }.average()).toLong() else 0L

            val momentum = when {
                currentRate > threeMonthAvgRate + 3.0 -> MomentumTrend.ACCELERATING
                currentRate < threeMonthAvgRate - 3.0 -> MomentumTrend.DECELERATING
                else -> MomentumTrend.STEADY
            }

            SavingsRateVelocitySummary(
                currentMonthSavingsRatePct = currentRate,
                threeMonthAvgSavingsRatePct = threeMonthAvgRate,
                sixMonthAvgSavingsRatePct = sixMonthAvgRate,
                monthlyNetWorthVelocity = monthlyVelocity,
                momentumTrend = momentum,
                history = monthPoints,
                currencyCode = baseCurrency
            )
        }
    }
}
