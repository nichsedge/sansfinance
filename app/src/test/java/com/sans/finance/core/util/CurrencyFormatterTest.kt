package com.sans.finance.core.util

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class CurrencyFormatterTest {

    @Before
    fun setup() {
        Locale.setDefault(Locale("id", "ID"))
    }

    @Test
    fun `formatAmount correctly formats positive amounts`() {
        // 1000 cents = 10 IDR
        assertEquals("Rp10", CurrencyFormatter.formatAmount(1000L, "IDR"))
    }

    @Test
    fun `formatAmount correctly formats negative amounts`() {
        // -1000 cents = -10 IDR
        assertEquals("-Rp10", CurrencyFormatter.formatAmount(-1000L, "IDR"))
    }

    @Test
    fun `formatAmount rounds up decimal amounts`() {
        // 1001 cents = 10.01 IDR -> rounded up to 11 IDR
        assertEquals("Rp11", CurrencyFormatter.formatAmount(1001L, "IDR"))
    }

    @Test
    fun `formatAmount correctly formats zero`() {
        assertEquals("Rp0", CurrencyFormatter.formatAmount(0L, "IDR"))
    }

    @Test
    fun `formatAmount correctly formats large amounts with separators`() {
        // 100,000,000 cents = 1,000,000 IDR
        assertEquals("Rp1.000.000", CurrencyFormatter.formatAmount(100_000_000L, "IDR"))
    }

    @Test
    fun `formatNumberOnly correctly formats positive amounts`() {
        assertEquals("10", CurrencyFormatter.formatNumberOnly(1000L))
    }

    @Test
    fun `formatNumberOnly rounds up decimal amounts`() {
        assertEquals("11", CurrencyFormatter.formatNumberOnly(1001L))
    }

    @Test
    fun `formatNumberOnly handles zero`() {
        assertEquals("0", CurrencyFormatter.formatNumberOnly(0L))
    }
}
