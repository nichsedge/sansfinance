package com.sans.finance.core.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil

object CurrencyFormatter {
    private val formatters = object : ThreadLocal<MutableMap<String, NumberFormat>>() {
        override fun initialValue(): MutableMap<String, NumberFormat> = HashMap()
    }

    private val compactDecimalFormatter = object : ThreadLocal<DecimalFormat>() {
        override fun initialValue(): DecimalFormat = DecimalFormat("#.#")
    }

    private val symbolCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    private fun getFormatter(currencyCode: String = "USD"): NumberFormat {
        val map = formatters.get() ?: HashMap<String, NumberFormat>().also { formatters.set(it) }
        return map.getOrPut(currencyCode) {
            val locale = when (currencyCode) {
                "IDR" -> Locale.forLanguageTag("id-ID")
                "SGD" -> Locale.forLanguageTag("en-SG")
                "CNY" -> Locale.CHINA
                "USD" -> Locale.US
                "EUR" -> Locale.GERMANY
                "GBP" -> Locale.UK
                "JPY" -> Locale.JAPAN
                "KRW" -> Locale.KOREA
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
            formatter.maximumFractionDigits = if (isZeroDecimalCurrency(currencyCode)) 0 else 2
            formatter
        }
    }

    private fun isZeroDecimalCurrency(currencyCode: String): Boolean {
        return currencyCode in setOf("IDR", "JPY", "KRW", "VND")
    }

    /**
     * Formats the amount in cents (Long) into a display string.
     * Zero-decimal currencies (IDR, JPY, KRW, VND) round up to whole units;
     * decimal currencies (USD, SGD, EUR, etc.) preserve up to 2 decimal places.
     */
    fun formatAmount(amountInCents: Long, currencyCode: String = "USD", isPrivacyMode: Boolean = false): String {
        if (isPrivacyMode) {
            val symbol = if (currencyCode == "IDR") "Rp " else "$"
            return "$symbol••••••••"
        }
        val formatter = getFormatter(currencyCode)
        var formatted = if (isZeroDecimalCurrency(currencyCode)) {
            val amount = ceil(amountInCents / 100.0).toLong()
            formatter.format(amount)
        } else {
            val hasFraction = amountInCents % 100 != 0L
            formatter.minimumFractionDigits = if (hasFraction) 2 else 0
            formatter.maximumFractionDigits = 2
            formatter.format(amountInCents / 100.0)
        }

        // Special case for IDR if it still shows IDR instead of Rp in some locales
        if (currencyCode == "IDR" && !formatted.contains("Rp")) {
            formatted = formatted.replace("IDR", "Rp").replace("ID", "Rp")
        }

        return formatted
    }

    /**
     * Formats the amount concisely (e.g. 10K, 1M) for UI charts or dense views.
     */
    fun formatAmountCompact(amountInCents: Long, currencyCode: String = "USD"): String {
        val symbol = symbolCache.getOrPut(currencyCode) {
            try {
                val currency = java.util.Currency.getInstance(currencyCode)
                val displayLocale = when (currencyCode) {
                    "IDR" -> Locale.forLanguageTag("id-ID")
                    "SGD" -> Locale.forLanguageTag("en-SG")
                    "CNY" -> Locale.CHINA
                    "USD" -> Locale.US
                    "EUR" -> Locale.GERMANY
                    "GBP" -> Locale.UK
                    "JPY" -> Locale.JAPAN
                    else -> Locale.getDefault()
                }
                val sym = currency.getSymbol(displayLocale)
                if (currencyCode == "IDR" && sym == "IDR") "Rp" else sym
            } catch (e: Exception) {
                currencyCode
            }
        }

        if (amountInCents == 0L) return "${symbol}0"

        val isZeroDecimal = isZeroDecimalCurrency(currencyCode)
        val exactAmount = if (isZeroDecimal) ceil(amountInCents / 100.0) else (amountInCents / 100.0)
        val isNegative = exactAmount < 0.0
        val absAmount = kotlin.math.abs(exactAmount)
        val prefix = if (isNegative) "-$symbol" else symbol
        val decimalFormat = compactDecimalFormatter.get() ?: DecimalFormat("#.#")

        return when {
            absAmount >= 1_000_000_000.0 -> "$prefix${decimalFormat.format(absAmount / 1_000_000_000.0)}B"
            absAmount >= 1_000_000.0 -> "$prefix${decimalFormat.format(absAmount / 1_000_000.0)}M"
            absAmount >= 1_000.0 -> "$prefix${decimalFormat.format(absAmount / 1_000.0)}K"
            !isZeroDecimal && amountInCents % 100 != 0L -> "$prefix${DecimalFormat("#.##", java.text.DecimalFormatSymbols(Locale.US)).format(absAmount)}"
            else -> "$prefix${absAmount.toLong()}"
        }
    }

    /**
     * Just the rounded up number without any symbols or separators
     */
    fun formatNumberOnly(amountInCents: Long): String {
        return ceil(amountInCents / 100.0).toLong().toString()
    }
}
