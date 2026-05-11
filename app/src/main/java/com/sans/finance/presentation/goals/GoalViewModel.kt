package com.sans.finance.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.local.entity.GoalEntity
import com.sans.finance.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class GoalState(
    val goals: List<GoalEntity> = emptyList(),
    val currentCurrency: String = "USD"
)

@HiltViewModel
class GoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val localeManager: com.sans.finance.data.util.LocaleManager
) : ViewModel() {

    val state = combine(
        goalRepository.getAllGoals(),
        kotlinx.coroutines.flow.flowOf(localeManager.getCurrency()) // This is a bit hacky since it doesn't observe changes, but matches other viewmodels
    ) { goals, currency ->
        GoalState(goals, currency)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalState()
    )

    fun addGoal(name: String, targetAmount: Long, deadline: Long? = null) {
        viewModelScope.launch {
            goalRepository.insertGoal(
                GoalEntity(
                    name = name,
                    targetAmount = targetAmount,
                    currency = localeManager.getCurrency(),
                    deadline = deadline
                )
            )
        }
    }

    fun updateProgress(goal: GoalEntity, amount: Long) {
        viewModelScope.launch {
            goalRepository.updateGoal(
                goal.copy(
                    currentAmount = goal.currentAmount + amount,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateGoalDetails(goal: GoalEntity, newName: String, newTargetAmount: Long) {
        viewModelScope.launch {
            goalRepository.updateGoal(
                goal.copy(
                    name = newName,
                    targetAmount = newTargetAmount,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            goalRepository.deleteGoal(goal)
        }
    }
}
