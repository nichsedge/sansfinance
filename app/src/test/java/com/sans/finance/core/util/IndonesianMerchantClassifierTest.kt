package com.sans.finance.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IndonesianMerchantClassifierTest {

    @Test
    fun testClassifyFoodDelivery() {
        val res = IndonesianMerchantClassifier.classify("Pembayaran GoFood Ayam Geprek")
        assertNotNull(res)
        assertEquals("Food Delivery", res!!.normalizedMerchant)
        assertEquals("Food", res.suggestedCategory)
        assertEquals("EXPENSE", res.suggestedType)
        assertTrue(res.tags.contains("food_delivery"))
    }

    @Test
    fun testClassifyGroceries() {
        val res = IndonesianMerchantClassifier.classify("Indomaret Point Dago")
        assertNotNull(res)
        assertEquals("Supermarket & Groceries", res!!.normalizedMerchant)
        assertEquals("Groceries", res.suggestedCategory)
    }

    @Test
    fun testClassifyUtilities() {
        val res = IndonesianMerchantClassifier.classify("Beli Token PLN 100k")
        assertNotNull(res)
        assertEquals("Electricity (PLN)", res!!.normalizedMerchant)
        assertEquals("Utility", res.suggestedCategory)
    }

    @Test
    fun testClassifySalaryIncome() {
        val res = IndonesianMerchantClassifier.classify("Payroll Gaji Bulan Agustus")
        assertNotNull(res)
        assertEquals("Salary / Payroll", res!!.normalizedMerchant)
        assertEquals("Salary", res.suggestedCategory)
        assertEquals("INCOME", res.suggestedType)
    }

    @Test
    fun testClassifyInvestmentYield() {
        val res = IndonesianMerchantClassifier.classify("Kupon SBN ST012")
        assertNotNull(res)
        assertEquals("Investment Yield", res!!.normalizedMerchant)
        assertEquals("INCOME", res.suggestedType)
        assertTrue(res.tags.contains("passive_income"))
    }
}
