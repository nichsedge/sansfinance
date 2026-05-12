package com.sans.finance.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.data.local.entity.PortfolioSnapshotHeaderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {

    @Query("SELECT * FROM portfolio_holdings WHERE snapshot_date = :date ORDER BY category, asset")
    fun getSnapshotByDate(date: Long): Flow<List<PortfolioHoldingEntity>>

    @Query("SELECT * FROM portfolio_holdings WHERE snapshot_date = :date ORDER BY category, asset")
    suspend fun getSnapshotByDateSync(date: Long): List<PortfolioHoldingEntity>

    @Query(
        """
        SELECT * FROM portfolio_holdings 
        WHERE snapshot_date = (SELECT MAX(snapshotDate) FROM portfolio_snapshot_headers) 
        ORDER BY category, asset
    """
    )
    fun getLatestSnapshot(): Flow<List<PortfolioHoldingEntity>>

    @Query("SELECT * FROM portfolio_snapshot_headers ORDER BY snapshotDate DESC LIMIT 1")
    fun getLatestSnapshotHeader(): Flow<PortfolioSnapshotHeaderEntity?>

    @Query("SELECT snapshotDate FROM portfolio_snapshot_headers ORDER BY snapshotDate DESC")
    fun getAllSnapshotDates(): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM portfolio_snapshot_headers")
    suspend fun getSnapshotCount(): Int

    @Query(
        """
        SELECT category, SUM(value_idr) as totalIdr, 0.0 as totalUsd
        FROM portfolio_holdings
        WHERE snapshot_date = :date
        GROUP BY category
        ORDER BY totalIdr DESC
    """
    )
    suspend fun getCategoryTotals(date: Long): List<CategoryTotal>

    @Query(
        """
        SELECT snapshotDate as snapshot_date, totalValueIdr as totalIdr, totalValueUsd as totalUsd
        FROM portfolio_snapshot_headers
        ORDER BY snapshotDate ASC
    """
    )
    fun getTotalValueOverTime(): Flow<List<SnapshotTotal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeader(header: PortfolioSnapshotHeaderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoldings(items: List<PortfolioHoldingEntity>)

    @Query("DELETE FROM portfolio_snapshot_headers WHERE snapshotDate = :date")
    suspend fun deleteByDate(date: Long)

    @Query("DELETE FROM portfolio_snapshot_headers")
    suspend fun deleteAll()

    @Query(
        """
        SELECT asset_class as assetClass, SUM(value_idr) as totalIdr
        FROM portfolio_holdings
        WHERE snapshot_date = :date
        GROUP BY asset_class
        ORDER BY totalIdr DESC
    """
    )
    suspend fun getAssetClassTotals(date: Long): List<AssetClassTotal>
}

data class CategoryTotal(
    val category: String,
    val totalIdr: Double,
    val totalUsd: Double
)

data class AssetClassTotal(
    val assetClass: String,
    val totalIdr: Double
)

data class SnapshotTotal(
    val snapshot_date: Long,
    val totalIdr: Double,
    val totalUsd: Double
)
