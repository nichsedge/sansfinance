package com.sans.finance.domain.repository

import com.sans.finance.data.local.dao.CategoryTotal
import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import kotlinx.coroutines.flow.Flow

interface PortfolioRepository {
    fun getLatestSnapshot(): Flow<List<PortfolioHoldingEntity>>
    fun getSnapshotByDate(date: Long): Flow<List<PortfolioHoldingEntity>>
    suspend fun getSnapshotByDateSync(date: Long): List<PortfolioHoldingEntity>
    fun getAllSnapshotDates(): Flow<List<Long>>
    fun getTotalValueOverTime(): Flow<List<SnapshotTotal>>
    suspend fun getSnapshotCount(): Int
    suspend fun getCategoryTotals(date: Long): List<CategoryTotal>
    suspend fun importSnapshot(date: Long, items: List<PortfolioHoldingEntity>, exchangeRate: Double? = null)
    suspend fun deleteByDate(date: Long)
    suspend fun deleteAll()
}
