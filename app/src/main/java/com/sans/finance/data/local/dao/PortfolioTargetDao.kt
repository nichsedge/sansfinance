package com.sans.finance.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sans.finance.data.local.entity.PortfolioTargetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioTargetDao {
    @Query("SELECT * FROM portfolio_targets")
    fun getAllTargets(): Flow<List<PortfolioTargetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarget(target: PortfolioTargetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargets(targets: List<PortfolioTargetEntity>)

    @Query("DELETE FROM portfolio_targets WHERE assetClass = :assetClass")
    suspend fun deleteTarget(assetClass: String)
}
