package com.sans.finance.domain.model

data class CategorySpent(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val totalAmount: Long
)
