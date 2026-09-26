package com.sans.finance.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatterUtils {
    // dd MMM yyyy -> 01 Jan 2024
    private val standardFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        }
    }

    fun getStandardFormatter(): SimpleDateFormat {
        val formatter = standardFormatter.get()!!
        formatter.applyPattern("dd MMM yyyy")
        return formatter
    }

    // EEE, dd MMM yyyy -> Mon, 01 Jan 2024
    private val fullHumanReadableFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        }
    }

    fun getFullHumanReadableFormatter(): SimpleDateFormat {
        val formatter = fullHumanReadableFormatter.get()!!
        formatter.applyPattern("EEE, dd MMM yyyy")
        return formatter
    }

    // yyyy-MM-dd -> 2024-01-01
    private val isoFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        }
    }

    fun getIsoFormatter(): SimpleDateFormat {
        val formatter = isoFormatter.get()!!
        formatter.applyPattern("yyyy-MM-dd")
        return formatter
    }

    // EEE -> Mon
    private val dayOfWeekFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("EEE", Locale.getDefault())
        }
    }

    fun getDayOfWeekFormatter(): SimpleDateFormat {
        val formatter = dayOfWeekFormatter.get()!!
        formatter.applyPattern("EEE")
        return formatter
    }

    // d MMM -> 1 Jan
    private val dayMonthFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("d MMM", Locale.getDefault())
        }
    }

    fun getDayMonthFormatter(): SimpleDateFormat {
        val formatter = dayMonthFormatter.get()!!
        formatter.applyPattern("d MMM")
        return formatter
    }

    // MMM yy -> Jan 24
    private val monthYearFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("MMM yy", Locale.getDefault())
        }
    }

    fun getMonthYearFormatter(): SimpleDateFormat {
        val formatter = monthYearFormatter.get()!!
        formatter.applyPattern("MMM yy")
        return formatter
    }

    // yyyy -> 2024
    private val yearFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("yyyy", Locale.getDefault())
        }
    }

    fun getYearFormatter(): SimpleDateFormat {
        val formatter = yearFormatter.get()!!
        formatter.applyPattern("yyyy")
        return formatter
    }

    // Helper formatting methods
    fun formatStandardDate(dateMillis: Long): String = getStandardFormatter().format(Date(dateMillis))
    fun formatStandardDate(date: Date): String = getStandardFormatter().format(date)

    fun formatFullDate(dateMillis: Long): String = getFullHumanReadableFormatter().format(Date(dateMillis))
    fun formatFullDate(date: Date): String = getFullHumanReadableFormatter().format(date)

    fun formatIsoDate(dateMillis: Long): String = getIsoFormatter().format(Date(dateMillis))
    fun formatIsoDate(date: Date): String = getIsoFormatter().format(date)

    fun formatDayOfWeek(dateMillis: Long): String = getDayOfWeekFormatter().format(Date(dateMillis))
    fun formatDayOfWeek(date: Date): String = getDayOfWeekFormatter().format(date)

    fun formatDayMonth(dateMillis: Long): String = getDayMonthFormatter().format(Date(dateMillis))
    fun formatDayMonth(date: Date): String = getDayMonthFormatter().format(date)

    fun formatMonthYear(dateMillis: Long): String = getMonthYearFormatter().format(Date(dateMillis))
    fun formatMonthYear(date: Date): String = getMonthYearFormatter().format(date)

    fun formatFullMonthYear(dateMillis: Long): String {
        return java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
            .format(java.time.Instant.ofEpochMilli(dateMillis).atZone(java.time.ZoneId.systemDefault()))
    }
}
