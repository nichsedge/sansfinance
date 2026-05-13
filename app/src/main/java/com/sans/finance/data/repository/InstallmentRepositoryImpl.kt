package com.sans.finance.data.repository

import com.sans.finance.data.local.dao.InstallmentDao
import com.sans.finance.data.local.entity.InstallmentEntity
import com.sans.finance.domain.model.Installment
import com.sans.finance.domain.repository.InstallmentRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InstallmentRepositoryImpl(
    private val dao: InstallmentDao
) : InstallmentRepository {
    override fun getAllInstallments(): Flow<List<Installment>> {
        return combine(
            dao.getAllInstallments(),
            dao.getAllInstallmentItems()
        ) { installments: List<com.sans.finance.data.local.entity.InstallmentWithExpense>, items: List<com.sans.finance.data.local.entity.InstallmentItemEntity> ->
            installments.map { it.toDomainModel(items) }
        }
    }

    override fun getActiveInstallments(): Flow<List<Installment>> {
        return combine(
            dao.getActiveInstallments(),
            dao.getAllInstallmentItems()
        ) { installments: List<com.sans.finance.data.local.entity.InstallmentWithExpense>, items: List<com.sans.finance.data.local.entity.InstallmentItemEntity> ->
            installments.map { it.toDomainModel(items) }
        }
    }

    override fun getCompletedInstallments(): Flow<List<Installment>> {
        return combine(
            dao.getCompletedInstallments(),
            dao.getAllInstallmentItems()
        ) { installments: List<com.sans.finance.data.local.entity.InstallmentWithExpense>, items: List<com.sans.finance.data.local.entity.InstallmentItemEntity> ->
            installments.map { it.toDomainModel(items) }
        }
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

        // Recalculate parent installment status
        val item = dao.getInstallmentItemById(itemId)
        if (item != null) {
            val installmentId = item.installmentId
            val parent = dao.getInstallmentById(installmentId)
            if (parent != null) {
                val pendingCount = dao.getPendingItemsCount(installmentId)
                val newStatus = if (pendingCount == 0) "Completed" else "Active"

                dao.updateInstallment(
                    parent.copy(
                        status = newStatus
                    )
                )
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

    private fun com.sans.finance.data.local.entity.InstallmentWithExpense.toDomainModel(
        allInstallmentItems: List<com.sans.finance.data.local.entity.InstallmentItemEntity>
    ): Installment {
        val items = allInstallmentItems.filter { it.installmentId == installment.id }
        val totalAmount = items.sumOf { it.amount }
        val remainingBalance = items.filter { it.status == "Pending" }.sumOf { it.amount }
        val nextDueDate = items.filter { it.status == "Pending" }.minOfOrNull { it.dueDate } ?: 0L
        val monthlyPayment = if (installment.durationMonths > 0) totalAmount / installment.durationMonths else 0L

        return Installment(
            id = installment.id,
            expenseId = installment.expenseId,
            totalAmount = totalAmount,
            monthlyPayment = monthlyPayment,
            durationMonths = installment.durationMonths,
            remainingBalance = remainingBalance,
            nextDueDate = nextDueDate,
            status = installment.status,
            createdAt = installment.createdAt,
            expenseName = expense.title,
            expenseDate = expense.date
        )
    }

    private suspend fun InstallmentEntity.toDomainModel(): Installment {
        val items = dao.getItemsByInstallmentIdForId(id)
        val totalAmount = items.sumOf { it.amount }
        val remainingBalance = items.filter { it.status == "Pending" }.sumOf { it.amount }
        val nextDueDate = items.filter { it.status == "Pending" }.minOfOrNull { it.dueDate } ?: 0L
        val monthlyPayment = if (durationMonths > 0) totalAmount / durationMonths else 0L

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
            status = status,
            durationMonths = durationMonths,
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
