package com.sans.finance.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long, // timestamp
    val platform: String?,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "note") val note: String,
    val quantity: Int,
    @ColumnInfo(name = "original_price") val originalPrice: Long,
    @ColumnInfo(name = "final_price") val finalPrice: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "account_id") val accountId: Long = 1, // Default to 1 (Cash) for migration
    @ColumnInfo(name = "to_account_id") val toAccountId: Long? = null,
    val type: String = "EXPENSE", // "INCOME", "EXPENSE", "TRANSFER"
    val status: String,
    @ColumnInfo(name = "is_recurring") val isRecurring: Boolean,
    @ColumnInfo(name = "is_installment") val isInstallment: Boolean = false,
    @ColumnInfo(name = "recurrence_interval") val recurrenceInterval: String? = null,
    @ColumnInfo(name = "next_due_date") val nextDueDate: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
