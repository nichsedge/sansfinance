package com.sans.expensetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sans.expensetracker.data.local.dao.CategoryDao
import com.sans.expensetracker.data.local.dao.ExpenseDao
import com.sans.expensetracker.data.local.dao.InstallmentDao
import com.sans.expensetracker.data.local.dao.TagDao
import com.sans.expensetracker.data.local.entity.CategoryEntity
import com.sans.expensetracker.data.local.entity.ExpenseEntity
import com.sans.expensetracker.data.local.entity.ExpenseTagCrossRef
import com.sans.expensetracker.data.local.entity.InstallmentEntity
import com.sans.expensetracker.data.local.entity.InstallmentItemEntity
import com.sans.expensetracker.data.local.entity.TagEntity
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@Database(
    entities = [
        ExpenseEntity::class,
        InstallmentEntity::class,
        InstallmentItemEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        ExpenseTagCrossRef::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val expenseDao: ExpenseDao
    abstract val categoryDao: CategoryDao
    abstract val installmentDao: InstallmentDao
    abstract val tagDao: TagDao

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
                if (categoryDao.getCount() == 0) {
                    categoryDao.insertCategory(CategoryEntity(name = "Food", icon = "🍔", orderIndex = 0))
                    categoryDao.insertCategory(CategoryEntity(name = "Health", icon = "💊", orderIndex = 1))
                    categoryDao.insertCategory(CategoryEntity(name = "Shopping", icon = "🛍️", orderIndex = 2))
                    categoryDao.insertCategory(CategoryEntity(name = "Transport", icon = "🚗", orderIndex = 3))
                    categoryDao.insertCategory(CategoryEntity(name = "Subscriptions", icon = "🌐", orderIndex = 4))
                    categoryDao.insertCategory(CategoryEntity(name = "Others", icon = "📁", orderIndex = 5))
                }

                // Inject Seed Data from CSV ONLY if EVERYTHING is empty
                if (expenseDao.getExpenseCount() == 0 && installmentDao.getInstallmentCount() == 0) {
                    try {
                        val expenses = com.sans.expensetracker.data.util.CsvParser.parse(context)
                        if (expenses.isNotEmpty()) {
                            expenseDao.insertExpenses(expenses)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
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
    }
}
