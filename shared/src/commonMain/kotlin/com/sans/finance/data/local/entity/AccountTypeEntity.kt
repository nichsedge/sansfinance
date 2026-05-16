package com.sans.finance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "account_types")
data class AccountTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String, // Icon name (e.g. "AccountBalance")
    val isLiability: Boolean = false,
    @ColumnInfo(name = "display_order") val displayOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
