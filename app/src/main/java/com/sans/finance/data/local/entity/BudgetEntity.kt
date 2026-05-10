package com.sans.finance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val period: String = "MONTHLY", // MONTHLY, WEEKLY, YEARLY
    val createdAt: Long = System.currentTimeMillis()
)
