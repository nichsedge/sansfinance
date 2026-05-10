package com.sans.expensetracker.core.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil
import java.text.DecimalFormat

object CurrencyFormatter {
    private val locale = Locale.of("id", "ID")

    private val threadLocalFormatter = object : ThreadLocal<NumberFormat>() {
        override fun initialValue(): NumberFormat {
            val formatter = NumberFormat.getCurrencyInstance(locale)
            formatter.isGroupingUsed = true
            formatter.maximumFractionDigits = 0
            return formatter
        }
    }

    /**
     * Formats the amount in cents (Long) into a display string.
     * Rounds up to the nearest whole number and removes thousands separators.
     */
    fun formatAmount(amountInCents: Long): String {
        val amount = ceil(amountInCents / 100.0).toLong()
        val formatter = threadLocalFormatter.get()!!

        // This will include the currency symbol and thousands separator but NO decimal part.
        return formatter.format(amount)
    }

    /**
     * Formats the amount concisely (e.g. 10K, 1M) for UI charts or dense views.
     */
    fun formatAmountCompact(amountInCents: Long): String {
        val amount = ceil(amountInCents / 100.0).toLong()
        if (amount == 0L) return "Rp0"

        val isNegative = amount < 0
        val absAmount = kotlin.math.abs(amount)
        val prefix = if (isNegative) "-Rp" else "Rp"

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
