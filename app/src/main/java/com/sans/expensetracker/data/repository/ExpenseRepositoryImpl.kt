package com.sans.expensetracker.data.repository

import com.sans.expensetracker.domain.model.Expense
import com.sans.expensetracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ExpenseRepositoryImpl(
    private val dao: com.sans.expensetracker.data.local.dao.ExpenseDao,
    private val tagDao: com.sans.expensetracker.data.local.dao.TagDao,
    private val categoryDao: com.sans.expensetracker.data.local.dao.CategoryDao,
    private val installmentDao: com.sans.expensetracker.data.local.dao.InstallmentDao
) : ExpenseRepository {

    override fun getAllExpenses(): Flow<List<Expense>> {
        return combine(
            dao.getAllExpenses(),
            installmentDao.getPaidInstallmentPaymentsBetween(0, Long.MAX_VALUE)
        ) { expenseEntities, installmentRows ->
            val expenses = expenseEntities.map { it.toDomain() }
            val installmentPayments = installmentRows.map { it.toDomain() }
            (expenses + installmentPayments).sortedByDescending { it.date }
        }
    }

    override fun getExpensesBetween(since: Long, until: Long): Flow<List<Expense>> {
        return combine(
            dao.getExpensesBetween(since, until),
            installmentDao.getPaidInstallmentPaymentsBetween(since, until)
        ) { expenseEntities, installmentRows ->
            val expenses = expenseEntities.map { it.toDomain() }
            val installmentPayments = installmentRows.map { it.toDomain() }
            (expenses + installmentPayments).sortedByDescending { it.date }
        }
    }

    override fun getFilteredExpenses(
        query: String?,
        categoryIds: List<Long>,
        since: Long,
        until: Long,
        minAmount: Long?,
        maxAmount: Long?,
        tags: List<String>
    ): Flow<List<Expense>> {
        val searchQuery = if (query.isNullOrBlank()) null else query
        
        val expensesFlow = dao.getFilteredExpenses(
            searchQuery,
            categoryIds,
            categoryIds.size,
            since,
            until,
            minAmount,
            maxAmount,
            tags,
            tags.size
        )

        // For filtered installments, we use a simpler filter logic in Kotlin for now
        // since mapping complex filters to SQL join is non-trivial for this dynamic use case
        val installmentsFlow = installmentDao.getPaidInstallmentPaymentsBetween(since, until)

        return combine(expensesFlow, installmentsFlow) { expenseEntities, installmentRows ->
            val expenses = expenseEntities.map { it.toDomain() }
            val installmentPayments = installmentRows.map { it.toDomain() }
                .filter { payment ->
                    val matchesQuery = searchQuery == null || payment.itemName.contains(searchQuery, ignoreCase = true)
                    val matchesCategory = categoryIds.isEmpty() || categoryIds.contains(payment.categoryId)
                    val matchesMinAmount = minAmount == null || payment.amount >= minAmount
                    val matchesMaxAmount = maxAmount == null || payment.amount <= maxAmount
                    // Tags are skipped for installments as they are sub-transactions of the parent
                    matchesQuery && matchesCategory && matchesMinAmount && matchesMaxAmount
                }
            
            (expenses + installmentPayments).sortedByDescending { it.date }
        }
    }

    override suspend fun getExpenseById(id: Long): Expense? {
        return dao.getExpenseById(id)?.toDomain()
    }

    override suspend fun insertExpense(expense: Expense): Long {
        val expenseId = dao.insertExpense(expense.toEntity())
        syncTags(expenseId, expense.tags)
        return expenseId
    }

    override suspend fun updateExpense(expense: Expense) {
        dao.updateExpense(expense.toEntity())
        syncTags(expense.id, expense.tags)
    }

    private suspend fun syncTags(expenseId: Long, tagNames: List<String>) {
        dao.deleteExpenseTagRefs(expenseId)
        val crossRefs = tagNames.map { tagName ->
            val existingTag = tagDao.getTagByName(tagName)
            val tagId = existingTag?.id
                ?: tagDao.insertTag(com.sans.expensetracker.data.local.entity.TagEntity(name = tagName))
            com.sans.expensetracker.data.local.entity.ExpenseTagCrossRef(expenseId, tagId)
        }
        if (crossRefs.isNotEmpty()) {
            dao.insertExpenseTagCrossRefs(crossRefs)
        }
    }

    override suspend fun deleteExpense(expense: Expense) {
        dao.deleteExpense(expense.toEntity())
    }

    override fun getTotalSpentSince(since: Long): Flow<Long?> {
        return dao.getTotalSpentSince(since)
    }

    override fun getTotalSpentBetween(since: Long, until: Long): Flow<Long?> {
        return dao.getTotalSpentBetween(since, until)
    }

    override fun getAllTimeSpent(): Flow<Long?> {
        return dao.getAllTimeSpent()
    }

    override fun getAllTags(): Flow<List<String>> {
        return tagDao.getAllTags().map { entities ->
            entities.map { it.name }
        }
    }

    override fun getAllCategories(): Flow<List<com.sans.expensetracker.data.local.entity.CategoryEntity>> {
        return categoryDao.getAllCategories()
    }

    override suspend fun insertCategory(category: com.sans.expensetracker.data.local.entity.CategoryEntity) {
        categoryDao.insertCategory(category)
    }

    override suspend fun updateCategory(category: com.sans.expensetracker.data.local.entity.CategoryEntity) {
        categoryDao.updateCategory(category)
    }

    override suspend fun updateCategories(categories: List<com.sans.expensetracker.data.local.entity.CategoryEntity>) {
        categoryDao.updateCategories(categories)
    }

    override suspend fun deleteCategory(category: com.sans.expensetracker.data.local.entity.CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    override fun getAllTagEntities(): Flow<List<com.sans.expensetracker.data.local.entity.TagEntity>> {
        return tagDao.getAllTags()
    }

    override suspend fun updateTag(tag: com.sans.expensetracker.data.local.entity.TagEntity) {
        tagDao.updateTag(tag)
    }

    override suspend fun updateTags(tags: List<com.sans.expensetracker.data.local.entity.TagEntity>) {
        tagDao.updateTags(tags)
    }

    override suspend fun deleteTag(tag: com.sans.expensetracker.data.local.entity.TagEntity) {
        tagDao.deleteTag(tag)
    }

    override fun getSpendingByCategoryBetween(
        since: Long,
        until: Long
    ): Flow<List<com.sans.expensetracker.data.local.entity.CategorySpent>> {
        return dao.getSpendingByCategoryBetween(since, until)
    }

    override fun getDailySpendingBetween(
        since: Long,
        until: Long
    ): Flow<List<com.sans.expensetracker.data.local.entity.DaySpent>> {
        return dao.getDailySpendingBetween(since, until)
    }

    // Internal mapping extension
    private fun com.sans.expensetracker.data.local.entity.ExpenseWithTags.toDomain(): Expense {
        val totalPaid = installment?.let { it.totalAmount - it.remainingBalance } ?: 0L
        return Expense(
            id = expense.id,
            date = expense.date,
            itemName = expense.itemName,
            amount = expense.finalPrice,
            categoryId = expense.categoryId,
            isRecurring = expense.isRecurring,
            isInstallment = expense.isInstallment,
            merchant = expense.merchant,
            tags = tags.map { it.name },
            quantity = expense.quantity,
            totalPaid = totalPaid,
            remainingBalance = installment?.remainingBalance ?: 0L,
            monthlyPayment = installment?.monthlyPayment ?: 0L
        )
    }

    private fun Expense.toEntity(): com.sans.expensetracker.data.local.entity.ExpenseEntity {
        return com.sans.expensetracker.data.local.entity.ExpenseEntity(
            id = id,
            date = date,
            itemName = itemName,
            finalPrice = amount,
            originalPrice = amount,
            categoryId = categoryId,
            isRecurring = isRecurring,
            isInstallment = isInstallment,
            merchant = merchant,
            platform = tags.firstOrNull(), // Keep for legacy if needed, or null
            quantity = quantity,
            status = "completed"
        )
    }

    private fun com.sans.expensetracker.data.local.entity.InstallmentPaymentRow.toDomain(): Expense {
        return Expense(
            id = id + 100_000_000L, // Offset to avoid conflict with ExpenseEntity IDs
            date = date,
            itemName = itemName,
            amount = amount,
            categoryId = categoryId,
            isInstallmentPayment = true,
            merchant = merchant
        )
    }
}
