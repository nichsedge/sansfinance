package com.sans.finance.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sans.finance.data.local.AppDatabase
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.data.local.entity.CategoryEntity
import com.sans.finance.data.local.entity.ExpenseEntity
import com.sans.finance.data.local.entity.InstallmentEntity
import com.sans.finance.data.local.entity.InstallmentItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var accountDao: AccountDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var installmentDao: InstallmentDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        expenseDao = db.expenseDao
        accountDao = db.accountDao
        categoryDao = db.categoryDao
        installmentDao = db.installmentDao
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getDailySpendingBetween_includesPaidInstallments_andHonorsDateBounds() = runBlocking {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        val start = now - day
        val end = now + day

        val accountId = accountDao.insertAccount(AccountEntity(name = "Cash", type = "Cash", balance = 0L))
        val categoryId = categoryDao.insertCategoryAndReturnId(
            CategoryEntity(name = "Food", icon = "Restaurant", type = "EXPENSE")
        )

        expenseDao.insertExpense(
            ExpenseEntity(
                date = now,
                title = "Lunch",
                details = null,
                amount = 15_000,
                categoryId = categoryId,
                accountId = accountId,
                status = "Paid",
                isRecurring = false,
                isInstallment = false,
                type = "EXPENSE",
                currency = "IDR"
            )
        )

        val parentExpenseId = expenseDao.insertExpense(
            ExpenseEntity(
                date = now,
                title = "Phone",
                details = null,
                amount = 120_000,
                categoryId = categoryId,
                accountId = accountId,
                status = "Pending",
                isRecurring = false,
                isInstallment = true,
                type = "EXPENSE",
                currency = "IDR"
            )
        )

        val installmentId = installmentDao.insertInstallment(
            InstallmentEntity(
                expenseId = parentExpenseId,
                status = "Active",
                durationMonths = 3
            )
        )

        installmentDao.insertInstallmentItem(
            InstallmentItemEntity(
                installmentId = installmentId,
                amount = 10_000,
                dueDate = now,
                status = "Paid",
                monthNumber = 1
            )
        )
        installmentDao.insertInstallmentItem(
            InstallmentItemEntity(
                installmentId = installmentId,
                amount = 50_000,
                dueDate = now,
                status = "Pending",
                monthNumber = 2
            )
        )
        installmentDao.insertInstallmentItem(
            InstallmentItemEntity(
                installmentId = installmentId,
                amount = 20_000,
                dueDate = end,
                status = "Paid",
                monthNumber = 3
            )
        )

        val rows = expenseDao.getDailySpendingBetween(start, end).first()
        val total = rows.sumOf { it.amount }

        assertEquals(25_000L, total)
    }
}

private suspend fun CategoryDao.insertCategoryAndReturnId(category: CategoryEntity): Long {
    insertCategory(category)
    return getAllCategoriesSync().first { it.name == category.name }.id
}
