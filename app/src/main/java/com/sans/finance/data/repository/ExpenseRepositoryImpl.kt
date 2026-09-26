package com.sans.finance.data.repository

import androidx.room.withTransaction
import com.sans.finance.domain.model.AccountSyncDryRunResult
import com.sans.finance.domain.model.Category
import com.sans.finance.domain.model.CategorySpent
import com.sans.finance.domain.model.DaySpent
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.model.ReSyncMode
import com.sans.finance.domain.model.Tag
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.presentation.widget.FinancialSummaryWidgetProvider
import com.sans.finance.presentation.widget.QuickAddWidgetProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ExpenseRepositoryImpl(
    private val db: com.sans.finance.data.local.AppDatabase,
    private val dao: com.sans.finance.data.local.dao.ExpenseDao,
    private val tagDao: com.sans.finance.data.local.dao.TagDao,
    private val categoryDao: com.sans.finance.data.local.dao.CategoryDao,
    private val installmentDao: com.sans.finance.data.local.dao.InstallmentDao,
    private val accountDao: com.sans.finance.data.local.dao.AccountDao,
    private val context: android.content.Context? = null
) : ExpenseRepository {

    private val widgetScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.SupervisorJob())
    private var widgetDebounceJob: kotlinx.coroutines.Job? = null

    private fun notifyWidgets() {
        context?.let { ctx ->
            widgetDebounceJob?.cancel()
            widgetDebounceJob = widgetScope.launch {
                kotlinx.coroutines.delay(200)
                QuickAddWidgetProvider.updateAllWidgets(ctx)
                FinancialSummaryWidgetProvider.updateAllWidgets(ctx)
                com.sans.finance.presentation.util.DynamicShortcutManager.updateShortcuts(ctx)
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getAllExpenses(): Flow<List<Expense>> {
        val expensesFlow = dao.getAllExpenses()
        val installmentPaymentsFlow = installmentDao.getInstallmentPaymentsBetween(0, Long.MAX_VALUE)
        val recurringFlow = dao.getRecurringExpenses()

        return combine(
            expensesFlow,
            installmentPaymentsFlow,
            recurringFlow
        ) { e, i, r -> Triple(e, i, r) }.flatMapLatest { (expenseEntities, installmentRows, recurringEntities) ->
            val installmentIds = expenseEntities.mapNotNull { it.installment?.id }.distinct()
            val itemsFlow = if (installmentIds.isEmpty()) {
                flowOf(emptyList<com.sans.finance.data.local.entity.InstallmentItemEntity>())
            } else {
                installmentDao.getItemsByInstallmentIds(installmentIds)
            }

            itemsFlow.map { items ->
                val itemsByInstallment = items.groupBy { it.installmentId }
                val expenses = expenseEntities.map { it.toDomain(itemsByInstallment) }
                val installmentPayments = installmentRows.map { it.toDomain() }
                val recurringProjections = projectRecurringOccurrences(
                    recurringEntities = recurringEntities,
                    since = 0L,
                    until = Long.MAX_VALUE
                )
                (expenses + installmentPayments + recurringProjections).sortedByDescending { it.date }
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getExpensesBetween(since: Long, until: Long): Flow<List<Expense>> {
        val expensesFlow = dao.getExpensesBetween(since, until)
        val installmentPaymentsFlow = installmentDao.getInstallmentPaymentsBetween(since, until)
        val recurringFlow = dao.getRecurringExpenses()

        return combine(
            expensesFlow,
            installmentPaymentsFlow,
            recurringFlow
        ) { e, i, r -> Triple(e, i, r) }.flatMapLatest { (expenseEntities, installmentRows, recurringEntities) ->
            val installmentIds = expenseEntities.mapNotNull { it.installment?.id }.distinct()
            val itemsFlow = if (installmentIds.isEmpty()) {
                flowOf(emptyList<com.sans.finance.data.local.entity.InstallmentItemEntity>())
            } else {
                installmentDao.getItemsByInstallmentIds(installmentIds)
            }

            itemsFlow.map { items ->
                val itemsByInstallment = items.groupBy { it.installmentId }
                val expenses = expenseEntities.map { it.toDomain(itemsByInstallment) }
                val installmentPayments = installmentRows.map { it.toDomain() }
                val recurringProjections = projectRecurringOccurrences(
                    recurringEntities = recurringEntities,
                    since = since,
                    until = until
                )
                (expenses + installmentPayments + recurringProjections).sortedByDescending { it.date }
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getRecurringExpenses(): Flow<List<Expense>> {
        return dao.getRecurringExpenses().flatMapLatest { entities ->
            val installmentIds = entities.mapNotNull { it.installment?.id }.distinct()
            val itemsFlow = if (installmentIds.isEmpty()) {
                flowOf(emptyList<com.sans.finance.data.local.entity.InstallmentItemEntity>())
            } else {
                installmentDao.getItemsByInstallmentIds(installmentIds)
            }

            itemsFlow.map { items ->
                val itemsByInstallment = items.groupBy { it.installmentId }
                entities.map { it.toDomain(itemsByInstallment) }
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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

        val recurringFlow = dao.getRecurringExpenses()

        return combine(
            expensesFlow,
            installmentsFlow,
            recurringFlow
        ) { e, i, r -> Triple(e, i, r) }.flatMapLatest { (expenseEntities, installmentRows, recurringEntities) ->
            val installmentIds = expenseEntities.mapNotNull { it.installment?.id }.distinct()
            val itemsFlow = if (installmentIds.isEmpty()) {
                flowOf(emptyList<com.sans.finance.data.local.entity.InstallmentItemEntity>())
            } else {
                installmentDao.getItemsByInstallmentIds(installmentIds)
            }

            itemsFlow.map { items ->
                val itemsByInstallment = items.groupBy { it.installmentId }
                val expenses = expenseEntities.map { it.toDomain(itemsByInstallment) }
                val installmentPayments = installmentRows.map { it.toDomain() }
                val recurringProjections = projectRecurringOccurrences(
                    recurringEntities = recurringEntities,
                    since = since,
                    until = until,
                    query = searchQuery,
                    categoryIds = categoryIds,
                    accountIds = accountIds,
                    minAmount = minAmount,
                    maxAmount = maxAmount,
                    tags = tags,
                    types = types
                )
                (expenses + installmentPayments + recurringProjections).sortedByDescending { it.date }
            }
        }
    }

    override suspend fun getExpenseById(id: Long): Expense? {
        if (com.sans.finance.core.util.RecurringOccurrenceCalculator.isSyntheticRecurringId(id)) {
            val parentId = com.sans.finance.core.util.RecurringOccurrenceCalculator.extractParentRuleId(id)
            val occIndex = com.sans.finance.core.util.RecurringOccurrenceCalculator.extractOccurrenceIndex(id)
            val parent = dao.getExpenseById(parentId)?.let {
                val items = installmentDao.getItemsByInstallmentIdForId(it.installment?.id ?: -1)
                it.toDomain(items.groupBy { it.installmentId })
            } ?: return null

            return parent.copy(
                id = id,
                isRecurringInstance = true,
                parentRecurringId = parentId,
                recurringOccurrenceIndex = occIndex
            )
        }

        // For positive IDs, always check the expenses table first
        if (id > 0) {
            val directExpense = dao.getExpenseById(id)?.let {
                val items = installmentDao.getItemsByInstallmentIdForId(it.installment?.id ?: -1)
                it.toDomain(items.groupBy { it.installmentId })
            }
            if (directExpense != null) {
                return directExpense
            }
        }

        // Negative IDs are synthetic installment payment references: id = -installmentItemId
        if (id < 0) {
            val installmentItemId = -id
            return installmentDao.getInstallmentItemById(installmentItemId)?.let { item ->
                val installment = installmentDao.getInstallmentById(item.installmentId)
                val parentExpense = installment?.let { dao.getExpenseById(it.expenseId) }

                Expense(
                    id = id,
                    date = item.dueDate,
                    title = parentExpense?.expense?.title ?: "Installment",
                    amount = item.amount,
                    categoryId = parentExpense?.expense?.categoryId ?: 1L,
                    isInstallmentPayment = true,
                    installmentMonth = item.monthNumber,
                    installmentTotalMonths = installment?.durationMonths ?: 0,
                    status = item.status,
                    details = parentExpense?.expense?.details,
                    accountId = parentExpense?.expense?.accountId ?: 1L,
                    currency = parentExpense?.expense?.currency ?: "USD",
                    tags = parentExpense?.tags?.map { it.name } ?: emptyList(),
                    categoryName = parentExpense?.category?.name,
                    categoryIcon = parentExpense?.category?.icon
                )
            }
        }
        return null
    }

    override suspend fun getTitleSuggestions(query: String): List<String> {
        return dao.getTitleSuggestions(query)
    }

    override suspend fun getTopFrequentTitles(limit: Int): List<String> {
        return dao.getTopFrequentTitles(limit)
    }

    override suspend fun getTopFrequentTitlesByDay(dayOfWeek: Int, limit: Int): List<String> {
        return dao.getTopFrequentTitlesByDay(dayOfWeek.toString(), limit)
    }

    override suspend fun getDetailsSuggestions(query: String): List<String> {
        return dao.getDetailsSuggestions(query)
    }

    override suspend fun getPredictionForTitle(title: String): Expense? {
        return dao.getLastExpenseByTitle(title)?.let {
            val items = installmentDao.getItemsByInstallmentIdForId(it.installment?.id ?: -1)
            it.toDomain(items.groupBy { it.installmentId })
        }
    }

    override suspend fun findPotentialDuplicate(
        title: String,
        amount: Long,
        date: Long,
        accountId: Long
    ): Expense? {
        val window = 5 * 60 * 1000 // 5 minutes
        return dao.findDuplicateExpense(
            title = title,
            amount = amount,
            startTime = date - window,
            endTime = date + window,
            accountId = accountId
        )?.let {
            val items = installmentDao.getItemsByInstallmentIdForId(it.installment?.id ?: -1)
            it.toDomain(items.groupBy { it.installmentId })
        }
    }

    override suspend fun insertExpense(expense: Expense): Long {
        val expenseToInsert = if (expense.isInstallmentPayment) {
            expense.copy(id = 0)
        } else {
            expense
        }
        val expenseId = dao.insertExpense(expenseToInsert.toEntity())
        notifyWidgets()
        return expenseId
    }

    override suspend fun updateExpense(expense: Expense) {
        if (com.sans.finance.core.util.RecurringOccurrenceCalculator.isSyntheticRecurringId(expense.id) ||
            (expense.isRecurringInstance && expense.parentRecurringId != null)
        ) {
            val parentId = expense.parentRecurringId ?: com.sans.finance.core.util.RecurringOccurrenceCalculator.extractParentRuleId(expense.id)
            val parentEntity = dao.getExpenseById(parentId)?.expense
            if (parentEntity != null) {
                dao.updateExpense(
                    expense.copy(id = parentId, isRecurringInstance = false).toEntity()
                )
                notifyWidgets()
                return
            }
        }

        if (expense.isInstallmentPayment && expense.id < 0) {
            val itemId = -expense.id
            val oldItem = installmentDao.getInstallmentItemById(itemId)
            if (oldItem != null) {
                installmentDao.insertInstallmentItem(
                    oldItem.copy(
                        amount = expense.amount,
                        dueDate = expense.date,
                        status = expense.status
                    )
                )
                // Update parent installment status
                val installment = installmentDao.getInstallmentById(oldItem.installmentId)
                if (installment != null) {
                    installmentDao.updateInstallment(
                        installment.copy(
                            status = if (installmentDao.getPendingItemsCount(installment.id) == 0) "Completed" else "Active"
                        )
                    )
                }
                notifyWidgets()
                return
            }
        }
        dao.updateExpense(expense.toEntity())
        notifyWidgets()
    }

    override suspend fun deleteExpense(expense: Expense) {
        if (com.sans.finance.core.util.RecurringOccurrenceCalculator.isSyntheticRecurringId(expense.id) ||
            (expense.isRecurringInstance && expense.parentRecurringId != null)
        ) {
            val parentId = expense.parentRecurringId ?: com.sans.finance.core.util.RecurringOccurrenceCalculator.extractParentRuleId(expense.id)
            dao.getExpenseById(parentId)?.let {
                dao.deleteExpense(it.expense)
            }
            notifyWidgets()
            return
        }

        if (expense.isInstallmentPayment && expense.id < 0) {
            val itemId = -expense.id
            val item = installmentDao.getInstallmentItemById(itemId)
            if (item != null) {
                installmentDao.updateInstallmentItemStatus(itemId, "Pending")
                val installment = installmentDao.getInstallmentById(item.installmentId)
                if (installment != null) {
                    installmentDao.updateInstallment(
                        installment.copy(status = "Active")
                    )
                }
                notifyWidgets()
                return
            }
        }
        dao.deleteExpense(expense.toEntity())
        notifyWidgets()
    }

    override fun getTotalSpentSince(since: Long): Flow<Long?> {
        return getTotalSpentBetween(since, Long.MAX_VALUE)
    }

    override fun getTotalSpentBetween(since: Long, until: Long): Flow<Long?> {
        return getTotalAmountByTypeBetween(since, until, "EXPENSE")
    }

    override fun getAllTimeSpent(): Flow<Long?> {
        return getTotalSpentBetween(0L, Long.MAX_VALUE)
    }

    override fun getOldestExpenseDate(): Flow<Long?> {
        return dao.getOldestExpenseDate()
    }

    override suspend fun getReSyncBalancesDryRun(): List<AccountSyncDryRunResult> {
        val accounts = accountDao.getAllAccounts().first()
        val balances = mutableMapOf<Long, Long>()
        accounts.forEach { balances[it.id] = 0L }

        val expenses = dao.getAllExpenseEntities()
        expenses.forEach { exp ->
            if (exp.type == "TRANSFER") {
                balances[exp.accountId] = (balances[exp.accountId] ?: 0L) - exp.amount
                val toId = exp.toAccountId
                if (toId != null) {
                    balances[toId] = (balances[toId] ?: 0L) + exp.amount
                }
            } else if (exp.type == "INCOME") {
                balances[exp.accountId] = (balances[exp.accountId] ?: 0L) + exp.amount
            } else {
                balances[exp.accountId] = (balances[exp.accountId] ?: 0L) - exp.amount
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

        return accounts.map { account ->
            val calculated = balances[account.id] ?: 0L
            AccountSyncDryRunResult(
                accountId = account.id,
                accountName = account.name,
                currentBalance = account.balance,
                calculatedBalance = calculated,
                currency = account.currency
            )
        }
    }

    override suspend fun reSyncAccountBalances(mode: ReSyncMode, adjustmentDate: Long) {
        db.withTransaction {
            val accounts = accountDao.getAllAccounts().first()
            val dryRunResults = getReSyncBalancesDryRun()

            if (mode == ReSyncMode.TRANSACTIONS_AS_TRUTH) {
                accounts.forEach { account ->
                    val calc = dryRunResults.firstOrNull { it.accountId == account.id }?.calculatedBalance ?: 0L
                    if (account.balance != calc) {
                        accountDao.updateAccount(
                            account.copy(
                                balance = calc,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            } else {
                val categories = categoryDao.getAllCategoriesSync()
                val adjustmentTag = tagDao.getTagByName("Adjustment")
                val tagId = adjustmentTag?.id ?: tagDao.insertTag(
                    com.sans.finance.data.local.entity.TagEntity(name = "Adjustment")
                )

                dryRunResults.forEach { result ->
                    if (result.isDifferenceExist) {
                        val delta = result.delta
                        val existingAdjustment = dao.getAllExpenseEntities().firstOrNull { exp ->
                            exp.accountId == result.accountId &&
                            exp.date == adjustmentDate &&
                            exp.title == "Balance Adjustment"
                        }

                        if (existingAdjustment != null) {
                            val existingEffect = if (existingAdjustment.type == "INCOME") existingAdjustment.amount else -existingAdjustment.amount
                            val newEffect = existingEffect - delta
                            if (newEffect == 0L) {
                                dao.deleteExpense(existingAdjustment)
                            } else {
                                val newType = if (newEffect > 0L) "INCOME" else "EXPENSE"
                                val newAmount = kotlin.math.abs(newEffect)
                                val targetCategory = categories.firstOrNull {
                                    it.type == newType && (it.name.contains("Misc", ignoreCase = true) || it.name.contains("Other", ignoreCase = true))
                                } ?: categories.firstOrNull { it.type == newType }

                                val resolvedCategoryId = targetCategory?.id
                                    ?: categories.firstOrNull { it.type == newType }?.id
                                    ?: categories.firstOrNull()?.id
                                    ?: 1L

                                dao.updateExpense(
                                    existingAdjustment.copy(
                                        amount = newAmount,
                                        type = newType,
                                        categoryId = resolvedCategoryId,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                                dao.insertExpenseTagCrossRefs(listOf(
                                    com.sans.finance.data.local.entity.ExpenseTagCrossRef(existingAdjustment.id, tagId)
                                ))
                            }
                        } else {
                            val isIncome = delta < 0L
                            val type = if (isIncome) "INCOME" else "EXPENSE"
                            val amount = kotlin.math.abs(delta)
                            val targetCategory = categories.firstOrNull {
                                it.type == type && (it.name.contains("Misc", ignoreCase = true) || it.name.contains("Other", ignoreCase = true))
                            } ?: categories.firstOrNull { it.type == type }

                            val resolvedCategoryId = targetCategory?.id
                                ?: categories.firstOrNull { it.type == type }?.id
                                ?: categories.firstOrNull()?.id
                                ?: 1L

                            val expenseEntity = com.sans.finance.data.local.entity.ExpenseEntity(
                                date = adjustmentDate,
                                title = "Balance Adjustment",
                                details = "System generated to match actual account balance during re-sync.",
                                amount = amount,
                                categoryId = resolvedCategoryId,
                                accountId = result.accountId,
                                type = type,
                                currency = result.currency,
                                status = "Paid",
                                isRecurring = false,
                                isInstallment = false
                            )
                            val newId = dao.insertExpense(expenseEntity)
                            dao.insertExpenseTagCrossRefs(listOf(
                                com.sans.finance.data.local.entity.ExpenseTagCrossRef(newId, tagId)
                            ))
                        }
                    }
                }
            }
        }
    }

    override fun getSpendingByCategoryBetween(
        since: Long,
        until: Long
    ): Flow<List<CategorySpent>> {
        return getBreakdownByCategoryBetween(since, until, "EXPENSE")
    }

    override fun getBreakdownByCategoryBetween(
        since: Long,
        until: Long,
        type: String
    ): Flow<List<CategorySpent>> {
        val breakdownFlow = dao.getBreakdownByCategoryBetween(since, until, type)
        val recurringFlow = dao.getRecurringExpenses()
        val ratesFlow = db.currencyDao.getAllRates()

        return combine(breakdownFlow, recurringFlow, ratesFlow) { dbBreakdown, recurringEntities, rates ->
            val ratesMap = rates.associate { it.code to it.rateToIdr }
            val projectedExpenses = projectRecurringOccurrences(
                recurringEntities = recurringEntities,
                since = since,
                until = until,
                types = listOf(type)
            )

            if (projectedExpenses.isEmpty()) {
                dbBreakdown
            } else {
                val categoryMap = dbBreakdown.associateBy { it.categoryId }.toMutableMap()
                for (expense in projectedExpenses) {
                    val rate = if (expense.currency == "IDR") 1.0 else ratesMap[expense.currency] ?: 1.0
                    val amountInIdr = (expense.amount * rate).toLong()
                    val existing = categoryMap[expense.categoryId]
                    if (existing != null) {
                        categoryMap[expense.categoryId] = existing.copy(
                            totalAmount = existing.totalAmount + amountInIdr
                        )
                    } else {
                        categoryMap[expense.categoryId] = CategorySpent(
                            categoryId = expense.categoryId,
                            categoryName = expense.categoryName ?: "Uncategorized",
                            categoryIcon = expense.categoryIcon ?: "📁",
                            totalAmount = amountInIdr
                        )
                    }
                }
                categoryMap.values.sortedByDescending { it.totalAmount }
            }
        }
    }

    override fun getTotalAmountByTypeBetween(
        since: Long,
        until: Long,
        type: String
    ): Flow<Long?> {
        val totalFlow = dao.getTotalAmountByTypeBetween(since, until, type)
        val recurringFlow = dao.getRecurringExpenses()
        val ratesFlow = db.currencyDao.getAllRates()

        return combine(totalFlow, recurringFlow, ratesFlow) { dbTotal, recurringEntities, rates ->
            val ratesMap = rates.associate { it.code to it.rateToIdr }
            val projectedExpenses = projectRecurringOccurrences(
                recurringEntities = recurringEntities,
                since = since,
                until = until,
                types = listOf(type)
            )

            val baseTotal = dbTotal ?: 0L
            val projectedTotal = projectedExpenses.sumOf { expense ->
                val rate = if (expense.currency == "IDR") 1.0 else ratesMap[expense.currency] ?: 1.0
                (expense.amount * rate).toLong()
            }

            baseTotal + projectedTotal
        }
    }

    override fun getDailySpendingBetween(
        since: Long,
        until: Long
    ): Flow<List<DaySpent>> {
        val dailyFlow = dao.getDailySpendingBetween(since, until)
        val recurringFlow = dao.getRecurringExpenses()
        val ratesFlow = db.currencyDao.getAllRates()

        return combine(dailyFlow, recurringFlow, ratesFlow) { dbDaily, recurringEntities, rates ->
            val ratesMap = rates.associate { it.code to it.rateToIdr }
            val projectedExpenses = projectRecurringOccurrences(
                recurringEntities = recurringEntities,
                since = since,
                until = until,
                types = listOf("EXPENSE")
            )

            if (projectedExpenses.isEmpty()) {
                dbDaily
            } else {
                val dailyMap = dbDaily.associateBy { it.day }.toMutableMap()
                for (expense in projectedExpenses) {
                    val dayMs = (expense.date / 86400000L) * 86400000L
                    val rate = if (expense.currency == "IDR") 1.0 else ratesMap[expense.currency] ?: 1.0
                    val amountInIdr = (expense.amount * rate).toLong()
                    val existing = dailyMap[dayMs]
                    if (existing != null) {
                        dailyMap[dayMs] = existing.copy(amount = existing.amount + amountInIdr)
                    } else {
                        dailyMap[dayMs] = DaySpent(day = dayMs, amount = amountInIdr)
                    }
                }
                dailyMap.values.sortedBy { it.day }
            }
        }
    }

    override fun getDailyBreakdownByCategoryBetween(
        since: Long,
        until: Long,
        categoryId: Long,
        type: String
    ): Flow<List<DaySpent>> {
        val dailyFlow = dao.getDailyBreakdownByCategoryBetween(since, until, categoryId, type)
        val recurringFlow = dao.getRecurringExpenses()
        val ratesFlow = db.currencyDao.getAllRates()

        return combine(dailyFlow, recurringFlow, ratesFlow) { dbDaily, recurringEntities, rates ->
            val ratesMap = rates.associate { it.code to it.rateToIdr }
            val projectedExpenses = projectRecurringOccurrences(
                recurringEntities = recurringEntities,
                since = since,
                until = until,
                categoryIds = listOf(categoryId),
                types = listOf(type)
            )

            if (projectedExpenses.isEmpty()) {
                dbDaily
            } else {
                val dailyMap = dbDaily.associateBy { it.day }.toMutableMap()
                for (expense in projectedExpenses) {
                    val dayMs = (expense.date / 86400000L) * 86400000L
                    val rate = if (expense.currency == "IDR") 1.0 else ratesMap[expense.currency] ?: 1.0
                    val amountInIdr = (expense.amount * rate).toLong()
                    val existing = dailyMap[dayMs]
                    if (existing != null) {
                        dailyMap[dayMs] = existing.copy(amount = existing.amount + amountInIdr)
                    } else {
                        dailyMap[dayMs] = DaySpent(day = dayMs, amount = amountInIdr)
                    }
                }
                dailyMap.values.sortedBy { it.day }
            }
        }
    }

    override fun getMonthlyBreakdownByCategory(
        categoryId: Long,
        type: String
    ): Flow<List<DaySpent>> {
        val monthlyFlow = dao.getMonthlyBreakdownByCategory(categoryId, type)
        val recurringFlow = dao.getRecurringExpenses()
        val ratesFlow = db.currencyDao.getAllRates()

        return combine(monthlyFlow, recurringFlow, ratesFlow) { dbMonthly, recurringEntities, rates ->
            val ratesMap = rates.associate { it.code to it.rateToIdr }
            val now = System.currentTimeMillis()
            val projectedExpenses = projectRecurringOccurrences(
                recurringEntities = recurringEntities,
                since = 0L,
                until = now,
                categoryIds = listOf(categoryId),
                types = listOf(type)
            )

            if (projectedExpenses.isEmpty()) {
                dbMonthly
            } else {
                val monthlyMap = dbMonthly.associateBy { getStartOfMonth(it.day) }.toMutableMap()
                for (expense in projectedExpenses) {
                    val monthMs = getStartOfMonth(expense.date)
                    val rate = if (expense.currency == "IDR") 1.0 else ratesMap[expense.currency] ?: 1.0
                    val amountInIdr = (expense.amount * rate).toLong()
                    val existing = monthlyMap[monthMs]
                    if (existing != null) {
                        monthlyMap[monthMs] = existing.copy(amount = existing.amount + amountInIdr)
                    } else {
                        monthlyMap[monthMs] = DaySpent(day = monthMs, amount = amountInIdr)
                    }
                }
                monthlyMap.values.sortedBy { it.day }
            }
        }
    }

    override fun getMonthlyBreakdownByCategoryBetween(
        since: Long,
        until: Long,
        categoryId: Long,
        type: String
    ): Flow<List<DaySpent>> {
        val monthlyFlow = dao.getMonthlyBreakdownByCategoryBetween(since, until, categoryId, type)
        val recurringFlow = dao.getRecurringExpenses()
        val ratesFlow = db.currencyDao.getAllRates()

        return combine(monthlyFlow, recurringFlow, ratesFlow) { dbMonthly, recurringEntities, rates ->
            val ratesMap = rates.associate { it.code to it.rateToIdr }
            val now = System.currentTimeMillis()
            val effectiveUntil = minOf(until, now)
            val projectedExpenses = if (since < effectiveUntil) {
                projectRecurringOccurrences(
                    recurringEntities = recurringEntities,
                    since = since,
                    until = effectiveUntil,
                    categoryIds = listOf(categoryId),
                    types = listOf(type)
                )
            } else {
                emptyList()
            }

            if (projectedExpenses.isEmpty()) {
                dbMonthly
            } else {
                val monthlyMap = dbMonthly.associateBy { getStartOfMonth(it.day) }.toMutableMap()
                for (expense in projectedExpenses) {
                    val monthMs = getStartOfMonth(expense.date)
                    val rate = if (expense.currency == "IDR") 1.0 else ratesMap[expense.currency] ?: 1.0
                    val amountInIdr = (expense.amount * rate).toLong()
                    val existing = monthlyMap[monthMs]
                    if (existing != null) {
                        monthlyMap[monthMs] = existing.copy(amount = existing.amount + amountInIdr)
                    } else {
                        monthlyMap[monthMs] = DaySpent(day = monthMs, amount = amountInIdr)
                    }
                }
                monthlyMap.values.sortedBy { it.day }
            }
        }
    }

    private fun getStartOfMonth(dateMs: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = dateMs
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun com.sans.finance.data.local.entity.ExpenseWithTags.toDomain(
        itemsByInstallment: Map<Long, List<com.sans.finance.data.local.entity.InstallmentItemEntity>>? = null
    ): Expense {
        val items = installment?.id?.let { itemsByInstallment?.get(it) }
        val totalPaid = items?.filter { it.status == "Paid" }?.sumOf { it.amount } ?: 0L
        val remainingBalance = items?.filter { it.status == "Pending" }?.sumOf { it.amount } ?: 0L
        val totalAmount = items?.sumOf { it.amount } ?: 0L
        val inst = installment
        val monthlyPayment =
            if (inst != null && inst.durationMonths > 0) totalAmount / inst.durationMonths else 0L

        return Expense(
            id = expense.id,
            date = expense.date,
            title = expense.title,
            amount = expense.amount,
            categoryId = expense.categoryId,
            isRecurring = expense.isRecurring,
            isInstallment = expense.isInstallment,
            recurrenceInterval = expense.recurrenceInterval,
            nextDueDate = expense.nextDueDate,
            recurrenceEndType = expense.recurrenceEndType ?: "NEVER",
            recurrenceEndDate = expense.recurrenceEndDate,
            recurrenceTotalOccurrences = expense.recurrenceTotalOccurrences,
            recurrenceIntervalMultiplier = expense.recurrenceIntervalMultiplier,
            recurrenceStatus = expense.recurrenceStatus,
            accountId = expense.accountId,
            toAccountId = expense.toAccountId,
            type = expense.type,
            details = expense.details,
            tags = tags.map { it.name },
            currency = expense.currency,
            status = expense.status,
            totalPaid = totalPaid,
            remainingBalance = remainingBalance,
            monthlyPayment = monthlyPayment,
            categoryName = category?.name,
            categoryIcon = category?.icon
        )
    }

    private fun Expense.toEntity(): com.sans.finance.data.local.entity.ExpenseEntity {
        return com.sans.finance.data.local.entity.ExpenseEntity(
            id = id,
            date = date,
            title = title,
            amount = amount,
            categoryId = categoryId,
            isRecurring = isRecurring,
            isInstallment = isInstallment,
            recurrenceInterval = recurrenceInterval,
            nextDueDate = nextDueDate,
            recurrenceEndType = recurrenceEndType,
            recurrenceEndDate = recurrenceEndDate,
            recurrenceTotalOccurrences = recurrenceTotalOccurrences,
            recurrenceIntervalMultiplier = recurrenceIntervalMultiplier,
            recurrenceStatus = recurrenceStatus,
            accountId = accountId,
            toAccountId = toAccountId,
            type = type,
            details = details,
            currency = currency,
            status = status
        )
    }

    private fun projectRecurringOccurrences(
        recurringEntities: List<com.sans.finance.data.local.entity.ExpenseWithTags>,
        since: Long,
        until: Long,
        query: String? = null,
        categoryIds: List<Long> = emptyList(),
        accountIds: List<Long> = emptyList(),
        minAmount: Long? = null,
        maxAmount: Long? = null,
        tags: List<String> = emptyList(),
        types: List<String> = emptyList()
    ): List<Expense> {
        val projected = mutableListOf<Expense>()
        val now = System.currentTimeMillis()

        for (entity in recurringEntities) {
            val rule = entity.toDomain()
            if (rule.recurrenceStatus.equals("CANCELLED", ignoreCase = true) ||
                rule.recurrenceStatus.equals("PAUSED", ignoreCase = true)
            ) {
                continue
            }

            if (!query.isNullOrBlank()) {
                val matchesTitle = rule.title.contains(query, ignoreCase = true)
                val matchesDetails = rule.details?.contains(query, ignoreCase = true) == true
                if (!matchesTitle && !matchesDetails) continue
            }
            if (categoryIds.isNotEmpty() && !categoryIds.contains(rule.categoryId)) continue
            if (accountIds.isNotEmpty() && !accountIds.contains(rule.accountId)) continue
            if (minAmount != null && rule.amount < minAmount) continue
            if (maxAmount != null && rule.amount > maxAmount) continue
            if (tags.isNotEmpty() && !rule.tags.any { tags.contains(it) }) continue
            if (types.isNotEmpty() && !types.contains(rule.type)) continue

            val occurrences = com.sans.finance.core.util.RecurringOccurrenceCalculator.calculateOccurrences(
                startDate = rule.date,
                interval = rule.recurrenceInterval,
                multiplier = rule.recurrenceIntervalMultiplier,
                endType = rule.recurrenceEndType,
                endDate = rule.recurrenceEndDate,
                totalOccurrences = rule.recurrenceTotalOccurrences,
                status = rule.recurrenceStatus,
                since = since,
                until = until
            )

            for (occ in occurrences) {
                if (occ.occurrenceIndex > 0) {
                    val syntheticId = com.sans.finance.core.util.RecurringOccurrenceCalculator.generateSyntheticId(
                        rule.id,
                        occ.occurrenceIndex
                    )
                    val status = if (occ.date <= now) "Paid" else "Pending"
                    projected.add(
                        rule.copy(
                            id = syntheticId,
                            date = occ.date,
                            status = status,
                            isRecurring = true,
                            isRecurringInstance = true,
                            parentRecurringId = rule.id,
                            recurringOccurrenceIndex = occ.occurrenceIndex
                        )
                    )
                }
            }
        }
        return projected
    }

    private fun com.sans.finance.data.local.entity.InstallmentPaymentRow.toDomain(): Expense {
        return Expense(
            id = -this.id,
            amount = this.amount,
            date = this.date,
            title = this.title,
            type = "EXPENSE",
            categoryId = this.categoryId,
            isInstallmentPayment = true,
            installmentMonth = this.monthNumber,
            installmentTotalMonths = this.totalMonths,
            status = this.status,
            details = this.details,
            accountId = this.accountId,
            currency = this.currency,
            tags = this.tagsList?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
                ?: emptyList(),
            categoryName = this.categoryName,
            categoryIcon = this.categoryIcon
        )
    }
}
