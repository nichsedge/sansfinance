package com.sans.finance.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "installments",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expense_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["expense_id"])]
)
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "expense_id") val expenseId: Long,
    @ColumnInfo(name = "total_amount") val totalAmount: Long,
    @ColumnInfo(name = "monthly_payment") val monthlyPayment: Long,
    @ColumnInfo(name = "duration_months") val durationMonths: Int,
    @ColumnInfo(name = "remaining_balance") val remainingBalance: Long,
    @ColumnInfo(name = "next_due_date") val nextDueDate: Long,
    val status: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
