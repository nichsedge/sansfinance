package com.sans.expensetracker.data.local

import android.util.Log
import androidx.room.Database
import androidx.room.RoomDatabase
import com.sans.expensetracker.data.local.dao.CategoryDao
import com.sans.expensetracker.data.local.dao.ExpenseDao
import com.sans.expensetracker.data.local.dao.InstallmentDao
import com.sans.expensetracker.data.local.dao.TagDao
import com.sans.expensetracker.data.local.entity.AccountEntity
import com.sans.expensetracker.data.local.entity.CategoryEntity
import com.sans.expensetracker.data.local.entity.ExpenseEntity
import com.sans.expensetracker.data.local.entity.ExpenseTagCrossRef
import com.sans.expensetracker.data.local.entity.InstallmentEntity
import com.sans.expensetracker.data.local.entity.InstallmentItemEntity
import com.sans.expensetracker.data.local.entity.TagEntity
import com.sans.expensetracker.data.local.entity.NetWorthSnapshotEntity
import com.sans.expensetracker.data.local.entity.GoalEntity
import com.sans.expensetracker.data.local.entity.BudgetEntity
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
        BudgetEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val expenseDao: ExpenseDao
    abstract val categoryDao: CategoryDao
    abstract val installmentDao: InstallmentDao
    abstract val tagDao: TagDao
    abstract val accountDao: com.sans.expensetracker.data.local.dao.AccountDao
    abstract val goalDao: com.sans.expensetracker.data.local.dao.GoalDao
    abstract val budgetDao: com.sans.expensetracker.data.local.dao.BudgetDao

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
                    categoryDao.insertCategory(CategoryEntity(name = "Food", icon = "🍔", orderIndex = 0, type = "EXPENSE"))
                    categoryDao.insertCategory(CategoryEntity(name = "Health", icon = "💊", orderIndex = 1, type = "EXPENSE"))
                    categoryDao.insertCategory(CategoryEntity(name = "Shopping", icon = "🛍️", orderIndex = 2, type = "EXPENSE"))
                    categoryDao.insertCategory(CategoryEntity(name = "Transport", icon = "🚗", orderIndex = 3, type = "EXPENSE"))
                    categoryDao.insertCategory(CategoryEntity(name = "Subscriptions", icon = "🌐", orderIndex = 4, type = "EXPENSE"))
                    categoryDao.insertCategory(CategoryEntity(name = "Others", icon = "📁", orderIndex = 5, type = "EXPENSE"))
                }

                // Check for Income categories specifically (useful for existing users)
                val incomeCats = allCats.filter { it.type == "INCOME" }
                if (incomeCats.isEmpty()) {
                    categoryDao.insertCategory(CategoryEntity(name = "Salary", icon = "💰", orderIndex = 6, type = "INCOME"))
                    categoryDao.insertCategory(CategoryEntity(name = "Business", icon = "📈", orderIndex = 7, type = "INCOME"))
                    categoryDao.insertCategory(CategoryEntity(name = "Bonus", icon = "🎁", orderIndex = 8, type = "INCOME"))
                    categoryDao.insertCategory(CategoryEntity(name = "Investments", icon = "🏦", orderIndex = 9, type = "INCOME"))
                }

                // Inject Seed Data from CSV ONLY if EVERYTHING is empty
                if (expenseDao.getExpenseCount() == 0 && installmentDao.getInstallmentCount() == 0) {
                    try {
                        val expenses = com.sans.expensetracker.data.util.CsvParser.parse(context)
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
    }
}
