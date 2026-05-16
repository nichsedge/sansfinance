package com.sans.finance

import com.sans.finance.core.util.CalendarUtils
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformanceTest {

    @Test
    fun calendarUtils_returnsDistinctInstancesPerCall() {
        val first = CalendarUtils.getInstance()
        val second = CalendarUtils.getInstance()

        assertNotSame(first, second)
    }

    @Test
    fun calendarUtils_resetsTimeToNowWindow() {
        val before = System.currentTimeMillis()
        val calendar = CalendarUtils.getInstance()
        val after = System.currentTimeMillis()

        assertTrue(calendar.timeInMillis in before..after)
    }
}
