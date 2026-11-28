package com.sans.finance.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateFormatterUtilsTest {

    @Test
    fun testDateFormatting() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 30, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val millis = cal.timeInMillis

        val iso = DateFormatterUtils.formatIsoDate(millis)
        assertEquals("2026-08-30", iso)

        val dayOfWeek = DateFormatterUtils.formatDayOfWeek(millis)
        assertEquals(DateFormatterUtils.formatDayOfWeek(cal.time), dayOfWeek)

        val standard = DateFormatterUtils.formatStandardDate(millis)
        assertTrue(standard.contains("2026"))
        assertTrue(standard.contains("30"))

        val full = DateFormatterUtils.formatFullDate(millis)
        assertTrue(full.contains("2026"))

        val year = DateFormatterUtils.formatYear(millis)
        assertEquals("2026", year)

        val monthYear = DateFormatterUtils.formatMonthYear(millis)
        assertTrue(monthYear.contains("26"))

        val dayMonth = DateFormatterUtils.formatDayMonth(millis)
        assertTrue(dayMonth.contains("30"))
    }
}
