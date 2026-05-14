package com.sans.finance.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sans.finance.data.local.dao.CategoryDao
import com.sans.finance.data.local.dao.ExpenseDao
import com.sans.finance.data.local.dao.InstallmentDao
import com.sans.finance.data.local.dao.TagDao
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.data.local.entity.BudgetEntity
import com.sans.finance.data.local.entity.CategoryEntity
import com.sans.finance.data.local.entity.ExpenseEntity
import com.sans.finance.data.local.entity.ExpenseTagCrossRef
import com.sans.finance.data.local.entity.GoalEntity
import com.sans.finance.data.local.entity.InstallmentEntity
import com.sans.finance.data.local.entity.InstallmentItemEntity
import com.sans.finance.data.local.entity.NetWorthSnapshotEntity
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.data.local.entity.PortfolioSnapshotHeaderEntity
import com.sans.finance.data.local.entity.TagEntity

@Database(
    entities = [
        ExpenseEntity::class,
        InstallmentEntity::class,
        InstallmentItemEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        ExpenseTagCrossRef::class,
        AccountEntity::class,
        NetWorthSnapshotEntity::class,
        PortfolioSnapshotHeaderEntity::class,
        PortfolioHoldingEntity::class,
        com.sans.finance.data.local.entity.ExpenseFtsEntity::class,
        com.sans.finance.data.local.entity.ExchangeRateEntity::class,
        GoalEntity::class,
        BudgetEntity::class,
        com.sans.finance.data.local.entity.PortfolioTargetEntity::class
    ],
    version = 27,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val expenseDao: ExpenseDao
    abstract val categoryDao: CategoryDao
    abstract val installmentDao: InstallmentDao
    abstract val tagDao: TagDao
    abstract val accountDao: com.sans.finance.data.local.dao.AccountDao
    abstract val goalDao: com.sans.finance.data.local.dao.GoalDao
    abstract val budgetDao: com.sans.finance.data.local.dao.BudgetDao
    abstract val currencyDao: com.sans.finance.data.local.dao.CurrencyDao
    abstract val portfolioDao: com.sans.finance.data.local.dao.PortfolioDao
    abstract val portfolioTargetDao: com.sans.finance.data.local.dao.PortfolioTargetDao

    fun checkpoint() {
        val cursor =
            query(androidx.sqlite.db.SimpleSQLiteQuery("PRAGMA wal_checkpoint(TRUNCATE)"), null)
        if (cursor.moveToFirst()) {
            cursor.getInt(0) // Forces evaluation
        }
        cursor.close()
    }
}
