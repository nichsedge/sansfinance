package com.sans.finance.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Thread-safe date formatting helpers backed by immutable [DateTimeFormatter] instances.
 */
object DateFormatterUtils {
    private fun formatter(pattern: String): DateTimeFormatter =
        DateTimeFormatter.ofPattern(pattern, Locale.getDefault())

    private fun formatMillis(dateMillis: Long, pattern: String): String =
        formatter(pattern).format(Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()))

    // dd MMM yyyy -> 01 Jan 2024
    fun formatStandardDate(dateMillis: Long): String = formatMillis(dateMillis, "dd MMM yyyy")
    fun formatStandardDate(date: Date): String = formatStandardDate(date.time)

    // EEE, dd MMM yyyy -> Mon, 01 Jan 2024
    fun formatFullDate(dateMillis: Long): String = formatMillis(dateMillis, "EEE, dd MMM yyyy")
    fun formatFullDate(date: Date): String = formatFullDate(date.time)

    // yyyy-MM-dd -> 2024-01-01
    fun formatIsoDate(dateMillis: Long): String = formatMillis(dateMillis, "yyyy-MM-dd")
    fun formatIsoDate(date: Date): String = formatIsoDate(date.time)

    // EEE -> Mon
    fun formatDayOfWeek(dateMillis: Long): String = formatMillis(dateMillis, "EEE")
    fun formatDayOfWeek(date: Date): String = formatDayOfWeek(date.time)

    // d MMM -> 1 Jan
    fun formatDayMonth(dateMillis: Long): String = formatMillis(dateMillis, "d MMM")
    fun formatDayMonth(date: Date): String = formatDayMonth(date.time)

    // MMM yy -> Jan 24
    fun formatMonthYear(dateMillis: Long): String = formatMillis(dateMillis, "MMM yy")
    fun formatMonthYear(date: Date): String = formatMonthYear(date.time)

    // yyyy -> 2024
    fun formatYear(dateMillis: Long): String = formatMillis(dateMillis, "yyyy")
    fun formatYear(date: Date): String = formatYear(date.time)

    // MMMM yyyy -> January 2024
    fun formatFullMonthYear(dateMillis: Long): String = formatMillis(dateMillis, "MMMM yyyy")
}
