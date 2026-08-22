package com.sans.finance.domain.repository

import com.sans.finance.domain.model.FxRate
import kotlinx.coroutines.flow.Flow

interface CurrencyRepository {
    suspend fun getRateToIdr(code: String): Double?

    suspend fun getHistoricalRate(fromCurrency: String, toCurrency: String, date: String): Double?

    suspend fun getHistoricalRate(fromCurrency: String, toCurrency: String, dateMillis: Long): Double?

    suspend fun backfillHistoricalRates(
        dates: List<String>,
        baseCurrencies: List<String> = listOf("USD", "EUR", "SGD", "IDR")
    )

    fun getFxRatesForPair(currencyPair: String): Flow<List<FxRate>>

    suspend fun saveFxRate(currencyPair: String, date: String, rate: Double)
}
