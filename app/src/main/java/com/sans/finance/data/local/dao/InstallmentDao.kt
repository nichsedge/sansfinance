package com.sans.finance.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sans.finance.data.local.entity.InstallmentEntity
import com.sans.finance.data.local.entity.InstallmentWithExpense
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallmentDao {
    @androidx.room.Transaction
    @Query("SELECT * FROM installments ORDER BY created_at DESC")
    fun getAllInstallments(): Flow<List<InstallmentWithExpense>>

    @androidx.room.Transaction
    @Query("SELECT * FROM installments WHERE status = 'Active' ORDER BY next_due_date ASC")
    fun getActiveInstallments(): Flow<List<InstallmentWithExpense>>

    @androidx.room.Transaction
    @Query("SELECT * FROM installments WHERE status = 'Completed' ORDER BY next_due_date ASC")
    fun getCompletedInstallments(): Flow<List<InstallmentWithExpense>>

    @Query("SELECT * FROM installments WHERE expense_id = :expenseId")
    suspend fun getInstallmentByExpenseId(expenseId: Long): InstallmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallment(installment: InstallmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallmentItem(item: com.sans.finance.data.local.entity.InstallmentItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallmentItems(items: List<com.sans.finance.data.local.entity.InstallmentItemEntity>)

    @Query("SELECT * FROM installment_items WHERE installment_id = :installmentId")
    fun getItemsByInstallmentId(installmentId: Long): Flow<List<com.sans.finance.data.local.entity.InstallmentItemEntity>>

    @Query("UPDATE installment_items SET status = :status WHERE id = :itemId")
    suspend fun updateInstallmentItemStatus(itemId: Long, status: String)

    @Query("SELECT * FROM installment_items WHERE id = :id")
    suspend fun getInstallmentItemById(id: Long): com.sans.finance.data.local.entity.InstallmentItemEntity?

    @Query("SELECT * FROM installments WHERE id = :id")
    suspend fun getInstallmentById(id: Long): InstallmentEntity?

    @Query("SELECT SUM(amount) FROM installment_items WHERE installment_id = :installmentId AND status = 'Paid'")
    suspend fun getPaidAmountForInstallment(installmentId: Long): Long?

    @Query("SELECT COUNT(*) FROM installment_items WHERE installment_id = :installmentId AND status = 'Pending'")
    suspend fun getPendingItemsCount(installmentId: Long): Int

    @Query("SELECT MIN(due_date) FROM installment_items WHERE installment_id = :installmentId AND status = 'Pending'")
    suspend fun getNextDueDateForInstallment(installmentId: Long): Long?

    @Query("SELECT SUM(amount) FROM installment_items WHERE status = 'Paid' AND due_date >= :since AND due_date < :until")
    fun getTotalPaidAmountBetween(since: Long, until: Long): Flow<Long?>

    @Query("SELECT * FROM installment_items WHERE status = 'Paid' AND due_date >= :since AND due_date < :until")
    fun getPaidItemsInDateRange(since: Long, until: Long): Flow<List<com.sans.finance.data.local.entity.InstallmentItemEntity>>

    @Query("""
        SELECT 
            ii.id as id, 
            ii.due_date as date, 
            e.item_name || ' (Installment ' || ii.month_number || ')' as item_name,
            ii.amount as amount,
            e.category_id as category_id,
            e.merchant as merchant,
            e.id as expense_id,
            (SELECT GROUP_CONCAT(t.name) FROM tags t JOIN expense_tag_ref etr ON t.id = etr.tagId WHERE etr.expenseId = e.id) as tags_list
        FROM installment_items ii
        JOIN installments i ON ii.installment_id = i.id
        JOIN expenses e ON i.expense_id = e.id
        WHERE ii.status = 'Paid' AND ii.due_date >= :since AND ii.due_date < :until
    """)


    fun getPaidInstallmentPaymentsBetween(since: Long, until: Long): Flow<List<com.sans.finance.data.local.entity.InstallmentPaymentRow>>

    @Update
    suspend fun updateInstallment(installment: InstallmentEntity)

    @Query("SELECT COUNT(*) FROM installments")
    suspend fun getInstallmentCount(): Int

    @Query("DELETE FROM installments WHERE expense_id = :expenseId")
    suspend fun deleteInstallmentByExpenseId(expenseId: Long)
}
