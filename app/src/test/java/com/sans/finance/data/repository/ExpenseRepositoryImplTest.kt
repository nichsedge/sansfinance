package com.sans.finance.data.repository

import androidx.room.withTransaction
import com.sans.finance.data.local.AppDatabase
import com.sans.finance.data.local.dao.AccountDao
import com.sans.finance.data.local.dao.CategoryDao
import com.sans.finance.data.local.dao.ExpenseDao
import com.sans.finance.data.local.dao.InstallmentDao
import com.sans.finance.data.local.dao.TagDao
import com.sans.finance.data.local.entity.ExpenseEntity
import com.sans.finance.data.local.entity.ExpenseWithTags
import com.sans.finance.data.local.entity.InstallmentItemEntity
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.data.local.entity.CategoryEntity
import com.sans.finance.data.local.entity.ExpenseTagCrossRef
import com.sans.finance.data.local.entity.TagEntity
import com.sans.finance.domain.model.ReSyncMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExpenseRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ExpenseDao
    private lateinit var tagDao: TagDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var installmentDao: InstallmentDao
    private lateinit var accountDao: AccountDao
    private lateinit var repository: ExpenseRepositoryImpl

    @Before
    fun setUp() {
        db = mockk(relaxed = true)
        dao = mockk(relaxed = true)
        tagDao = mockk(relaxed = true)
        categoryDao = mockk(relaxed = true)
        installmentDao = mockk(relaxed = true)
        accountDao = mockk(relaxed = true)

        repository = ExpenseRepositoryImpl(
            db = db,
            dao = dao,
            tagDao = tagDao,
            categoryDao = categoryDao,
            installmentDao = installmentDao,
            accountDao = accountDao,
            context = null
        )
    }

    @Test
    fun getExpenseById_withLargeAutoIncrementId_returnsDirectExpense() = runTest {
        // Real expenses can have IDs >= 100_000_000 from old autoincrement.
        // These should be found directly in the expenses table.
        val largeId = 100_000_097L
        val entity = ExpenseEntity(
            id = largeId,
            date = 1787567044456L,
            title = "Nasi Padang",
            details = null,
            amount = 1400000L,
            categoryId = 1L,
            accountId = 1L,
            type = "EXPENSE",
            currency = "IDR",
            status = "completed",
            isRecurring = false
        )
        val expenseWithTags = ExpenseWithTags(
            expense = entity,
            tags = emptyList(),
            category = null,
            installment = null
        )

        coEvery { dao.getExpenseById(largeId) } returns expenseWithTags

        val result = repository.getExpenseById(largeId)

        assertNotNull(result)
        assertEquals(largeId, result?.id)
        assertEquals("Nasi Padang", result?.title)
        assertEquals(1400000L, result?.amount)
        assertEquals("IDR", result?.currency)
        assertEquals(1L, result?.categoryId)
    }

    @Test
    fun getExpenseById_withNegativeId_returnsSyntheticInstallmentPayment() = runTest {
        // Negative IDs are synthetic: id = -installmentItemId
        val installmentItemId = 82L
        val syntheticId = -installmentItemId // -82

        val itemEntity = InstallmentItemEntity(
            id = installmentItemId,
            installmentId = 13L,
            amount = 4597500L,
            dueDate = 1785078262655L,
            status = "Paid",
            monthNumber = 1
        )

        coEvery { dao.getExpenseById(syntheticId) } returns null // negative ID won't be in DB
        coEvery { installmentDao.getInstallmentItemById(installmentItemId) } returns itemEntity
        coEvery { installmentDao.getInstallmentById(13L) } returns null

        val result = repository.getExpenseById(syntheticId)

        assertNotNull(result)
        assertEquals(syntheticId, result?.id)
        assertEquals(4597500L, result?.amount)
        assertTrue(result?.isInstallmentPayment == true)
        assertEquals(1, result?.installmentMonth)
    }

    @Test
    fun getExpenseById_withNonExistentId_returnsNull() = runTest {
        coEvery { dao.getExpenseById(999999L) } returns null

        val result = repository.getExpenseById(999999L)
        assertNull(result)
    }

    @Test
    fun getExpenseById_withSyntheticRecurringId_returnsParentRuleWithInstanceMetadata() = runTest {
        val ruleId = 15L
        val occurrenceIndex = 3
        val syntheticId = com.sans.finance.core.util.RecurringOccurrenceCalculator.generateSyntheticId(ruleId, occurrenceIndex)

        val entity = ExpenseEntity(
            id = ruleId,
            date = 1787567044456L,
            title = "Spotify Premium",
            details = "Family Plan",
            amount = 8600000L,
            categoryId = 2L,
            accountId = 1L,
            type = "EXPENSE",
            currency = "IDR",
            status = "Paid",
            isRecurring = true,
            recurrenceInterval = "MONTHLY",
            recurrenceIntervalMultiplier = 1,
            recurrenceEndType = "NEVER"
        )
        val expenseWithTags = ExpenseWithTags(
            expense = entity,
            tags = emptyList(),
            category = null,
            installment = null
        )

        coEvery { dao.getExpenseById(ruleId) } returns expenseWithTags

        val result = repository.getExpenseById(syntheticId)

        assertNotNull(result)
        assertEquals(syntheticId, result?.id)
        assertEquals("Spotify Premium", result?.title)
        assertTrue(result?.isRecurringInstance == true)
        assertEquals(ruleId, result?.parentRecurringId)
        assertEquals(occurrenceIndex, result?.recurringOccurrenceIndex)
    }

    @Test
    fun reSyncAccountBalances_withBalanceAsTruth_usesExistingAdjustmentTagId() = runTest {
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction(any<suspend () -> Any>()) } coAnswers {
            secondArg<suspend () -> Any>().invoke()
        }

        val account = AccountEntity(id = 26L, name = "CIMB Niaga", balance = 300000000L, type = "Bank")
        coEvery { accountDao.getAllAccounts() } returns flowOf(listOf(account))
        coEvery { dao.getAllExpenseEntities() } returns emptyList()
        coEvery { installmentDao.getInstallmentPaymentsBetween(any(), any()) } returns flowOf(emptyList())

        val category = CategoryEntity(id = 8L, name = "Misc", icon = "wrench", orderIndex = 7, type = "INCOME")
        coEvery { categoryDao.getAllCategoriesSync() } returns listOf(category)

        // Existing Adjustment tag has id 16
        val existingTag = TagEntity(id = 16L, name = "Adjustment", orderIndex = 0, isVisible = true)
        coEvery { tagDao.getTagByName("Adjustment") } returns existingTag

        val insertedExpenseId = 999L
        coEvery { dao.insertExpense(any()) } returns insertedExpenseId

        val crossRefsSlot = slot<List<ExpenseTagCrossRef>>()
        coEvery { dao.insertExpenseTagCrossRefs(capture(crossRefsSlot)) } returns Unit

        repository.reSyncAccountBalances(ReSyncMode.BALANCE_AS_TRUTH, 0L)

        coVerify { dao.insertExpense(any()) }
        coVerify { dao.insertExpenseTagCrossRefs(any()) }
        assertEquals(1, crossRefsSlot.captured.size)
        assertEquals(insertedExpenseId, crossRefsSlot.captured[0].expenseId)
        assertEquals(16L, crossRefsSlot.captured[0].tagId)
    }

    @Test
    fun reSyncAccountBalances_withBalanceAsTruth_createsAdjustmentTagIfMissing() = runTest {
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction(any<suspend () -> Any>()) } coAnswers {
            secondArg<suspend () -> Any>().invoke()
        }

        val account = AccountEntity(id = 26L, name = "CIMB Niaga", balance = 300000000L, type = "Bank")
        coEvery { accountDao.getAllAccounts() } returns flowOf(listOf(account))
        coEvery { dao.getAllExpenseEntities() } returns emptyList()
        coEvery { installmentDao.getInstallmentPaymentsBetween(any(), any()) } returns flowOf(emptyList())

        val category = CategoryEntity(id = 8L, name = "Misc", icon = "wrench", orderIndex = 7, type = "INCOME")
        coEvery { categoryDao.getAllCategoriesSync() } returns listOf(category)

        // No Adjustment tag initially
        coEvery { tagDao.getTagByName("Adjustment") } returns null
        coEvery { tagDao.insertTag(any()) } returns 42L

        val insertedExpenseId = 1001L
        coEvery { dao.insertExpense(any()) } returns insertedExpenseId

        val crossRefsSlot = slot<List<ExpenseTagCrossRef>>()
        coEvery { dao.insertExpenseTagCrossRefs(capture(crossRefsSlot)) } returns Unit

        repository.reSyncAccountBalances(ReSyncMode.BALANCE_AS_TRUTH, 0L)

        coVerify { tagDao.insertTag(match { it.name == "Adjustment" }) }
        assertEquals(1, crossRefsSlot.captured.size)
        assertEquals(insertedExpenseId, crossRefsSlot.captured[0].expenseId)
        assertEquals(42L, crossRefsSlot.captured[0].tagId)
    }
}
