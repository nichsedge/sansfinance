package com.sans.finance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portfolio_snapshot_headers")
data class PortfolioSnapshotHeaderEntity(
    @PrimaryKey val snapshotDate: Long, // epoch millis (start of day)
    val exchangeRateUsd: Double,        // IDR per USD at this date
    val totalValueIdr: Double,          // Pre-calculated total for faster history queries
    val totalValueUsd: Double,          // Pre-calculated total
    val createdAt: Long = System.currentTimeMillis()
)
