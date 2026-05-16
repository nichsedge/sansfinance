package com.sans.finance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "account_aliases")
data class AccountAliasEntity(
    @PrimaryKey val accountKey: String,
    val aliasName: String,
    val updatedAt: Long = System.currentTimeMillis()
)
