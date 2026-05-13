package com.sans.finance.data.local.entity

import androidx.room.ColumnInfo

data class InstallmentPaymentRow(
    val id: Long,
    val date: Long,
    @ColumnInfo(name = "title") val title: String,
    val amount: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "details") val details: String?,
    @ColumnInfo(name = "expense_id") val expenseId: Long,
    @ColumnInfo(name = "account_id") val accountId: Long = 1,
    @ColumnInfo(name = "month_number") val monthNumber: Int = 0,
    @ColumnInfo(name = "total_months") val totalMonths: Int = 0,
    @ColumnInfo(name = "status") val status: String = "Paid",
    @ColumnInfo(name = "tags_list") val tagsList: String? = null,
    @ColumnInfo(name = "currency") val currency: String = "USD",
    @ColumnInfo(name = "category_name") val categoryName: String? = null,
    @ColumnInfo(name = "category_icon") val categoryIcon: String? = null
)



