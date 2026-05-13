package com.sans.finance.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Tag(
    val id: Long = 0,
    val name: String,
    val orderIndex: Int = 0
)
