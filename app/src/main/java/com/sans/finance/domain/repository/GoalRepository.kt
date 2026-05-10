package com.sans.finance.domain.repository

import com.sans.finance.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getAllGoals(): Flow<List<GoalEntity>>
    suspend fun getGoalById(id: Long): GoalEntity?
    suspend fun insertGoal(goal: GoalEntity): Long
    suspend fun updateGoal(goal: GoalEntity)
    suspend fun deleteGoal(goal: GoalEntity)
}
