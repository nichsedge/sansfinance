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
    private val accountDao: com.sans.finance.data.local.dao.AccountDao,
    private val accountTypeDao: com.sans.finance.data.local.dao.AccountTypeDao
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
        val filteredItems = items.filterNot { it.source.equals("SansFinance", ignoreCase = true) }
        val normalizedNewItems = filteredItems.map { item ->
            val accountKey = item.accountKey?.trim().takeUnless { it.isNullOrEmpty() }
            val accountName =
                item.accountName?.trim().takeUnless { it.isNullOrEmpty() }
                    ?: item.account.trim().takeIf { it.isNotEmpty() }

            val linkedAccountId = resolveExistingAccountId(
                accountKey = accountKey,
                accountName = accountName
            )

            item.copy(
                snapshotDate = date,
                accountId = linkedAccountId,
                accountKey = accountKey,
                accountName = accountName,
                account = accountName ?: accountKey ?: item.account
            )
        }

        // --- INTELLIGENT MERGE LOGIC ---
        val existingItems = dao.getSnapshotByDateSync(date)
        val newSources = normalizedNewItems.map { it.source }.toSet()

        // Keep existing items from sources that ARE NOT in the new batch
        val itemsToKeep = existingItems.filter { it.source !in newSources }

        // Combined items
        val finalItems = itemsToKeep + normalizedNewItems

        val totalIdr = finalItems.sumOf { it.valueIdr }
        // --- END MERGE LOGIC ---

        // Estimate exchange rate if not provided (fallback to a reasonable default or calculate from items)
        val rate = exchangeRate ?: finalItems.filter { (it.currency == "USD") && (it.quantity > 0) }
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

        dao.insertSnapshot(header, finalItems)
    }

    private suspend fun resolveExistingAccountId(
        accountKey: String?,
        accountName: String?
    ): Long? {
        if (!accountKey.isNullOrBlank()) {
            val linkedId = dao.findLinkedAccountIdByKey(accountKey)
            if (linkedId != null) {
                val linkedAccount = accountDao.getAccountById(linkedId)
                if (linkedAccount != null) return linkedId
            }
        }

        if (!accountName.isNullOrBlank()) {
            val byName = accountDao.getAccountByName(accountName)
            if (byName != null) return byName.id
        }

        return null
    }

    override suspend fun deleteByDate(date: Long) =
        dao.deleteByDate(date)

    override suspend fun deleteAll() =
        dao.deleteAll()

    override suspend fun pruneSnapshotsMonthly(): Int {
        val allDates = dao.getAllSnapshotDates().first()
        if (allDates.isEmpty()) return 0

        val calendar = java.util.Calendar.getInstance()
        val monthToLatestDate = mutableMapOf<String, Long>()

        // Group dates by year-month and retain only the latest timestamp per month
        allDates.sorted().forEach { dateMillis ->
            calendar.timeInMillis = dateMillis
            val yearMonth = "${calendar.get(java.util.Calendar.YEAR)}-${calendar.get(java.util.Calendar.MONTH)}"
            monthToLatestDate[yearMonth] = dateMillis
        }

        val keepDates = monthToLatestDate.values.toList()
        val pruneDates = allDates.filter { it !in keepDates }

        if (pruneDates.isNotEmpty()) {
            dao.deleteHeadersExceptDates(keepDates)
            dao.deleteHoldingsExceptDates(keepDates)
        }

        return pruneDates.size
    }

    override fun getPortfolioTargets(): Flow<List<com.sans.finance.data.local.entity.PortfolioTargetEntity>> =
        targetDao.getAllTargets()

    override suspend fun updatePortfolioTarget(target: com.sans.finance.data.local.entity.PortfolioTargetEntity) {
        targetDao.insertTarget(target)
    }

    override suspend fun calculateXirr(endDate: Long): Double {
        val accounts = accountDao.getAllAccounts().first()
        val accountTypes = accountTypeDao.getAllAccountTypes().first()
        val investmentTypeNames = accountTypes.filter { it.isInvestment }.map { it.name.trim().lowercase() }.toSet()

        val investmentAccounts = accounts.filter { account ->
            val lowerType = account.type.trim().lowercase()
            lowerType in investmentTypeNames || lowerType == "investment"
        }.map { it.id }.toSet()
        if (investmentAccounts.isEmpty()) return Double.NaN

        val transferFlows = expenseDao.getTransferCashFlows(investmentAccounts.toList(), endDate)
        val cashFlows = mutableListOf<com.sans.finance.core.util.CashFlow>()

        transferFlows.forEach { tx ->
            val fromInv = investmentAccounts.contains(tx.account_id)
            val toInv = tx.to_account_id?.let { investmentAccounts.contains(it) } ?: false

            if (!fromInv && toInv) {
                cashFlows.add(com.sans.finance.core.util.CashFlow(-tx.amount.toDouble(), tx.date))
            } else if (fromInv && !toInv) {
                cashFlows.add(com.sans.finance.core.util.CashFlow(tx.amount.toDouble(), tx.date))
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
