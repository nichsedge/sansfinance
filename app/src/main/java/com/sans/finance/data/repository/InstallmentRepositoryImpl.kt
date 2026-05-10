package com.sans.finance.data.repository

import com.sans.finance.data.local.dao.InstallmentDao
import com.sans.finance.data.local.entity.InstallmentEntity
import com.sans.finance.domain.model.Installment
import com.sans.finance.domain.repository.InstallmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InstallmentRepositoryImpl(
    private val dao: InstallmentDao
) : InstallmentRepository {
    override fun getAllInstallments(): Flow<List<Installment>> {
        return dao.getAllInstallments().map { list -> list.map { it.toDomainModel() } }
    }

    override fun getActiveInstallments(): Flow<List<Installment>> {
        return dao.getActiveInstallments().map { list -> list.map { it.toDomainModel() } }
    }

    override fun getCompletedInstallments(): Flow<List<Installment>> {
        return dao.getCompletedInstallments().map { list -> list.map { it.toDomainModel() } }
    }

    override suspend fun getInstallmentByExpenseId(expenseId: Long): Installment? {
        return dao.getInstallmentByExpenseId(expenseId)?.toDomainModel()
    }

    override suspend fun createInstallment(installment: Installment): Long {
        return dao.insertInstallment(installment.toEntity())
    }

    override suspend fun createInstallmentItems(
        installmentId: Long,
        duration: Int,
        totalAmount: Long,
        startDate: Long
    ) {

        val monthlyAmount = totalAmount / duration
        val items = mutableListOf<com.sans.finance.data.local.entity.InstallmentItemEntity>()

        val calendar = java.util.Calendar.getInstance()

        for (i in 1..duration) {
            calendar.timeInMillis = startDate
            calendar.add(java.util.Calendar.MONTH, i - 1)

            val amount = if (i == duration) {
                totalAmount - (monthlyAmount * (duration - 1))
            } else {
                monthlyAmount
            }

            val item = com.sans.finance.data.local.entity.InstallmentItemEntity(
                installmentId = installmentId,
                amount = amount,
                dueDate = calendar.timeInMillis,
                status = "Pending",
                monthNumber = i
            )
            items.add(item)
        }
        dao.insertInstallmentItems(items)
    }

    override suspend fun updateInstallment(installment: Installment) {
        dao.updateInstallment(installment.toEntity())
    }

    override fun getInstallmentItems(installmentId: Long): Flow<List<com.sans.finance.domain.model.InstallmentItem>> {
        return dao.getItemsByInstallmentId(installmentId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateInstallmentItemStatus(itemId: Long, status: String) {
        dao.updateInstallmentItemStatus(itemId, status)

        // Recalculate parent installment remaining balance
        val item = dao.getInstallmentItemById(itemId)
        if (item != null) {
            val installmentId = item.installmentId
            val parent = dao.getInstallmentById(installmentId)
            if (parent != null) {
                val totalPaid = dao.getPaidAmountForInstallment(installmentId) ?: 0L
                val newBalance = parent.totalAmount - totalPaid
                val pendingCount = dao.getPendingItemsCount(installmentId)
                val newStatus = if (pendingCount == 0) "Completed" else "Active"
                dao.updateInstallment(parent.copy(remainingBalance = newBalance, status = newStatus))
            }
        }
    }

    override fun getTotalPaidAmountSince(since: Long): Flow<Long?> {
        return dao.getTotalPaidAmountBetween(since, Long.MAX_VALUE)
    }

    override fun getTotalPaidAmountBetween(since: Long, until: Long): Flow<Long?> {
        return dao.getTotalPaidAmountBetween(since, until)
    }

    override suspend fun deleteInstallmentByExpenseId(expenseId: Long) {
        dao.deleteInstallmentByExpenseId(expenseId)
    }

    override fun getPaidItemsInDateRange(
        since: Long,
        until: Long
    ): Flow<List<com.sans.finance.domain.model.InstallmentItem>> {
        return dao.getPaidItemsInDateRange(since, until).map { list -> list.map { it.toDomain() } }
    }

    private fun com.sans.finance.data.local.entity.InstallmentWithExpense.toDomainModel(): Installment {
        return installment.toDomainModel().copy(
            expenseName = expense.itemName,
            expenseDate = expense.date
        )
    }

    private fun InstallmentEntity.toDomainModel(): Installment {
        return Installment(
            id = id,
            expenseId = expenseId,
            totalAmount = totalAmount,
            monthlyPayment = monthlyPayment,
            durationMonths = durationMonths,
            remainingBalance = remainingBalance,
            nextDueDate = nextDueDate,
            status = status,
            createdAt = createdAt
        )
    }

    private fun Installment.toEntity(): InstallmentEntity {
        return InstallmentEntity(
            id = id,
            expenseId = expenseId,
            totalAmount = totalAmount,
            monthlyPayment = monthlyPayment,
            durationMonths = durationMonths,
            remainingBalance = remainingBalance,
            nextDueDate = nextDueDate,
            status = status,
            createdAt = createdAt
        )
    }

    private fun com.sans.finance.data.local.entity.InstallmentItemEntity.toDomain(): com.sans.finance.domain.model.InstallmentItem {
        return com.sans.finance.domain.model.InstallmentItem(
            id = id,
            installmentId = installmentId,
            amount = amount,
            dueDate = dueDate,
            status = status,
            monthNumber = monthNumber
        )
    }
}
