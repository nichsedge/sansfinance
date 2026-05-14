package com.sans.finance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portfolio_targets")
data class PortfolioTargetEntity(
    @PrimaryKey val assetClass: String,
    val targetPercentage: Double,
    val description: String = "",
    val riskLevel: String = "MEDIUM"
)
