package com.sans.finance.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CategorySpent(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val totalAmount: Long
)
