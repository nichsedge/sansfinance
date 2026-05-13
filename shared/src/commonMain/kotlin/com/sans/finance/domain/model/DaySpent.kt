package com.sans.finance.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DaySpent(
    val day: Long,
    val amount: Long
)
