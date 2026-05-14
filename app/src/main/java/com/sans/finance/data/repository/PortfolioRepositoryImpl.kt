package com.sans.finance.data.repository

import com.sans.finance.data.local.dao.AssetClassTotal
import com.sans.finance.data.local.dao.CategoryTotal
import com.sans.finance.data.local.dao.PortfolioDao
import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.data.local.entity.PortfolioSnapshotHeaderEntity
import com.sans.finance.domain.repository.PortfolioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class PortfolioRepositoryImpl(
    private val dao: PortfolioDao,
    private val targetDao: com.sans.finance.data.local.dao.PortfolioTargetDao,
    private val expenseDao: com.sans.finance.data.local.dao.ExpenseDao,
    private val accountDao: com.sans.finance.data.local.dao.AccountDao
) : PortfolioRepository {

    override fun getLatestSnapshot(): Flow<List<PortfolioHoldingEntity>> =
        dao.getLatestSnapshot()

    override fun getLatestSnapshotHeader(): Flow<PortfolioSnapshotHeaderEntity?> =
        dao.getLatestSnapshotHeader()

    override fun getSnapshotByDate(date: Long): Flow<List<PortfolioHoldingEntity>> =
        dao.getSnapshotByDate(date)

    override suspend fun getSnapshotByDateSync(date: Long): List<PortfolioHoldingEntity> =
        dao.getSnapshotByDateSync(date)

    override fun getAllSnapshotDates(): Flow<List<Long>> =
        dao.getAllSnapshotDates()

    override fun getTotalValueOverTime(): Flow<List<SnapshotTotal>> =
        dao.getTotalValueOverTime()

    override suspend fun getSnapshotCount(): Int =
        dao.getSnapshotCount()

    override suspend fun getCategoryTotals(date: Long): List<CategoryTotal> =
        dao.getCategoryTotals(date)

    override suspend fun getAssetClassTotals(date: Long): List<AssetClassTotal> =
        dao.getAssetClassTotals(date)

    override suspend fun importSnapshot(
        date: Long,
        items: List<PortfolioHoldingEntity>,
        exchangeRate: Double?
    ) {
        val totalIdr = items.sumOf { it.valueIdr }

        // Estimate exchange rate if not provided (fallback to a reasonable default or calculate from items)
        val rate = exchangeRate ?: items.filter { it.currency == "USD" && it.quantity > 0 }
            .map { it.valueIdr / it.quantity }
            .average()
            .takeIf { !it.isNaN() } ?: 16000.0 // Default fallback

        val totalUsd = totalIdr / rate

        val header = PortfolioSnapshotHeaderEntity(
            snapshotDate = date,
            exchangeRateUsd = rate,
            totalValueIdr = totalIdr,
            totalValueUsd = totalUsd
        )

        dao.insertSnapshot(header, items)
    }

    override suspend fun deleteByDate(date: Long) =
        dao.deleteByDate(date)

    override suspend fun deleteAll() =
        dao.deleteAll()

    override fun getPortfolioTargets(): Flow<List<com.sans.finance.data.local.entity.PortfolioTargetEntity>> =
        targetDao.getAllTargets()

    override suspend fun updatePortfolioTarget(target: com.sans.finance.data.local.entity.PortfolioTargetEntity) {
        targetDao.insertTarget(target)
    }

    override suspend fun calculateXirr(endDate: Long): Double {
        val accounts = accountDao.getAllAccounts().first()
        val investmentAccounts = accounts.filter { it.type == "Investment" }.map { it.id }.toSet()
        if (investmentAccounts.isEmpty()) return Double.NaN

        val transactions = expenseDao.getExpensesBetween(0, endDate).first()
        val cashFlows = mutableListOf<com.sans.finance.core.util.CashFlow>()

        transactions.forEach { tx ->
            val fromInv = investmentAccounts.contains(tx.expense.accountId)
            val toInv = tx.expense.toAccountId?.let { investmentAccounts.contains(it) } ?: false

            if (tx.expense.type == "TRANSFER") {
                if (!fromInv && toInv) {
                    cashFlows.add(com.sans.finance.core.util.CashFlow(-tx.expense.amount.toDouble(), tx.expense.date))
                } else if (fromInv && !toInv) {
                    cashFlows.add(com.sans.finance.core.util.CashFlow(tx.expense.amount.toDouble(), tx.expense.date))
                }
            }
        }

        val latestHeader = dao.getLatestSnapshotHeader().first()
        if (latestHeader != null) {
            cashFlows.add(com.sans.finance.core.util.CashFlow(latestHeader.totalValueIdr, latestHeader.snapshotDate))
        }

        if (cashFlows.size < 2) return Double.NaN
        return com.sans.finance.core.util.XirrCalculator.calculate(cashFlows)
    }
}
