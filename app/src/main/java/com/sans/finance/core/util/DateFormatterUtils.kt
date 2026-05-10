package com.sans.finance.core.util

import java.text.SimpleDateFormat
import java.util.Locale

object DateFormatterUtils {
    // dd MMM yyyy -> 01 Jan 2024
    private val standardFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        }
    }

    fun getStandardFormatter(): SimpleDateFormat {
        return standardFormatter.get()!!
    }

    // d MMM -> 1 Jan
    private val dayMonthFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("d MMM", Locale.getDefault())
        }
    }

    fun getDayMonthFormatter(): SimpleDateFormat {
        return dayMonthFormatter.get()!!
    }

    // MMM yy -> Jan 24
    private val monthYearFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("MMM yy", Locale.getDefault())
        }
    }

    fun getMonthYearFormatter(): SimpleDateFormat {
        return monthYearFormatter.get()!!
    }

    // yyyy -> 2024
    private val yearFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("yyyy", Locale.getDefault())
        }
    }

    fun getYearFormatter(): SimpleDateFormat {
        return yearFormatter.get()!!
    }
}
