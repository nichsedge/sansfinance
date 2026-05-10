package com.sans.finance.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "expense_tag_ref",
    primaryKeys = ["expenseId", "tagId"],
    indices = [
        Index(value = ["expenseId"]),
        Index(value = ["tagId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExpenseTagCrossRef(
    val expenseId: Long,
    val tagId: Long
)
