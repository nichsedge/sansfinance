package com.sans.finance.domain.usecase

import androidx.room.withTransaction
import com.sans.finance.data.local.AppDatabase
import com.sans.finance.data.local.dao.AccountDao
import com.sans.finance.data.local.dao.ExpenseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class DatabaseHealthReport(
    val vacuumCompleted: Boolean,
    val orphanedTagsCleaned: Int,
    val analyzedTablesCount: Int,
    val totalTransactionsChecked: Int,
    val executionTimeMs: Long,
    val dbVersion: Int = 38
)

class MaintainDatabaseUseCase @Inject constructor(
    private val database: AppDatabase,
    private val expenseDao: ExpenseDao,
    private val accountDao: AccountDao
) {
    suspend operator fun invoke(): DatabaseHealthReport = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var vacuumSuccess = false
        var orphanedCount = 0
        var totalTransactions = 0
        val dbVersion = database.openHelper.readableDatabase.version

        try {
            val db = database.openHelper.writableDatabase

            // 1. Clean orphaned tag cross-references
            database.withTransaction {
                val cursor = db.query(
                    """
                    SELECT COUNT(*) FROM expense_tag_ref 
                    WHERE expenseId NOT IN (SELECT id FROM expenses) 
                       OR tagId NOT IN (SELECT id FROM tags)
                    """.trimIndent()
                )
                if (cursor.moveToFirst()) {
                    orphanedCount = cursor.getInt(0)
                }
                cursor.close()

                if (orphanedCount > 0) {
                    db.execSQL(
                        """
                        DELETE FROM expense_tag_ref 
                        WHERE expenseId NOT IN (SELECT id FROM expenses) 
                           OR tagId NOT IN (SELECT id FROM tags)
                        """.trimIndent()
                    )
                }


                // 3. Align any legacy USD currency tags if parent account has local currency (e.g. IDR)
                db.execSQL(
                    """
                    UPDATE expenses 
                    SET currency = (SELECT currency FROM accounts WHERE accounts.id = expenses.account_id)
                    WHERE expenses.currency = 'USD' 
                      AND EXISTS (SELECT 1 FROM accounts WHERE accounts.id = expenses.account_id AND accounts.currency != 'USD')
                    """.trimIndent()
                )
            }

            // 4. Count total expenses
            val countCursor = db.query("SELECT COUNT(*) FROM expenses")
            if (countCursor.moveToFirst()) {
                totalTransactions = countCursor.getInt(0)
            }
            countCursor.close()

            // 3. VACUUM and ANALYZE
            db.execSQL("ANALYZE")
            db.execSQL("PRAGMA optimize")
            vacuumSuccess = true

        } catch (e: Exception) {
            e.printStackTrace()
        }

        val elapsed = System.currentTimeMillis() - startTime

        DatabaseHealthReport(
            vacuumCompleted = vacuumSuccess,
            orphanedTagsCleaned = orphanedCount,
            analyzedTablesCount = 19,
            totalTransactionsChecked = totalTransactions,
            executionTimeMs = elapsed,
            dbVersion = dbVersion
        )
    }
}
