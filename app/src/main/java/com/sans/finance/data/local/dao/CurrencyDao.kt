package com.sans.finance.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sans.finance.data.local.entity.ExchangeRateEntity
import com.sans.finance.data.local.entity.FxRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM exchange_rates")
    fun getAllRates(): Flow<List<ExchangeRateEntity>>

    @Query("SELECT * FROM exchange_rates WHERE code = :code")
    suspend fun getRate(code: String): ExchangeRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<ExchangeRateEntity>)

    // Historical FX Rates
    @Query("SELECT * FROM fx_rates WHERE currency_pair = :currencyPair AND date = :date")
    suspend fun getFxRate(currencyPair: String, date: String): FxRateEntity?

    @Query("SELECT * FROM fx_rates WHERE currency_pair = :currencyPair AND date <= :date ORDER BY date DESC LIMIT 1")
    suspend fun getLatestFxRateOnOrBefore(currencyPair: String, date: String): FxRateEntity?

    @Query("SELECT * FROM fx_rates WHERE currency_pair = :currencyPair ORDER BY date DESC")
    fun getFxRatesForPair(currencyPair: String): Flow<List<FxRateEntity>>

    @Query("SELECT * FROM fx_rates WHERE date = :date")
    suspend fun getFxRatesForDate(date: String): List<FxRateEntity>

    @Query("SELECT * FROM fx_rates")
    fun getAllFxRates(): Flow<List<FxRateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFxRates(rates: List<FxRateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFxRate(rate: FxRateEntity)
}
