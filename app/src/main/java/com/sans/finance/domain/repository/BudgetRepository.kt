package com.sans.finance.domain.repository

import com.sans.finance.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getAllBudgets(): Flow<List<BudgetEntity>>
    suspend fun insertBudget(budget: BudgetEntity): Long
    suspend fun updateBudget(budget: BudgetEntity)
    suspend fun deleteBudget(budget: BudgetEntity)
}
