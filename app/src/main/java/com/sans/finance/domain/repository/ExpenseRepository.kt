package com.sans.finance.domain.repository

import com.sans.finance.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getAllExpenses(): Flow<List<Expense>>
    fun getExpensesBetween(since: Long, until: Long): Flow<List<Expense>>
    fun getRecurringExpenses(): Flow<List<Expense>>
    fun getFilteredExpenses(
        query: String? = null,
        categoryIds: List<Long> = emptyList(),
        accountIds: List<Long> = emptyList(),
        since: Long = 0L,
        until: Long = Long.MAX_VALUE,
        minAmount: Long? = null,
        maxAmount: Long? = null,
        tags: List<String> = emptyList(),
        types: List<String> = emptyList()
    ): Flow<List<Expense>>

    suspend fun getExpenseById(id: Long): Expense?
    suspend fun getTitleSuggestions(query: String): List<String>
    suspend fun getTopFrequentTitles(limit: Int): List<String>
    suspend fun getTopFrequentTitlesByDay(dayOfWeek: Int, limit: Int): List<String>
    suspend fun getDetailsSuggestions(query: String): List<String>
    suspend fun getPredictionForTitle(title: String): Expense?
    suspend fun findPotentialDuplicate(title: String, amount: Long, date: Long, accountId: Long): Expense?
    suspend fun insertExpense(expense: Expense): Long
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(expense: Expense)
    fun getTotalSpentSince(since: Long): Flow<Long?>
    fun getTotalSpentBetween(since: Long, until: Long): Flow<Long?>
    fun getAllTimeSpent(): Flow<Long?>
    fun getOldestExpenseDate(): Flow<Long?>

    fun getSpendingByCategoryBetween(
        since: Long,
        until: Long
    ): Flow<List<CategorySpent>>

    fun getBreakdownByCategoryBetween(
        since: Long,
        until: Long,
        type: String
    ): Flow<List<CategorySpent>>

    fun getTotalAmountByTypeBetween(
        since: Long,
        until: Long,
        type: String
    ): Flow<Long?>

    fun getDailySpendingBetween(
        since: Long,
        until: Long
    ): Flow<List<DaySpent>>

    fun getDailyBreakdownByCategoryBetween(
        since: Long,
        until: Long,
        categoryId: Long,
        type: String
    ): Flow<List<DaySpent>>

    fun getMonthlyBreakdownByCategory(
        categoryId: Long,
        type: String
    ): Flow<List<DaySpent>>

    fun getMonthlyBreakdownByCategoryBetween(
        since: Long,
        until: Long,
        categoryId: Long,
        type: String
    ): Flow<List<DaySpent>>

    suspend fun getReSyncBalancesDryRun(): List<AccountSyncDryRunResult>
    suspend fun reSyncAccountBalances(mode: ReSyncMode, adjustmentDate: Long)
}
