package com.sans.finance.data.repository

import com.sans.finance.data.local.dao.AssetClassTotal
import com.sans.finance.data.local.dao.CategoryTotal
import com.sans.finance.data.local.dao.PortfolioDao
import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.data.local.entity.PortfolioSnapshotHeaderEntity
import com.sans.finance.domain.repository.PortfolioRepository
import kotlinx.coroutines.flow.Flow

class PortfolioRepositoryImpl(
    private val dao: PortfolioDao
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
        val rate = exchangeRate ?: items.filter { it.currency == "USD" && it.amount > 0 }
            .map { it.valueIdr / it.amount }
            .average()
            .takeIf { !it.isNaN() } ?: 16000.0 // Default fallback

        val totalUsd = totalIdr / rate

        val header = PortfolioSnapshotHeaderEntity(
            snapshotDate = date,
            exchangeRateUsd = rate,
            totalValueIdr = totalIdr,
            totalValueUsd = totalUsd
        )

        dao.deleteByDate(date)
        dao.insertHeader(header)
        dao.insertHoldings(items)
    }

    override suspend fun deleteByDate(date: Long) =
        dao.deleteByDate(date)

    override suspend fun deleteAll() =
        dao.deleteAll()
}
