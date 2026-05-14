package com.sans.finance.core.util

import java.util.*
import kotlin.math.pow

data class CashFlow(
    val amount: Double,
    val date: Long // epoch millis
)

object XirrCalculator {

    private const val MAX_ITERATIONS = 100
    private const val PRECISION = 1e-7

    /**
     * Calculates the Extended Internal Rate of Return (XIRR).
     * @param flows List of cash flows. Negative for outflows (investments), positive for inflows (returns).
     * @return The annual interest rate (e.g., 0.1 for 10%), or Double.NaN if calculation fails to converge.
     */
    fun calculate(flows: List<CashFlow>): Double {
        if (flows.isEmpty()) return Double.NaN

        // Initial guess: 10%
        var x0 = 0.1
        var x1: Double

        for (i in 0 until MAX_ITERATIONS) {
            val fValue = npv(x0, flows)
            val fDerivative = npvDerivative(x0, flows)

            if (fDerivative == 0.0) break

            x1 = x0 - fValue / fDerivative

            if (kotlin.math.abs(x1 - x0) <= PRECISION) {
                return x1
            }
            x0 = x1
        }

        return Double.NaN
    }

    private fun npv(rate: Double, flows: List<CashFlow>): Double {
        val firstDate = flows.minByOrNull { it.date }?.date ?: return 0.0
        return flows.sumOf { flow ->
            val years = (flow.date - firstDate).toDouble() / (1000.0 * 60 * 60 * 24 * 365)
            flow.amount / (1.0 + rate).pow(years)
        }
    }

    private fun npvDerivative(rate: Double, flows: List<CashFlow>): Double {
        val firstDate = flows.minByOrNull { it.date }?.date ?: return 0.0
        return flows.sumOf { flow ->
            val years = (flow.date - firstDate).toDouble() / (1000.0 * 60 * 60 * 24 * 365)
            if (years == 0.0) 0.0
            else -years * flow.amount * (1.0 + rate).pow(-years - 1.0)
        }
    }
}
