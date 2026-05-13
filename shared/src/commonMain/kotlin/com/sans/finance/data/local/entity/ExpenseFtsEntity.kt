package com.sans.finance.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = ExpenseEntity::class)
@Entity(tableName = "expenses_fts")
data class ExpenseFtsEntity(
    val description: String?,
    val note: String
)
