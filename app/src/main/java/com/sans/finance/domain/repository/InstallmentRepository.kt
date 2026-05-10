package com.sans.finance.domain.repository

import com.sans.finance.domain.model.Installment
import kotlinx.coroutines.flow.Flow

interface InstallmentRepository {
    fun getAllInstallments(): Flow<List<Installment>>
    fun getActiveInstallments(): Flow<List<Installment>>
    fun getCompletedInstallments(): Flow<List<Installment>>
    suspend fun getInstallmentByExpenseId(expenseId: Long): Installment?
    suspend fun createInstallment(installment: Installment): Long
    suspend fun createInstallmentItems(
        installmentId: Long,
        duration: Int,
        totalAmount: Long,
        startDate: Long
    )

    suspend fun updateInstallment(installment: Installment)
    fun getInstallmentItems(installmentId: Long): Flow<List<com.sans.finance.domain.model.InstallmentItem>>
    suspend fun updateInstallmentItemStatus(itemId: Long, status: String)
    fun getTotalPaidAmountSince(since: Long): Flow<Long?>
    fun getTotalPaidAmountBetween(since: Long, until: Long): Flow<Long?>
    fun getPaidItemsInDateRange(
        since: Long,
        until: Long
    ): Flow<List<com.sans.finance.domain.model.InstallmentItem>>

    suspend fun deleteInstallmentByExpenseId(expenseId: Long)
}
