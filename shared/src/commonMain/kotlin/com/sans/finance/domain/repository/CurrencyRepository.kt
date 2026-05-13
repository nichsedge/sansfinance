package com.sans.finance.domain.repository

interface CurrencyRepository {
    suspend fun getRateToIdr(code: String): Double?
}
