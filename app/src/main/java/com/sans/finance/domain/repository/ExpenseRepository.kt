package com.sans.finance.domain.repository

import com.sans.finance.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getAllExpenses(): Flow<List<Expense>>
    fun getExpensesBetween(since: Long, until: Long): Flow<List<Expense>>
    fun getRecurringExpenses(): Flow<List<Expense>>
    fun getFilteredExpenses(
        query: String? = null,
        categoryIds: List<Long> = emptyList(),
        since: Long = 0L,
        until: Long = Long.MAX_VALUE,
        minAmount: Long? = null,
        maxAmount: Long? = null,
        tags: List<String> = emptyList(),
        types: List<String> = emptyList()
    ): Flow<List<Expense>>

    suspend fun getExpenseById(id: Long): Expense?
    suspend fun getNoteSuggestions(query: String): List<String>
    suspend fun getDescriptionSuggestions(query: String): List<String>
    suspend fun insertExpense(expense: Expense): Long
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(expense: Expense)
    fun getTotalSpentSince(since: Long): Flow<Long?>
    fun getTotalSpentBetween(since: Long, until: Long): Flow<Long?>
    fun getAllTimeSpent(): Flow<Long?>
    fun getAllTags(): Flow<List<String>>

    // Category management
    fun getAllCategories(): Flow<List<com.sans.finance.data.local.entity.CategoryEntity>>
    fun getCategoriesByType(type: String): Flow<List<com.sans.finance.data.local.entity.CategoryEntity>>
    suspend fun insertCategory(category: com.sans.finance.data.local.entity.CategoryEntity)
    suspend fun updateCategory(category: com.sans.finance.data.local.entity.CategoryEntity)
    suspend fun updateCategories(categories: List<com.sans.finance.data.local.entity.CategoryEntity>)
    suspend fun deleteCategory(category: com.sans.finance.data.local.entity.CategoryEntity)

    // Tag management
    fun getAllTagEntities(): Flow<List<com.sans.finance.data.local.entity.TagEntity>>
    suspend fun updateTag(tag: com.sans.finance.data.local.entity.TagEntity)
    suspend fun updateTags(tags: List<com.sans.finance.data.local.entity.TagEntity>)
    suspend fun deleteTag(tag: com.sans.finance.data.local.entity.TagEntity)

    fun getSpendingByCategoryBetween(
        since: Long,
        until: Long
    ): Flow<List<com.sans.finance.data.local.entity.CategorySpent>>

    fun getBreakdownByCategoryBetween(
        since: Long,
        until: Long,
        type: String
    ): Flow<List<com.sans.finance.data.local.entity.CategorySpent>>

    fun getTotalAmountByTypeBetween(
        since: Long,
        until: Long,
        type: String
    ): Flow<Long?>

    fun getDailySpendingBetween(
        since: Long,
        until: Long
    ): Flow<List<com.sans.finance.data.local.entity.DaySpent>>

    fun getDailyBreakdownByCategoryBetween(
        since: Long,
        until: Long,
        categoryId: Long,
        type: String
    ): Flow<List<com.sans.finance.data.local.entity.DaySpent>>

    fun getMonthlyBreakdownByCategory(
        categoryId: Long,
        type: String
    ): Flow<List<com.sans.finance.data.local.entity.DaySpent>>
}
