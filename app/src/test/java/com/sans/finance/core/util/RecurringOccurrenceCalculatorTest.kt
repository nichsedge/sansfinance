package com.sans.finance.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RecurringOccurrenceCalculatorTest {

    @Test
    fun testMonthlyRecurrenceIndefinite() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 15, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis

        // Window: Jan 1 to Apr 30 2026
        val windowSince = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val windowUntil = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val occurrences = RecurringOccurrenceCalculator.calculateOccurrences(
            startDate = start,
            interval = "MONTHLY",
            multiplier = 1,
            endType = "NEVER",
            since = windowSince,
            until = windowUntil
        )

        // Should return 4 occurrences: Jan 15, Feb 15, Mar 15, Apr 15
        assertEquals(4, occurrences.size)
        assertEquals(0, occurrences[0].occurrenceIndex)
        assertEquals(1, occurrences[1].occurrenceIndex)
        assertEquals(2, occurrences[2].occurrenceIndex)
        assertEquals(3, occurrences[3].occurrenceIndex)

        val febCal = Calendar.getInstance().apply { timeInMillis = occurrences[1].date }
        assertEquals(Calendar.FEBRUARY, febCal.get(Calendar.MONTH))
        assertEquals(15, febCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testRecurrenceMultiplierQuarterly() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis

        val windowSince = start
        val windowUntil = Calendar.getInstance().apply {
            set(2026, Calendar.DECEMBER, 31, 23, 59, 59)
        }.timeInMillis

        // Every 3 months (Quarterly)
        val occurrences = RecurringOccurrenceCalculator.calculateOccurrences(
            startDate = start,
            interval = "MONTHLY",
            multiplier = 3,
            endType = "NEVER",
            since = windowSince,
            until = windowUntil
        )

        // Jan 1 (0), Apr 1 (1), Jul 1 (2), Oct 1 (3)
        assertEquals(4, occurrences.size)
        val aprCal = Calendar.getInstance().apply { timeInMillis = occurrences[1].date }
        assertEquals(Calendar.APRIL, aprCal.get(Calendar.MONTH))
        val julCal = Calendar.getInstance().apply { timeInMillis = occurrences[2].date }
        assertEquals(Calendar.JULY, julCal.get(Calendar.MONTH))
    }

    @Test
    fun testEndByDateCondition() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 10, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis

        // End on March 15
        val endDate = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 15, 23, 59, 59)
        }.timeInMillis

        val occurrences = RecurringOccurrenceCalculator.calculateOccurrences(
            startDate = start,
            interval = "MONTHLY",
            multiplier = 1,
            endType = "UNTIL_DATE",
            endDate = endDate,
            since = 0L,
            until = Long.MAX_VALUE
        )

        // Jan 10, Feb 10, Mar 10 -> Apr 10 is > endDate, so only 3
        assertEquals(3, occurrences.size)
    }

    @Test
    fun testEndByOccurrenceCount() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis

        val occurrences = RecurringOccurrenceCalculator.calculateOccurrences(
            startDate = start,
            interval = "MONTHLY",
            multiplier = 1,
            endType = "AFTER_COUNT",
            totalOccurrences = 5,
            since = 0L,
            until = Long.MAX_VALUE
        )

        assertEquals(5, occurrences.size)
        assertEquals(4, occurrences.last().occurrenceIndex)
    }

    @Test
    fun testPausedAndCancelledStatus() {
        val start = System.currentTimeMillis()

        val paused = RecurringOccurrenceCalculator.calculateOccurrences(
            startDate = start,
            interval = "MONTHLY",
            status = "PAUSED"
        )
        assertTrue(paused.isEmpty())

        val cancelled = RecurringOccurrenceCalculator.calculateOccurrences(
            startDate = start,
            interval = "MONTHLY",
            status = "CANCELLED"
        )
        assertTrue(cancelled.isEmpty())
    }

    @Test
    fun testSyntheticIdBidirectional() {
        val ruleId = 42L
        val occurrenceIndex = 5

        val syntheticId = RecurringOccurrenceCalculator.generateSyntheticId(ruleId, occurrenceIndex)
        assertTrue(RecurringOccurrenceCalculator.isSyntheticRecurringId(syntheticId))
        assertEquals(ruleId, RecurringOccurrenceCalculator.extractParentRuleId(syntheticId))
        assertEquals(occurrenceIndex, RecurringOccurrenceCalculator.extractOccurrenceIndex(syntheticId))

        assertFalse(RecurringOccurrenceCalculator.isSyntheticRecurringId(42L))
        assertFalse(RecurringOccurrenceCalculator.isSyntheticRecurringId(-5L)) // Installment item ID range
    }

    @Test
    fun testCalculateNextDueDate() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 15, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis

        val referenceTime = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 20, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val nextDue = RecurringOccurrenceCalculator.calculateNextDueDate(
            startDate = start,
            interval = "MONTHLY",
            multiplier = 1,
            endType = "NEVER",
            afterTime = referenceTime
        )

        assertNotNull(nextDue)
        val nextCal = Calendar.getInstance().apply { timeInMillis = nextDue!! }
        assertEquals(Calendar.MARCH, nextCal.get(Calendar.MONTH))
        assertEquals(15, nextCal.get(Calendar.DAY_OF_MONTH))
    }
}
