package com.sans.finance.data.local.entity

import androidx.room.ColumnInfo

data class InstallmentPaymentRow(
    val id: Long,
    val date: Long,
    @ColumnInfo(name = "note") val note: String,
    val amount: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "expense_id") val expenseId: Long,
    @ColumnInfo(name = "tags_list") val tagsList: String? = null,
    @ColumnInfo(name = "currency") val currency: String = "USD"
)



