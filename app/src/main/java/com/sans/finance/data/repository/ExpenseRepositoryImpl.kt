package com.sans.finance.data.repository

import com.sans.finance.domain.model.*
import com.sans.finance.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import androidx.room.withTransaction

class ExpenseRepositoryImpl(
    private val db: com.sans.finance.data.local.AppDatabase,
    private val dao: com.sans.finance.data.local.dao.ExpenseDao,
    private val tagDao: com.sans.finance.data.local.dao.TagDao,
    private val categoryDao: com.sans.finance.data.local.dao.CategoryDao,
    private val installmentDao: com.sans.finance.data.local.dao.InstallmentDao,
    private val accountDao: com.sans.finance.data.local.dao.AccountDao
) : ExpenseRepository {

    companion object {
        private const val INSTALLMENT_PAYMENT_ID_OFFSET = 100_000_000L
    }

    override fun getAllExpenses(): Flow<List<Expense>> {
        return combine(
            dao.getAllExpenses(),
            installmentDao.getInstallmentPaymentsBetween(0, Long.MAX_VALUE)
        ) { expenseEntities: List<com.sans.finance.data.local.entity.ExpenseWithTags>, installmentRows: List<com.sans.finance.data.local.entity.InstallmentPaymentRow> ->
            val expenses = expenseEntities.map { it.toDomain() }
            val installmentPayments = installmentRows.map { it.toDomain() }
            (expenses + installmentPayments).sortedByDescending { it.date }
        }
    }

    override fun getExpensesBetween(since: Long, until: Long): Flow<List<Expense>> {
        return combine(
            dao.getExpensesBetween(since, until),
            installmentDao.getInstallmentPaymentsBetween(since, until)
        ) { expenseEntities: List<com.sans.finance.data.local.entity.ExpenseWithTags>, installmentRows: List<com.sans.finance.data.local.entity.InstallmentPaymentRow> ->
            val expenses = expenseEntities.map { it.toDomain() }
            val installmentPayments = installmentRows.map { it.toDomain() }
            (expenses + installmentPayments).sortedByDescending { it.date }
        }
    }

    override fun getRecurringExpenses(): Flow<List<Expense>> {
        return dao.getRecurringExpenses().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFilteredExpenses(
        query: String?,
        categoryIds: List<Long>,
        accountIds: List<Long>,
        since: Long,
        until: Long,
        minAmount: Long?,
        maxAmount: Long?,
        tags: List<String>,
        types: List<String>
    ): Flow<List<Expense>> {
        val searchQuery = if (query.isNullOrBlank()) null else query

        val expensesFlow = dao.getFilteredExpenses(
            searchQuery,
            categoryIds,
            categoryIds.size,
            accountIds,
            accountIds.size,
            since,
            until,
            minAmount,
            maxAmount,
            tags,
            tags.size,
            types,
            types.size
        )

        val installmentsFlow = installmentDao.getFilteredInstallmentPayments(
            since,
            until,
            searchQuery,
            categoryIds,
            categoryIds.size,
            accountIds,
            accountIds.size,
            minAmount,
            maxAmount,
            tags,
            tags.size,
            types,
            types.size
        )

        return combine(expensesFlow, installmentsFlow) { expenseEntities, installmentRows ->
            val expenses = expenseEntities.map { it.toDomain() }
            val installmentPayments = installmentRows.map { it.toDomain() }
            (expenses + installmentPayments).sortedByDescending { it.date }
        }
    }

    override suspend fun getExpenseById(id: Long): Expense? {
        return if (id >= INSTALLMENT_PAYMENT_ID_OFFSET) {
            val installmentItemId = id - INSTALLMENT_PAYMENT_ID_OFFSET
            // We return a pseudo-expense for the installment item
            // Note: Since InstallmentPaymentRow is internal to the DAO query, we might need a separate way to fetch it
            // or just fetch the installment item and map it.
            installmentDao.getInstallmentItemById(installmentItemId)?.let { item ->
                val installment = installmentDao.getInstallmentById(item.installmentId)
                val parentExpense = installment?.let { dao.getExpenseById(it.expenseId) }

                Expense(
                    id = id,
                    date = item.dueDate,
                    note = parentExpense?.expense?.note ?: "Installment",
                    amount = item.amount,
                    categoryId = parentExpense?.expense?.categoryId ?: 1L,
                    isInstallmentPayment = true,
                    installmentMonth = item.monthNumber,
                    installmentTotalMonths = installment?.durationMonths ?: 0,
                    status = item.status,
                    description = parentExpense?.expense?.description,
                    accountId = parentExpense?.expense?.accountId ?: 1L,
                    currency = parentExpense?.expense?.currency ?: "USD",
                    tags = parentExpense?.tags?.map { it.name } ?: emptyList()
                )
            }
        } else {
            dao.getExpenseById(id)?.toDomain()
        }
    }

    override suspend fun getNoteSuggestions(query: String): List<String> {
        return dao.getNoteSuggestions(query)
    }

    override suspend fun getTopFrequentNotes(limit: Int): List<String> {
        return dao.getTopFrequentNotes(limit)
    }

    override suspend fun getTopFrequentNotesByDay(dayOfWeek: Int, limit: Int): List<String> {
        return dao.getTopFrequentNotesByDay(dayOfWeek.toString(), limit)
    }

    override suspend fun getDescriptionSuggestions(query: String): List<String> {
        return dao.getDescriptionSuggestions(query)
    }
    
    override suspend fun getPredictionForNote(note: String): Expense? {
        return dao.getLastExpenseByNote(note)?.toDomain()
    }

    override suspend fun findPotentialDuplicate(note: String, amount: Long, date: Long, accountId: Long): Expense? {
        val window = 5 * 60 * 1000 // 5 minutes
        return dao.findDuplicateExpense(
            note = note,
            amount = amount,
            startTime = date - window,
            endTime = date + window,
            accountId = accountId
        )?.toDomain()
    }

    override suspend fun insertExpense(expense: Expense): Long = db.withTransaction {
        val expenseId = dao.insertExpense(expense.toEntity())
        syncTags(expenseId, expense.tags)
        adjustAccountBalance(expense, isReverse = false)
        expenseId
    }

    override suspend fun updateExpense(expense: Expense) = db.withTransaction {
        if (expense.id >= INSTALLMENT_PAYMENT_ID_OFFSET) {
            updateInstallmentPayment(expense)
            return@withTransaction
        }

        val oldExpense = dao.getExpenseById(expense.id)?.toDomain()

        if (oldExpense != null) {
            // Reverse old balance effect
            adjustAccountBalance(oldExpense, isReverse = true)
            
            // Apply new balance effect
            adjustAccountBalance(expense, isReverse = false)
            
            // Update expense and tags
            dao.updateExpense(expense.toEntity())
            syncTags(expense.id, expense.tags)
        }
    }

    private suspend fun updateInstallmentPayment(expense: Expense) {
        val itemId = expense.id - INSTALLMENT_PAYMENT_ID_OFFSET
        val oldItem = installmentDao.getInstallmentItemById(itemId)

        if (oldItem != null) {
            // If status changed from Pending to Paid, subtract from balance
            if (oldItem.status == "Pending" && expense.status == "Paid") {
                updateAccountBalance(expense.accountId, -expense.amount)

                // Update parent installment remaining balance
                val installment = installmentDao.getInstallmentById(oldItem.installmentId)
                if (installment != null) {
                    val newBalance = installment.remainingBalance - expense.amount
                    val nextDate = installmentDao.getNextDueDateForInstallment(installment.id)
                        ?: installment.nextDueDate
                    installmentDao.updateInstallment(
                        installment.copy(
                            remainingBalance = newBalance,
                            nextDueDate = nextDate,
                            status = if (newBalance <= 0) "Completed" else "Active"
                        )
                    )
                }
            } else if (oldItem.status == "Paid" && expense.status == "Pending") {
                // Reverse balance if changed back to Pending
                updateAccountBalance(expense.accountId, expense.amount)

                val installment = installmentDao.getInstallmentById(oldItem.installmentId)
                if (installment != null) {
                    val newBalance = installment.remainingBalance + expense.amount
                    val nextDate = installmentDao.getNextDueDateForInstallment(installment.id)
                        ?: installment.nextDueDate
                    installmentDao.updateInstallment(
                        installment.copy(
                            remainingBalance = newBalance,
                            nextDueDate = nextDate,
                            status = "Active"
                        )
                    )
                }
            } else if (oldItem.status == "Paid" && oldItem.amount != expense.amount) {
                // If amount changed and it was already paid, adjust balance
                val diff = oldItem.amount - expense.amount
                updateAccountBalance(expense.accountId, diff)
            }

            // Update the item itself
            installmentDao.insertInstallmentItem(
                oldItem.copy(
                    amount = expense.amount,
                    dueDate = expense.date,
                    status = expense.status
                )
            )
        }
    }

    private suspend fun adjustAccountBalance(expense: Expense, isReverse: Boolean) {
        if (expense.type == "TRANSFER") {
            val amount = if (isReverse) -expense.amount else expense.amount
            updateAccountBalance(expense.accountId, -amount)
            val toId = expense.toAccountId
            if (toId != null) {
                updateAccountBalance(toId, amount)
            }
        } else {
            val isIncome = expense.type == "INCOME"
            val multiplier = if (isReverse) -1 else 1
            val delta = if (isIncome) expense.amount * multiplier else -expense.amount * multiplier
            updateAccountBalance(expense.accountId, delta)
        }
    }

    private suspend fun updateAccountBalance(accountId: Long, amountDelta: Long) {
        accountDao.getAccountById(accountId)?.let { account ->
            accountDao.updateAccount(
                account.copy(
                    balance = account.balance + amountDelta,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun syncTags(expenseId: Long, tagNames: List<String>) {
        dao.deleteExpenseTagRefs(expenseId)
        val crossRefs = tagNames.map { tagName ->
            val existingTag = tagDao.getTagByName(tagName)
            val tagId = existingTag?.id
                ?: tagDao.insertTag(com.sans.finance.data.local.entity.TagEntity(name = tagName))
            com.sans.finance.data.local.entity.ExpenseTagCrossRef(expenseId, tagId)
        }
        if (crossRefs.isNotEmpty()) {
            dao.insertExpenseTagCrossRefs(crossRefs)
        }
    }

    override suspend fun deleteExpense(expense: Expense) = db.withTransaction {
        if (expense.id >= INSTALLMENT_PAYMENT_ID_OFFSET) {
            val itemId = expense.id - INSTALLMENT_PAYMENT_ID_OFFSET
            installmentDao.getInstallmentItemById(itemId)?.let { item ->
                // Mark as pending instead of deleting (since it's a scheduled payment)
                installmentDao.updateInstallmentItemStatus(itemId, "Pending")

                // Update installment remaining balance and status
                val installment = installmentDao.getInstallmentById(item.installmentId)
                if (installment != null) {
                    val newRemaining = installment.remainingBalance + item.amount
                    val nextDue = installmentDao.getNextDueDateForInstallment(item.installmentId)
                        ?: item.dueDate
                    installmentDao.updateInstallment(
                        installment.copy(
                            remainingBalance = newRemaining,
                            nextDueDate = nextDue,
                            status = "Active"
                        )
                    )
                }
            }
        } else {
            dao.deleteExpense(expense.toEntity())
        }

        // Update account balance (reverse the transaction)
        adjustAccountBalance(expense, isReverse = true)
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

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCategoriesByType(type: String): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
    }

    override suspend fun updateCategories(categories: List<Category>) {
        categoryDao.updateCategories(categories.map { it.toEntity() })
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category.toEntity())
    }

    override fun getAllTagEntities(): Flow<List<Tag>> {
        return tagDao.getAllTags().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateTag(tag: Tag) {
        tagDao.updateTag(tag.toEntity())
    }

    override suspend fun updateTags(tags: List<Tag>) {
        tagDao.updateTags(tags.map { it.toEntity() })
    }

    override suspend fun deleteTag(tag: Tag) {
        tagDao.deleteTag(tag.toEntity())
    }

    override suspend fun performDatabaseMaintenance() {
        db.withTransaction {
            tagDao.deleteOrphanedTags()

            // Balance re-sync
            val accounts = accountDao.getAllAccounts().first()
            val balances = mutableMapOf<Long, Long>()
            accounts.forEach { balances[it.id] = 0L }

            val expenses = dao.getAllExpenseEntities()
            expenses.forEach { exp ->
                if (exp.type == "TRANSFER") {
                    balances[exp.accountId] = (balances[exp.accountId] ?: 0L) - exp.finalPrice
                    val toId = exp.toAccountId
                    if (toId != null) {
                        balances[toId] = (balances[toId] ?: 0L) + exp.finalPrice
                    }
                } else if (exp.type == "INCOME") {
                    balances[exp.accountId] = (balances[exp.accountId] ?: 0L) + exp.finalPrice
                } else {
                    balances[exp.accountId] = (balances[exp.accountId] ?: 0L) - exp.finalPrice
                }
            }

            // Add installment payments
            val installmentItems =
                installmentDao.getInstallmentPaymentsBetween(0, Long.MAX_VALUE).first()
            installmentItems.forEach { item ->
                if (item.status == "Paid") {
                    balances[item.accountId] = (balances[item.accountId] ?: 0L) - item.amount
                }
            }

            accounts.forEach { account ->
                val newBalance = balances[account.id] ?: 0L
                if (account.balance != newBalance) {
                    accountDao.updateAccount(
                        account.copy(
                            balance = newBalance,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    override fun getSpendingByCategoryBetween(
        since: Long,
        until: Long
    ): Flow<List<CategorySpent>> {
        return dao.getSpendingByCategoryBetween(since, until).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getBreakdownByCategoryBetween(
        since: Long,
        until: Long,
        type: String
    ): Flow<List<CategorySpent>> {
        return dao.getBreakdownByCategoryBetween(since, until, type).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTotalAmountByTypeBetween(
        since: Long,
        until: Long,
        type: String
    ): Flow<Long?> {
        return dao.getTotalAmountByTypeBetween(since, until, type)
    }

    override fun getDailySpendingBetween(
        since: Long,
        until: Long
    ): Flow<List<DaySpent>> {
        return dao.getDailySpendingBetween(since, until).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getDailyBreakdownByCategoryBetween(
        since: Long,
        until: Long,
        categoryId: Long,
        type: String
    ): Flow<List<DaySpent>> {
        return dao.getDailyBreakdownByCategoryBetween(since, until, categoryId, type).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMonthlyBreakdownByCategory(
        categoryId: Long,
        type: String
    ): Flow<List<DaySpent>> {
        return dao.getMonthlyBreakdownByCategory(categoryId, type).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // Internal mapping extension
    private fun com.sans.finance.data.local.entity.ExpenseWithTags.toDomain(): Expense {
        val totalPaid = installment?.let { it.totalAmount - it.remainingBalance } ?: 0L
        return Expense(
            id = expense.id,
            date = expense.date,
            note = expense.note,
            amount = expense.finalPrice,
            categoryId = expense.categoryId,
            isRecurring = expense.isRecurring,
            isInstallment = expense.isInstallment,
            recurrenceInterval = expense.recurrenceInterval,
            nextDueDate = expense.nextDueDate,
            accountId = expense.accountId,
            toAccountId = expense.toAccountId,
            type = expense.type,
            description = expense.description,
            tags = tags.map { it.name },
            quantity = expense.quantity,
            currency = expense.currency,
            totalPaid = totalPaid,
            remainingBalance = installment?.remainingBalance ?: 0L,
            monthlyPayment = installment?.monthlyPayment ?: 0L
        )
    }

    private fun Expense.toEntity(): com.sans.finance.data.local.entity.ExpenseEntity {
        return com.sans.finance.data.local.entity.ExpenseEntity(
            id = id,
            date = date,
            note = note,
            finalPrice = amount,
            originalPrice = amount,
            categoryId = categoryId,
            isRecurring = isRecurring,
            isInstallment = isInstallment,
            recurrenceInterval = recurrenceInterval,
            nextDueDate = nextDueDate,
            accountId = accountId,
            toAccountId = toAccountId,
            type = type,
            description = description,
            quantity = quantity,
            currency = currency,
            status = "completed"
        )
    }

    private fun com.sans.finance.data.local.entity.InstallmentPaymentRow.toDomain(): Expense {
        return Expense(
            id = this.id + INSTALLMENT_PAYMENT_ID_OFFSET,
            amount = this.amount,
            date = this.date,
            note = this.note,
            type = "EXPENSE",
            categoryId = this.categoryId,
            isInstallmentPayment = true,
            installmentMonth = this.monthNumber,
            installmentTotalMonths = this.totalMonths,
            status = this.status,
            description = this.description,
            accountId = this.accountId,
            currency = this.currency,
            tags = this.tagsList?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
                ?: emptyList()
        )
    }

    private fun com.sans.finance.data.local.entity.CategoryEntity.toDomain() = Category(
        id = id,
        name = name,
        icon = icon,
        orderIndex = orderIndex,
        type = type
    )

    private fun Category.toEntity() = com.sans.finance.data.local.entity.CategoryEntity(
        id = id,
        name = name,
        icon = icon,
        orderIndex = orderIndex,
        type = type
    )

    private fun com.sans.finance.data.local.entity.TagEntity.toDomain() = Tag(
        id = id,
        name = name,
        orderIndex = orderIndex
    )

    private fun Tag.toEntity() = com.sans.finance.data.local.entity.TagEntity(
        id = id,
        name = name,
        orderIndex = orderIndex
    )

    private fun com.sans.finance.data.local.entity.CategorySpent.toDomain() = CategorySpent(
        categoryId = categoryId,
        categoryName = categoryName,
        categoryIcon = categoryIcon,
        totalAmount = totalAmount
    )

    private fun com.sans.finance.data.local.entity.DaySpent.toDomain() = DaySpent(
        day = day,
        amount = amount
    )
}
