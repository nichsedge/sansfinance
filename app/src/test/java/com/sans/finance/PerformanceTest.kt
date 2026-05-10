package com.sans.finance

import org.junit.Test
import kotlin.system.measureTimeMillis

class PerformanceTest {

    @Test
    fun benchmarkCalendarCreation() {
        val duration = 10000
        val startDate = System.currentTimeMillis()

        // Unoptimized
        val unoptimizedTime = measureTimeMillis {
            for (i in 1..duration) {
                val calendar = java.util.Calendar.getInstance()
                calendar.timeInMillis = startDate
                calendar.add(java.util.Calendar.MONTH, i - 1)
                calendar.timeInMillis
            }
        }

        // Optimized
        val optimizedTime = measureTimeMillis {
            val calendar = java.util.Calendar.getInstance()
            for (i in 1..duration) {
                calendar.timeInMillis = startDate
                calendar.add(java.util.Calendar.MONTH, i - 1)
                calendar.timeInMillis
            }
        }

        println("Unoptimized time: $unoptimizedTime ms")
        println("Optimized time: $optimizedTime ms")
        assert(optimizedTime < unoptimizedTime)
    }
}
