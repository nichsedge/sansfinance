package com.sans.finance.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "fx_rates",
    primaryKeys = ["currency_pair", "date"]
)
data class FxRateEntity(
    @ColumnInfo(name = "currency_pair") val currencyPair: String, // e.g. "USD/IDR", "EUR/USD"
    @ColumnInfo(name = "date") val date: String,                   // "yyyy-MM-dd"
    @ColumnInfo(name = "rate") val rate: Double,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
