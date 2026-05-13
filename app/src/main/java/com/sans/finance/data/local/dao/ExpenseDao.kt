package com.sans.finance.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sans.finance.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Transaction
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<com.sans.finance.data.local.entity.ExpenseWithTags>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE date >= :since AND date < :until ORDER BY date DESC")
    fun getExpensesBetween(
        since: Long,
        until: Long
    ): Flow<List<com.sans.finance.data.local.entity.ExpenseWithTags>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE is_recurring = 1 ORDER BY date DESC")
    fun getRecurringExpenses(): Flow<List<com.sans.finance.data.local.entity.ExpenseWithTags>>

    @Transaction
    @Query(
        """
        SELECT DISTINCT e.* FROM expenses e
        LEFT JOIN expense_tag_ref etr ON e.id = etr.expenseId
        LEFT JOIN tags t ON etr.tagId = t.id
        WHERE (:query IS NULL OR e.note LIKE '%' || :query || '%' OR e.description LIKE '%' || :query || '%')
        AND (:categoryCount = 0 OR e.category_id IN (:categoryIds))
        AND (:accountCount = 0 OR e.account_id IN (:accountIds))
        AND (e.date >= :since AND e.date < :until)
        AND e.is_installment = 0
        AND (:minAmount IS NULL OR e.final_price >= :minAmount)
        AND (:maxAmount IS NULL OR e.final_price <= :maxAmount)
        AND (:tagCount = 0 OR t.name IN (:tags))
        AND (:typeCount = 0 OR e.type IN (:types))
        ORDER BY e.date DESC
    """
    )
    fun getFilteredExpenses(
        query: String?,
        categoryIds: List<Long>,
        categoryCount: Int,
        accountIds: List<Long>,
        accountCount: Int,
        since: Long,
        until: Long,
        minAmount: Long?,
        maxAmount: Long?,
        tags: List<String>,
        tagCount: Int,
        types: List<String>,
        typeCount: Int
    ): Flow<List<com.sans.finance.data.local.entity.ExpenseWithTags>>

    @Transaction
    @Query("""
        SELECT e.* FROM expenses e
        JOIN expenses_fts fts ON e.id = fts.rowid
        WHERE expenses_fts MATCH :query
        ORDER BY e.date DESC
    """)
    fun searchExpensesFts(query: String): Flow<List<com.sans.finance.data.local.entity.ExpenseWithTags>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): com.sans.finance.data.local.entity.ExpenseWithTags?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpenseEntities(): List<ExpenseEntity>

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseTagCrossRefs(crossRefs: List<com.sans.finance.data.local.entity.ExpenseTagCrossRef>)

    @Query("DELETE FROM expense_tag_ref WHERE expenseId = :expenseId")
    suspend fun deleteExpenseTagRefs(expenseId: Long)

    @Transaction
    @Query("SELECT * FROM expenses WHERE note = :note AND final_price = :amount AND date BETWEEN :startTime AND :endTime AND account_id = :accountId LIMIT 1")
    suspend fun findDuplicateExpense(note: String, amount: Long, startTime: Long, endTime: Long, accountId: Long): com.sans.finance.data.local.entity.ExpenseWithTags?

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun getExpenseCount(): Int

    @Transaction
    @Query("SELECT * FROM expenses WHERE note = :note ORDER BY date DESC LIMIT 1")
    suspend fun getLastExpenseByNote(note: String): com.sans.finance.data.local.entity.ExpenseWithTags?

    @Query("SELECT DISTINCT note FROM expenses WHERE note LIKE '%' || :query || '%' ORDER BY note ASC LIMIT 5")
    suspend fun getNoteSuggestions(query: String): List<String>

    @Query("SELECT DISTINCT description FROM expenses WHERE description LIKE '%' || :query || '%' AND description IS NOT NULL ORDER BY description ASC LIMIT 5")
    suspend fun getDescriptionSuggestions(query: String): List<String>

    @Query("SELECT note FROM expenses GROUP BY note ORDER BY COUNT(*) DESC LIMIT :limit")
    suspend fun getTopFrequentNotes(limit: Int): List<String>

    @Query("""
        SELECT note FROM expenses 
        WHERE strftime('%w', date / 1000, 'unixepoch') = :dayOfWeek
        GROUP BY note 
        ORDER BY COUNT(*) DESC 
        LIMIT :limit
    """)
    suspend fun getTopFrequentNotesByDay(dayOfWeek: String, limit: Int): List<String>

    @Query("SELECT SUM(final_price * COALESCE((SELECT rateToIdr FROM exchange_rates WHERE code = expenses.currency), 1.0)) FROM expenses WHERE type = 'EXPENSE' AND date >= :since AND is_installment = 0")
    fun getTotalSpentSince(since: Long): Flow<Long?>

    @Query("SELECT SUM(final_price * COALESCE((SELECT rateToIdr FROM exchange_rates WHERE code = expenses.currency), 1.0)) FROM expenses WHERE type = 'EXPENSE' AND is_installment = 0 AND date >= :since AND date < :until")
    fun getTotalSpentBetween(since: Long, until: Long): Flow<Long?>

    @Query("SELECT SUM(final_price * COALESCE((SELECT rateToIdr FROM exchange_rates WHERE code = expenses.currency), 1.0)) FROM expenses WHERE type = 'EXPENSE' AND is_installment = 0")
    fun getAllTimeSpent(): Flow<Long?>

    @Query(
        """
        SELECT categoryId, categoryName, categoryIcon, SUM(amount) as totalAmount
        FROM (
            SELECT c.id as categoryId, c.name as categoryName, c.icon as categoryIcon, 
                   SUM(e.final_price * COALESCE(er.rateToIdr, 1.0)) as amount
            FROM expenses e
            JOIN categories c ON e.category_id = c.id
            LEFT JOIN exchange_rates er ON e.currency = er.code
            WHERE e.date >= :since AND e.date < :until AND e.type = 'EXPENSE' AND e.is_installment = 0
            GROUP BY c.id
            UNION ALL
            SELECT c.id as categoryId, c.name as categoryName, c.icon as categoryIcon, 
                   SUM(ii.amount * COALESCE(er.rateToIdr, 1.0)) as amount
            FROM installment_items ii
            JOIN installments i ON ii.installment_id = i.id
            JOIN expenses e ON i.expense_id = e.id
            JOIN categories c ON e.category_id = c.id
            LEFT JOIN exchange_rates er ON e.currency = er.code
            WHERE ii.due_date >= :since AND ii.due_date < :until AND ii.status = 'Paid'
            GROUP BY c.id
        ) sub
        GROUP BY categoryId
    """
    )
    fun getSpendingByCategoryBetween(
        since: Long,
        until: Long
    ): Flow<List<com.sans.finance.data.local.entity.CategorySpent>>

    @Query(
        """
        SELECT categoryId, categoryName, categoryIcon, SUM(amount) as totalAmount
        FROM (
            SELECT c.id as categoryId, c.name as categoryName, c.icon as categoryIcon, 
                   SUM(e.final_price * COALESCE(er.rateToIdr, 1.0)) as amount
            FROM expenses e
            JOIN categories c ON e.category_id = c.id
            LEFT JOIN exchange_rates er ON e.currency = er.code
            WHERE e.date >= :since AND e.date < :until AND e.type = :type AND e.is_installment = 0
            GROUP BY c.id
            UNION ALL
            SELECT c.id as categoryId, c.name as categoryName, c.icon as categoryIcon, 
                   SUM(ii.amount * COALESCE(er.rateToIdr, 1.0)) as amount
            FROM installment_items ii
            JOIN installments i ON ii.installment_id = i.id
            JOIN expenses e ON i.expense_id = e.id
            JOIN categories c ON e.category_id = c.id
            LEFT JOIN exchange_rates er ON e.currency = er.code
            WHERE :type = 'EXPENSE' AND ii.due_date >= :since AND ii.due_date < :until AND ii.status = 'Paid'
            GROUP BY c.id
        ) sub
        GROUP BY categoryId
    """
    )
    fun getBreakdownByCategoryBetween(
        since: Long,
        until: Long,
        type: String
    ): Flow<List<com.sans.finance.data.local.entity.CategorySpent>>

    @Query(
        """
        SELECT SUM(amount) 
        FROM (
            SELECT (final_price * COALESCE(er.rateToIdr, 1.0)) as amount FROM expenses e
            LEFT JOIN exchange_rates er ON e.currency = er.code
            WHERE e.date >= :since AND e.date < :until AND e.type = :type AND e.is_installment = 0
            UNION ALL
            SELECT (ii.amount * COALESCE(er.rateToIdr, 1.0)) as amount FROM installment_items ii
            JOIN installments i ON ii.installment_id = i.id
            JOIN expenses e ON i.expense_id = e.id
            LEFT JOIN exchange_rates er ON e.currency = er.code
            WHERE :type = 'EXPENSE' AND ii.due_date >= :since AND ii.due_date < :until AND ii.status = 'Paid'
        )
    """
    )
    fun getTotalAmountByTypeBetween(since: Long, until: Long, type: String): Flow<Long?>

    @Query(
        """
        SELECT day, SUM(amount) as amount
        FROM (
            SELECT (date / 86400000) * 86400000 as day, SUM(final_price) as amount
            FROM expenses
            WHERE date >= :since AND date < :until AND type = 'EXPENSE' AND is_installment = 0
            GROUP BY day
            UNION ALL
            SELECT (due_date / 86400000) * 86400000 as day, SUM(amount) as amount
            FROM installment_items
            WHERE due_date >= :since AND due_date < :until AND status = 'Paid'
            GROUP BY day
        ) sub
        GROUP BY day
        ORDER BY day ASC
    """
    )
    fun getDailySpendingBetween(
        since: Long,
        until: Long
    ): Flow<List<com.sans.finance.data.local.entity.DaySpent>>

    @Query(
        """
        SELECT day, SUM(amount) as amount
        FROM (
            SELECT (date / 86400000) * 86400000 as day, SUM(final_price) as amount
            FROM expenses
            WHERE date >= :since AND date < :until AND type = :type AND category_id = :categoryId
            GROUP BY day
            UNION ALL
            SELECT (due_date / 86400000) * 86400000 as day, SUM(ii.amount) as amount
            FROM installment_items ii
            JOIN installments i ON ii.installment_id = i.id
            JOIN expenses e ON i.expense_id = e.id
            WHERE :type = 'EXPENSE' AND e.category_id = :categoryId AND ii.due_date >= :since AND ii.due_date < :until AND ii.status = 'Paid'
            GROUP BY day
        ) sub
        GROUP BY day
        ORDER BY day ASC
    """
    )
    fun getDailyBreakdownByCategoryBetween(
        since: Long,
        until: Long,
        categoryId: Long,
        type: String
    ): Flow<List<com.sans.finance.data.local.entity.DaySpent>>

    @Query(
        """
        SELECT day, SUM(amount) as amount
        FROM (
            SELECT CAST(strftime('%s', date / 1000, 'unixepoch', 'start of month') AS INTEGER) * 1000 as day, final_price as amount
            FROM expenses
            WHERE type = :type AND category_id = :categoryId AND is_installment = 0
            UNION ALL
            SELECT CAST(strftime('%s', due_date / 1000, 'unixepoch', 'start of month') AS INTEGER) * 1000 as day, amount
            FROM installment_items ii
            JOIN installments i ON ii.installment_id = i.id
            JOIN expenses e ON i.expense_id = e.id
            WHERE :type = 'EXPENSE' AND e.category_id = :categoryId AND ii.status = 'Paid'
        ) sub
        GROUP BY day
        ORDER BY day ASC
    """
    )
    fun getMonthlyBreakdownByCategory(
        categoryId: Long,
        type: String
    ): Flow<List<com.sans.finance.data.local.entity.DaySpent>>
}

