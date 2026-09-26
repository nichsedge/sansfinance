package com.sans.finance.data.repository

import com.sans.finance.data.local.dao.PortfolioDao
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.data.local.entity.PortfolioSnapshotHeaderEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PortfolioRepositoryImplTest {

    private lateinit var dao: PortfolioDao
    private lateinit var repository: PortfolioRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = PortfolioRepositoryImpl(
            dao = dao,
            targetDao = mockk(relaxed = true),
            expenseDao = mockk(relaxed = true),
            accountDao = mockk(relaxed = true),
            accountTypeDao = mockk(relaxed = true)
        )
    }

    private fun holding(source: String, asset: String, value: Double) = PortfolioHoldingEntity(
        snapshotDate = 1000L,
        source = source,
        category = "Test",
        asset = asset,
        currency = "IDR",
        quantity = 1.0,
        price = value,
        valueIdr = value,
        assetClass = "Equity",
        account = "Test Acc",
        details = null
    )

    @Test
    fun `importSnapshot merges new items with existing items from different sources`() = runBlocking {
        val date = 1000L
        val existing = listOf(
            holding("SourceA", "Asset1", 100.0),
            holding("SourceB", "Asset2", 200.0)
        )
        coEvery { dao.getSnapshotByDateSync(date) } returns existing

        val newBatch = listOf(
            holding("SourceB", "Asset2-New", 250.0),
            holding("SourceC", "Asset3", 300.0)
        )

        val headerSlot = slot<PortfolioSnapshotHeaderEntity>()
        val itemsSlot = slot<List<PortfolioHoldingEntity>>()
        coEvery { dao.insertSnapshot(capture(headerSlot), capture(itemsSlot)) } returns Unit

        repository.importSnapshot(date, newBatch)

        val finalItems = itemsSlot.captured
        // Should keep SourceA (100)
        // Should REPLACE SourceB (200) with SourceB (250)
        // Should add SourceC (300)
        // Total = 100 + 250 + 300 = 650

        assertEquals(3, finalItems.size)
        assertEquals(650.0, headerSlot.captured.totalValueIdr, 0.01)

        val sources = finalItems.map { it.source }.toSet()
        assertEquals(setOf("SourceA", "SourceB", "SourceC"), sources)

        val sourceBAsset = finalItems.first { it.source == "SourceB" }.asset
        assertEquals("Asset2-New", sourceBAsset)
    }
}
