package com.sans.finance.domain.usecase

import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.repository.PortfolioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetNetWorthTrendUseCase @Inject constructor(
    private val portfolioRepository: PortfolioRepository,
    private val currencyDao: CurrencyDao,
    private val localeManager: LocaleManager
) {
    operator fun invoke(days: Int = 30): Flow<List<Long>> {
        val baseCurrency = localeManager.getCurrency()

        return combine(
            portfolioRepository.getTotalValueOverTime(),
            currencyDao.getAllRates()
        ) { history, rates ->
            val ratesMap = rates.associate { it.code to it.rateToIdr }
            val baseRate = if (baseCurrency == "IDR") 1.0 else ratesMap[baseCurrency] ?: 1.0

            calculateTrend(history, baseRate, days)
        }
    }

    private fun calculateTrend(history: List<SnapshotTotal>, baseRate: Double, days: Int): List<Long> {
        val now = System.currentTimeMillis()
        val trend = mutableListOf<Long>()
        val sortedHistory = history.sortedBy { it.snapshot_date }
        var historyIdx = sortedHistory.lastIndex

        for (i in 0 until days) {
            val dayStart = now - (i.toLong() * 24 * 60 * 60 * 1000)
            while (historyIdx >= 0 && sortedHistory[historyIdx].snapshot_date > dayStart) {
                historyIdx--
            }
            val dayValueIdr = if (historyIdx >= 0) sortedHistory[historyIdx].totalIdr else 0.0
            val dayValue = if (baseRate > 0) ((dayValueIdr / baseRate) * 100).toLong() else (dayValueIdr * 100).toLong()
            trend.add(dayValue)
        }
        return trend.reversed()
    }
}
