package com.sans.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "net_worth_snapshots")
data class NetWorthSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long, // Start of day timestamp
    val totalAssets: Long,
    val totalLiabilities: Long,
    val netWorth: Long
)
