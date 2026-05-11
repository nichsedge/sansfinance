package com.sans.finance.core.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil

object CurrencyFormatter {
    private fun getFormatter(currencyCode: String = "USD"): NumberFormat {
        val locale = when (currencyCode) {
            "IDR" -> Locale.Builder().setLanguage("id").setRegion("ID").build()
            "CNY" -> Locale.SIMPLIFIED_CHINESE
            "USD" -> Locale.US
            else -> Locale.getDefault()
        }
        val formatter = NumberFormat.getCurrencyInstance(locale)
        try {
            val currency = java.util.Currency.getInstance(currencyCode)
            formatter.currency = currency
        } catch (e: Exception) {
            // Fallback
        }
        formatter.isGroupingUsed = true
        formatter.maximumFractionDigits = 0
        return formatter
    }

    /**
     * Formats the amount in cents (Long) into a display string.
     * Rounds up to the nearest whole number and removes thousands separators.
     */
    fun formatAmount(amountInCents: Long, currencyCode: String = "USD"): String {
        val amount = ceil(amountInCents / 100.0).toLong()
        val formatter = getFormatter(currencyCode)

        // This will include the currency symbol and thousands separator but NO decimal part.
        return formatter.format(amount)
    }

    /**
     * Formats the amount concisely (e.g. 10K, 1M) for UI charts or dense views.
     */
    fun formatAmountCompact(amountInCents: Long, currencyCode: String = "USD"): String {
        val amount = ceil(amountInCents / 100.0).toLong()
        val symbol = try {
            java.util.Currency.getInstance(currencyCode).getSymbol(Locale.getDefault())
        } catch (e: Exception) {
            "Rp"
        }

        if (amount == 0L) return "${symbol}0"

        val isNegative = amount < 0
        val absAmount = kotlin.math.abs(amount)
        val prefix = if (isNegative) "-$symbol" else symbol

        return when {
            absAmount >= 1_000_000_000L -> "$prefix${DecimalFormat("#.#").format(absAmount / 1_000_000_000.0)}B"
            absAmount >= 1_000_000L -> "$prefix${DecimalFormat("#.#").format(absAmount / 1_000_000.0)}M"
            absAmount >= 1_000L -> "$prefix${DecimalFormat("#.#").format(absAmount / 1_000.0)}K"
            else -> "$prefix$absAmount"
        }
    }

    /**
     * Just the rounded up number without any symbols or separators
     */
    fun formatNumberOnly(amountInCents: Long): String {
        return ceil(amountInCents / 100.0).toLong().toString()
    }
}
