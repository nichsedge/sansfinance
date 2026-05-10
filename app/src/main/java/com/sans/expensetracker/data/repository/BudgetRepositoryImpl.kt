package com.sans.expensetracker.data.repository

import com.sans.expensetracker.data.local.dao.BudgetDao
import com.sans.expensetracker.data.local.entity.BudgetEntity
import com.sans.expensetracker.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {
    override fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()
    override suspend fun insertBudget(budget: BudgetEntity): Long = budgetDao.insertBudget(budget)
    override suspend fun updateBudget(budget: BudgetEntity) = budgetDao.updateBudget(budget)
    override suspend fun deleteBudget(budget: BudgetEntity) = budgetDao.deleteBudget(budget)
}
