package com.sans.finance.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MathExpressionEvaluatorTest {

    @Test
    fun testSimpleNumbers() {
        assertEquals(50000.0, MathExpressionEvaluator.evaluate("50000")!!, 0.001)
        assertEquals(5000000L, MathExpressionEvaluator.evaluateToCents("50000"))
        assertFalse(MathExpressionEvaluator.hasArithmetic("50000"))
    }

    @Test
    fun testAddition() {
        assertTrue(MathExpressionEvaluator.hasArithmetic("50000 + 12000"))
        assertEquals(62000.0, MathExpressionEvaluator.evaluate("50000 + 12000")!!, 0.001)
        assertEquals(6200000L, MathExpressionEvaluator.evaluateToCents("50000 + 12000"))
    }

    @Test
    fun testSubtraction() {
        assertEquals(35000.0, MathExpressionEvaluator.evaluate("50000 - 15000")!!, 0.001)
    }

    @Test
    fun testMultiplicationAndDivision() {
        assertEquals(135000.0, MathExpressionEvaluator.evaluate("150000 * 0.9")!!, 0.001)
        assertEquals(135000.0, MathExpressionEvaluator.evaluate("150000 x 0.9")!!, 0.001)
        assertEquals(50000.0, MathExpressionEvaluator.evaluate("100000 / 2")!!, 0.001)
        assertEquals(50000.0, MathExpressionEvaluator.evaluate("100000 ÷ 2")!!, 0.001)
    }

    @Test
    fun testOperatorPrecedence() {
        // 10 + 2 * 5 should be 20, not 60
        assertEquals(20.0, MathExpressionEvaluator.evaluate("10 + 2 * 5")!!, 0.001)
        // 100 - 50 / 2 should be 75
        assertEquals(75.0, MathExpressionEvaluator.evaluate("100 - 50 / 2")!!, 0.001)
    }

    @Test
    fun testDecimalAndCents() {
        assertEquals(25.75, MathExpressionEvaluator.evaluate("10.50 + 15.25")!!, 0.001)
        assertEquals(2575L, MathExpressionEvaluator.evaluateToCents("10.50 + 15.25"))
    }

    @Test
    fun testInvalidAndEdgeCases() {
        assertNull(MathExpressionEvaluator.evaluate(""))
        assertNull(MathExpressionEvaluator.evaluate("   "))
        assertNull(MathExpressionEvaluator.evaluate("abc"))
        assertNull(MathExpressionEvaluator.evaluate("50000 / 0"))
        assertNull(MathExpressionEvaluator.evaluate("50000 +"))
    }
}
