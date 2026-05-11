package com.sans.finance.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "portfolio_holdings",
    indices = [Index(value = ["snapshot_date"])],
    foreignKeys = [
        ForeignKey(
            entity = PortfolioSnapshotHeaderEntity::class,
            parentColumns = ["snapshotDate"],
            childColumns = ["snapshot_date"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PortfolioHoldingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "snapshot_date") val snapshotDate: Long, // FK to header
    val source: String,
    val category: String,
    val asset: String,
    val currency: String,
    val amount: Double,
    val price: Double?,
    @ColumnInfo(name = "value_idr") val valueIdr: Double,
    val account: String,
    val details: String?
)
