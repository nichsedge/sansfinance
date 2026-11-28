package com.sans.finance.core.util

object CurrencyConverter {
    fun convert(
        amount: Long,
        fromCurrency: String?,
        toCurrency: String,
        ratesMap: Map<String, Double>
    ): Long {
        val from = fromCurrency?.trim()?.uppercase()
        val to = toCurrency.trim().uppercase()
        if (from.isNullOrBlank() || from == to) return amount
        val fromRate = if (from == "IDR") 1.0 else (ratesMap[from] ?: 1.0)
        val toRate = if (to == "IDR") 1.0 else (ratesMap[to] ?: 1.0)
        if (toRate == 0.0) return amount
        return ((amount * fromRate) / toRate).toLong()
    }

    fun convertFromIdr(
        amountInIdr: Long,
        toCurrency: String,
        ratesMap: Map<String, Double>
    ): Long = convert(amountInIdr, "IDR", toCurrency, ratesMap)
}
