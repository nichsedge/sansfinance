package com.sans.finance.data.local

import android.util.Log
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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

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
        GoalEntity::class,
        BudgetEntity::class,
        PortfolioSnapshotHeaderEntity::class,
        PortfolioHoldingEntity::class
    ],
    version = 19,
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
    abstract val portfolioDao: com.sans.finance.data.local.dao.PortfolioDao

    fun checkpoint() {
        val cursor =
            query(androidx.sqlite.db.SimpleSQLiteQuery("PRAGMA wal_checkpoint(TRUNCATE)"), null)
        if (cursor.moveToFirst()) {
            cursor.getInt(0) // Forces evaluation
        }
        cursor.close()
    }

    class Callback(
        private val context: android.content.Context,
        private val categoryDaoProvider: javax.inject.Provider<CategoryDao>,
        private val expenseDaoProvider: javax.inject.Provider<ExpenseDao>,
        private val installmentDaoProvider: javax.inject.Provider<InstallmentDao>
    ) : RoomDatabase.Callback() {
        override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            super.onOpen(db)
            MainScope().launch {
                val categoryDao = categoryDaoProvider.get()
                val expenseDao = expenseDaoProvider.get()
                val installmentDao = installmentDaoProvider.get()

                // Always ensure basic categories exist
                val allCats = categoryDao.getAllCategoriesSync()
                if (allCats.isEmpty()) {
                    // Expense Categories
                    categoryDao.insertCategory(
                        CategoryEntity(
                            name = "Food",
                            icon = "🍔",
                            orderIndex = 0,
                            type = "EXPENSE"
                        )
                    )
                    categoryDao.insertCategory(
                        CategoryEntity(
                            name = "Health",
                            icon = "💊",
                            orderIndex = 1,
                            type = "EXPENSE"
                        )
                    )
                    categoryDao.insertCategory(
                        CategoryEntity(
                            name = "Shopping",
                            icon = "🛍️",
                            orderIndex = 2,
                            type = "EXPENSE"
                        )
                    )
                    categoryDao.insertCategory(
                        CategoryEntity(
                            name = "Transport",
                            icon = "🚗",
                            orderIndex = 3,
                            type = "EXPENSE"
                        )
                    )
                    categoryDao.insertCategory(
                        CategoryEntity(
                            name = "Subscriptions",
                            icon = "🌐",
                            orderIndex = 4,
                            type = "EXPENSE"
                        )
                    )
                    categoryDao.insertCategory(
                        CategoryEntity(
                            name = "Others",
                            icon = "📁",
                            orderIndex = 5,
                            type = "EXPENSE"
                        )
                    )
                }

                // Check for Income categories specifically (useful for existing users)
                val incomeCats = allCats.filter { it.type == "INCOME" }
                if (incomeCats.isEmpty()) {
                    categoryDao.insertCategory(
                        CategoryEntity(
                            name = "Salary",
                            icon = "💰",
                            orderIndex = 6,
                            type = "INCOME"
                        )
                    )
                    categoryDao.insertCategory(
                        CategoryEntity(
                            name = "Business",
                            icon = "📈",
                            orderIndex = 7,
                            type = "INCOME"
                        )
                    )
                    categoryDao.insertCategory(
                        CategoryEntity(
                            name = "Bonus",
                            icon = "🎁",
                            orderIndex = 8,
                            type = "INCOME"
                        )
                    )
                    categoryDao.insertCategory(
                        CategoryEntity(
                            name = "Investments",
                            icon = "🏦",
                            orderIndex = 9,
                            type = "INCOME"
                        )
                    )
                }

                // Inject Seed Data from CSV ONLY if EVERYTHING is empty
                if (expenseDao.getExpenseCount() == 0 && installmentDao.getInstallmentCount() == 0) {
                    try {
                        val expenses = com.sans.finance.data.util.CsvParser.parse(context)
                        if (expenses.isNotEmpty()) {
                            expenseDao.insertExpenses(expenses)
                        }
                    } catch (e: Exception) {
                        Log.e("AppDatabase", "Failed to inject seed data", e)
                    }
                }
            }
        }
    }

    companion object {
        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Create tags table
                db.execSQL("CREATE TABLE IF NOT EXISTS `tags` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")

                // Create expense_tag_ref table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `expense_tag_ref` (
                        `expenseId` INTEGER NOT NULL, 
                        `tagId` INTEGER NOT NULL, 
                        PRIMARY KEY(`expenseId`, `tagId`), 
                        FOREIGN KEY(`expenseId`) REFERENCES `expenses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                        FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_tag_ref_expenseId` ON `expense_tag_ref` (`expenseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_tag_ref_tagId` ON `expense_tag_ref` (`tagId`)")

                // Pre-insert distinct platforms into tags
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO tags (name)
                    SELECT DISTINCT platform
                    FROM expenses
                    WHERE platform IS NOT NULL AND platform != ''
                """.trimIndent()
                )

                // Link expenses to these tags
                db.execSQL(
                    """
                    INSERT INTO expense_tag_ref (expenseId, tagId)
                    SELECT e.id, t.id
                    FROM expenses e
                    JOIN tags t ON e.platform = t.name
                    WHERE e.platform IS NOT NULL AND e.platform != ''
                """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tags ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Create accounts table
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `accounts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `balance` INTEGER NOT NULL, `currency` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)"
                )
                // Insert default account
                db.execSQL(
                    "INSERT INTO `accounts` (`id`, `name`, `type`, `balance`, `currency`, `created_at`, `updated_at`) VALUES (1, 'Cash', 'Cash', 0, 'IDR', ${System.currentTimeMillis()}, ${System.currentTimeMillis()})"
                )

                // Add columns to expenses
                db.execSQL("ALTER TABLE expenses ADD COLUMN account_id INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE expenses ADD COLUMN type TEXT NOT NULL DEFAULT 'EXPENSE'")

                // Add columns to categories
                db.execSQL("ALTER TABLE categories ADD COLUMN type TEXT NOT NULL DEFAULT 'EXPENSE'")
            }
        }

        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `net_worth_snapshots` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, `totalAssets` INTEGER NOT NULL, `totalLiabilities` INTEGER NOT NULL, `netWorth` INTEGER NOT NULL)"
                )
            }
        }

        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `targetAmount` INTEGER NOT NULL, `currentAmount` INTEGER NOT NULL, `currency` TEXT NOT NULL, `deadline` INTEGER, `accountId` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
                )
            }
        }

        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `budgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` INTEGER NOT NULL, `categoryId` INTEGER, `accountId` INTEGER, `period` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
            }
        }

        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN recurrence_interval TEXT")
                db.execSQL("ALTER TABLE expenses ADD COLUMN next_due_date INTEGER")
            }
        }

        val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses RENAME COLUMN item_name TO note")
                db.execSQL("ALTER TABLE expenses RENAME COLUMN merchant TO description")
            }
        }

        val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN to_account_id INTEGER")
            }
        }

        val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN currency TEXT NOT NULL DEFAULT 'USD'")
            }
        }

        val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `portfolio_snapshots` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `snapshot_date` INTEGER NOT NULL,
                        `source` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `asset` TEXT NOT NULL,
                        `currency` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `price` REAL,
                        `value_idr` REAL NOT NULL,
                        `value_usd` REAL NOT NULL,
                        `account` TEXT NOT NULL,
                        `details` TEXT,
                        `created_at` INTEGER NOT NULL
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_portfolio_snapshots_snapshot_date` ON `portfolio_snapshots` (`snapshot_date`)")
            }
        }

        val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 1. Create new header table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `portfolio_snapshot_headers` (
                        `snapshotDate` INTEGER PRIMARY KEY NOT NULL,
                        `exchangeRateUsd` REAL NOT NULL,
                        `totalValueIdr` REAL NOT NULL,
                        `totalValueUsd` REAL NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent()
                )

                // 2. Create new holdings table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `portfolio_holdings` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `snapshot_date` INTEGER NOT NULL,
                        `source` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `asset` TEXT NOT NULL,
                        `currency` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `price` REAL,
                        `value_idr` REAL NOT NULL,
                        `account` TEXT NOT NULL,
                        `details` TEXT,
                        FOREIGN KEY(`snapshot_date`) REFERENCES `portfolio_snapshot_headers`(`snapshotDate`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_portfolio_holdings_snapshot_date` ON `portfolio_holdings` (`snapshot_date`)")

                // 3. Migrate data from old flat table
                // Estimate exchange rate from existing data (value_idr / value_usd)
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO portfolio_snapshot_headers (snapshotDate, exchangeRateUsd, totalValueIdr, totalValueUsd, createdAt)
                    SELECT 
                        snapshot_date, 
                        AVG(value_idr / NULLIF(value_usd, 0)), 
                        SUM(value_idr), 
                        SUM(value_usd),
                        MIN(created_at)
                    FROM portfolio_snapshots
                    GROUP BY snapshot_date
                """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO portfolio_holdings (snapshot_date, source, category, asset, currency, amount, price, value_idr, account, details)
                    SELECT snapshot_date, source, category, asset, currency, amount, price, value_idr, account, details
                    FROM portfolio_snapshots
                """.trimIndent()
                )

                // 4. Drop old table
                db.execSQL("DROP TABLE portfolio_snapshots")
            }
        }

        val MIGRATION_17_18 = object : androidx.room.migration.Migration(17, 18) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE portfolio_holdings ADD COLUMN asset_class TEXT NOT NULL DEFAULT 'Other'")
            }
        }

        val MIGRATION_18_19 = object : androidx.room.migration.Migration(18, 19) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 1. Rename old table
                db.execSQL("ALTER TABLE goals RENAME TO goals_old")

                // 2. Create new table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `targetAmount` REAL NOT NULL, 
                        `targetType` TEXT NOT NULL, 
                        `targetName` TEXT, 
                        `currency` TEXT NOT NULL, 
                        `deadline` INTEGER, 
                        `createdAt` INTEGER NOT NULL, 
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent()
                )

                // 3. Copy data (defaulting targetType to 'TOTAL')
                db.execSQL(
                    """
                    INSERT INTO goals (id, name, targetAmount, targetType, targetName, currency, deadline, createdAt, updatedAt)
                    SELECT id, name, CAST(targetAmount AS REAL), 'TOTAL', NULL, currency, deadline, createdAt, updatedAt
                    FROM goals_old
                """.trimIndent()
                )

                // 4. Drop old table
                db.execSQL("DROP TABLE goals_old")
            }
        }
    }
}
