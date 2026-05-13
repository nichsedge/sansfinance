package com.sans.finance.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val orderIndex: Int = 0,
    val type: String = "EXPENSE"
)
