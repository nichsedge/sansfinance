package com.sans.finance.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ExpenseWithTags(
    @Embedded val expense: ExpenseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ExpenseTagCrossRef::class,
            parentColumn = "expenseId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "expense_id"
    )
    val installment: InstallmentEntity?
)
