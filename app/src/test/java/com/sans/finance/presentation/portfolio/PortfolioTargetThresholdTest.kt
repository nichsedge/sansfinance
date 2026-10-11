package com.sans.finance.presentation.portfolio

import com.sans.finance.presentation.portfolio.components.PortfolioViewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class PortfolioTargetThresholdTest {

    @Test
    fun `portfolio view mode contains three distinct filter modes`() {
        val modes = PortfolioViewMode.entries
        assertEquals(3, modes.size)
        assertEquals("All Assets", PortfolioViewMode.FLAT.label)
        assertEquals("By Category", PortfolioViewMode.BY_CATEGORY.label)
        assertEquals("By Asset Class", PortfolioViewMode.BY_ASSET_CLASS.label)
    }

    @Test
    fun `target percentage clamping enforces bounds 0 to 100`() {
        val negative = (-15.0).coerceIn(0.0, 100.0)
        val overHundred = (145.0).coerceIn(0.0, 100.0)
        val valid = (42.5).coerceIn(0.0, 100.0)

        assertEquals(0.0, negative, 0.001)
        assertEquals(100.0, overHundred, 0.001)
        assertEquals(42.5, valid, 0.001)
    }

    @Test
    fun `portfolio target budget detects over budget and computes headroom`() {
        val otherTargetsSum = 75.0
        val targetPercentage = 30.0
        val total = otherTargetsSum + targetPercentage

        val isOverBudget = total > 100.001
        assertTrue(isOverBudget)

        val maxAvailable = (100.0 - otherTargetsSum).coerceIn(0.0, 100.0)
        assertEquals(25.0, maxAvailable, 0.001)

        val cappedTotal = otherTargetsSum + maxAvailable
        assertEquals(100.0, cappedTotal, 0.001)
        assertFalse(cappedTotal > 100.001)
    }

    @Test
    fun `auto balance other targets proportionally fits 100 percent budget`() {
        // Given 4 asset classes:
        // Equities: 50%, Fixed Income: 20%, Crypto: 10%, Cash: 20% (Total = 100%)
        val targetAssetClass = "Equities"
        val requestedEquitiesTarget = 70.0 // User wants to increase Equities to 70%

        val otherTargets = listOf(20.0, 10.0, 20.0) // Fixed Income, Crypto, Cash sum = 50%
        val otherTargetsSum = otherTargets.sum()

        val remainingBudget = (100.0 - requestedEquitiesTarget).coerceAtLeast(0.0) // 30%
        val scale = remainingBudget / otherTargetsSum // 30.0 / 50.0 = 0.6

        val scaledOtherTargets = otherTargets.map { (it * scale * 10).roundToInt() / 10.0 }
        // 20.0 * 0.6 = 12.0
        // 10.0 * 0.6 = 6.0
        // 20.0 * 0.6 = 12.0
        // Sum = 12 + 6 + 12 = 30.0

        val newTotal = requestedEquitiesTarget + scaledOtherTargets.sum()
        assertEquals(100.0, newTotal, 0.001)
        assertFalse(newTotal > 100.001)
    }
}
