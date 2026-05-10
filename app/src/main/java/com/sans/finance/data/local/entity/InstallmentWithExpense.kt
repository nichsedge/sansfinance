package com.sans.finance.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class InstallmentWithExpense(
    @Embedded val installment: InstallmentEntity,
    @Relation(
        parentColumn = "expense_id",
        entityColumn = "id"
    )
    val expense: ExpenseEntity
)
