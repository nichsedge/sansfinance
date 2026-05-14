package com.sans.finance.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.local.entity.GoalEntity
import com.sans.finance.domain.repository.GoalRepository
import com.sans.finance.domain.repository.PortfolioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalWithProgress(
    val goal: GoalEntity,
    val currentAmount: Double
)

data class GoalState(
    val goals: List<GoalWithProgress> = emptyList(),
    val categories: List<String> = emptyList(),
    val assetClasses: List<String> = emptyList(),
    val currentCurrency: String = "USD"
)

@HiltViewModel
class GoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val portfolioRepository: PortfolioRepository,
    private val localeManager: com.sans.finance.data.util.LocaleManager
) : ViewModel() {

    val state = combine(
        goalRepository.getAllGoals(),
        portfolioRepository.getLatestSnapshot(),
        portfolioRepository.getLatestSnapshotHeader(),
        kotlinx.coroutines.flow.flowOf(localeManager.getCurrency())
    ) { goals, latestHoldings, latestHeader, currency ->
        val categories = latestHoldings.map { it.category }.distinct().sorted()
        val assetClasses = latestHoldings.map { it.assetClass }.distinct().sorted()
        val exchangeRate = latestHeader?.exchangeRateUsd ?: 16000.0

        val goalsWithProgress = goals.map { goal ->
            val currentAmountIdr = when (goal.targetType) {
                "TOTAL" -> latestHoldings.sumOf { it.valueIdr }
                "CATEGORY" -> latestHoldings.filter { it.category == goal.targetName }
                    .sumOf { it.valueIdr }

                "ASSET_CLASS" -> latestHoldings.filter { it.assetClass == goal.targetName }
                    .sumOf { it.valueIdr }

                else -> 0.0
            }

            val currentAmount = if (goal.currency == "USD") {
                currentAmountIdr / exchangeRate
            } else {
                currentAmountIdr
            }

            GoalWithProgress(goal, currentAmount)
        }
        GoalState(goalsWithProgress, categories, assetClasses, currency)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalState()
    )

    fun addGoal(
        name: String,
        targetAmount: Double,
        targetType: String = "TOTAL",
        targetName: String? = null,
        deadline: Long? = null
    ) {
        viewModelScope.launch {
            goalRepository.insertGoal(
                GoalEntity(
                    name = name,
                    targetAmount = targetAmount.toLong(),
                    targetType = targetType,
                    targetName = targetName,
                    currency = localeManager.getCurrency(),
                    deadline = deadline
                )
            )
        }
    }

    fun updateGoalDetails(
        goal: GoalEntity,
        newName: String,
        newTargetAmount: Double,
        newTargetType: String,
        newTargetName: String?,
        newDeadline: Long? = null
    ) {
        viewModelScope.launch {
            goalRepository.updateGoal(
                goal.copy(
                    name = newName,
                    targetAmount = newTargetAmount.toLong(),
                    targetType = newTargetType,
                    targetName = newTargetName,
                    deadline = newDeadline,
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
