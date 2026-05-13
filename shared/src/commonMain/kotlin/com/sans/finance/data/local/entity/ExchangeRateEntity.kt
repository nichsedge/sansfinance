package com.sans.finance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val code: String, // e.g. "USD", "IDR"
    val rateToIdr: Double,        // Base currency is IDR for this app
    val updatedAt: Long = System.currentTimeMillis()
)
