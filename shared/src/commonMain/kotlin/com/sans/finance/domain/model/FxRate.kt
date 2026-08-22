package com.sans.finance.domain.model

data class FxRate(
    val currencyPair: String,
    val date: String,
    val rate: Double,
    val createdAt: Long = 0L
)
