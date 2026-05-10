package com.sans.finance.core.util

import java.util.Calendar

object CalendarUtils {
    private val threadLocalCalendar = object : ThreadLocal<Calendar>() {
        override fun initialValue(): Calendar {
            return Calendar.getInstance()
        }
    }

    /**
     * Gets a Calendar instance for the current thread and resets it to the current time.
     * Uses .clone() to ensure distinct instances are returned for multiple calls.
     */
    fun getInstance(): Calendar {
        val cal = threadLocalCalendar.get()!!.clone() as Calendar
        cal.clear()
        cal.timeInMillis = System.currentTimeMillis()
        return cal
    }
}
