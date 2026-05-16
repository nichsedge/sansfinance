package com.sans.finance.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "portfolio_holdings",
    indices = [Index(value = ["snapshot_date"]), Index(value = ["account_id"]), Index(value = ["account_key"])],
    foreignKeys = [
        ForeignKey(
            entity = PortfolioSnapshotHeaderEntity::class,
            parentColumns = ["snapshotDate"],
            childColumns = ["snapshot_date"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.SET_NULL
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
    val quantity: Double,
    val price: Double?,
    @ColumnInfo(name = "value_idr") val valueIdr: Double,
    @ColumnInfo(name = "asset_class") val assetClass: String,
    @ColumnInfo(name = "account_id") val accountId: Long? = null,
    @ColumnInfo(name = "account_key") val accountKey: String? = null,
    @ColumnInfo(name = "account_name") val accountName: String? = null,
    val account: String,
    val details: String?
)
