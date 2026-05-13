package com.sans.finance.server.repository

import com.sans.finance.domain.model.*
import com.sans.finance.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class InMemoryExpenseRepository : ExpenseRepository {
    private val expenses = MutableStateFlow<List<Expense>>(emptyList())

    override fun getAllExpenses(): Flow<List<Expense>> = expenses

    override fun getExpensesBetween(since: Long, until: Long): Flow<List<Expense>> {
        return expenses.map { list ->
            list.filter { it.date in since..until }
        }
    }

    override fun getRecurringExpenses(): Flow<List<Expense>> {
        return expenses.map { list -> list.filter { it.isRecurring } }
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
        return expenses.map { list ->
            list.filter { it.date in since..until }
        }
    }

    override suspend fun getExpenseById(id: Long): Expense? {
        return expenses.value.find { it.id == id }
    }

    override suspend fun getNoteSuggestions(query: String): List<String> = emptyList()
    override suspend fun getTopFrequentNotes(limit: Int): List<String> = emptyList()
    override suspend fun getTopFrequentNotesByDay(dayOfWeek: Int, limit: Int): List<String> = emptyList()
    override suspend fun getDescriptionSuggestions(query: String): List<String> = emptyList()
    override suspend fun getPredictionForNote(note: String): Expense? = null
    override suspend fun findPotentialDuplicate(note: String, amount: Long, date: Long, accountId: Long): Expense? = null

    override suspend fun insertExpense(expense: Expense): Long {
        val newId = (expenses.value.maxOfOrNull { it.id } ?: 0L) + 1
        expenses.value = expenses.value + expense.copy(id = newId)
        return newId
    }

    override suspend fun updateExpense(expense: Expense) {
        expenses.value = expenses.value.map { if (it.id == expense.id) expense else it }
    }

    override suspend fun deleteExpense(expense: Expense) {
        expenses.value = expenses.value.filter { it.id != expense.id }
    }

    override fun getTotalSpentSince(since: Long): Flow<Long?> {
        return expenses.map { list -> list.filter { it.date >= since }.sumOf { it.amount } }
    }

    override fun getTotalSpentBetween(since: Long, until: Long): Flow<Long?> {
        return expenses.map { list -> list.filter { it.date in since..until }.sumOf { it.amount } }
    }

    override fun getAllTimeSpent(): Flow<Long?> {
        return expenses.map { list -> list.sumOf { it.amount } }
    }

    override fun getAllTags(): Flow<List<String>> = MutableStateFlow(emptyList())
    override fun getAllCategories(): Flow<List<Category>> = MutableStateFlow(emptyList())
    override fun getCategoriesByType(type: String): Flow<List<Category>> = MutableStateFlow(emptyList())
    override suspend fun insertCategory(category: Category) {}
    override suspend fun updateCategory(category: Category) {}
    override suspend fun updateCategories(categories: List<Category>) {}
    override suspend fun deleteCategory(category: Category) {}
    override fun getAllTagEntities(): Flow<List<Tag>> = MutableStateFlow(emptyList())
    override suspend fun updateTag(tag: Tag) {}
    override suspend fun updateTags(tags: List<Tag>) {}
    override suspend fun deleteTag(tag: Tag) {}
    override suspend fun performDatabaseMaintenance() {}
    override fun getSpendingByCategoryBetween(since: Long, until: Long): Flow<List<CategorySpent>> = MutableStateFlow(emptyList())
    override fun getBreakdownByCategoryBetween(since: Long, until: Long, type: String): Flow<List<CategorySpent>> = MutableStateFlow(emptyList())
    override fun getTotalAmountByTypeBetween(since: Long, until: Long, type: String): Flow<Long?> = MutableStateFlow(0L)
    override fun getDailySpendingBetween(since: Long, until: Long): Flow<List<DaySpent>> = MutableStateFlow(emptyList())
    override fun getDailyBreakdownByCategoryBetween(since: Long, until: Long, categoryId: Long, type: String): Flow<List<DaySpent>> = MutableStateFlow(emptyList())
    override fun getMonthlyBreakdownByCategory(categoryId: Long, type: String): Flow<List<DaySpent>> = MutableStateFlow(emptyList())
}
