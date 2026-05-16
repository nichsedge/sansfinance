package com.sans.finance.presentation.monthly_review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.data.ai.AiProviderFactory
import com.sans.finance.data.ai.MonthlyReviewInput
import com.sans.finance.data.ai.MonthlyReviewInsight
import com.sans.finance.data.ai.MonthlyReviewResult
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.usecase.GetCategoriesUseCase
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MonthlyReviewState(
    val monthLabel: String = "",
    val income: Long = 0L,
    val expense: Long = 0L,
    val savingsRate: Float = 0f,
    val headline: String = "",
    val insights: List<MonthlyReviewInsight> = emptyList(),
    val rawText: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val usedAi: Boolean = false
)

@HiltViewModel
class MonthlyReviewViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val localeManager: LocaleManager,
    private val aiProviderFactory: AiProviderFactory,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val monthOffset = runCatching { savedStateHandle.toRoute<Screen.MonthlyReview>().monthOffset }
        .getOrNull() ?: 0

    private val _state = MutableStateFlow(MonthlyReviewState(isLoading = true))
    val state: StateFlow<MonthlyReviewState> = _state.asStateFlow()

    init {
        refresh(dontCallAi = true)
    }

    fun refresh(dontCallAi: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val cal = CalendarUtils.getInstance().apply {
                    set(java.util.Calendar.DAY_OF_MONTH, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                    add(java.util.Calendar.MONTH, monthOffset)
                }
                val start = cal.timeInMillis
                cal.add(java.util.Calendar.MONTH, 1)
                val end = cal.timeInMillis

                val monthLabel = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date(start))

                val txns = expenseRepository.getExpensesBetween(start, end).first()
                    .filter { !it.isInstallment || it.isInstallmentPayment }

                val income = txns.filter { it.type == "INCOME" }.sumOf { it.amount }
                val expense = txns.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                val savingsRate = if (income > 0) ((income - expense).toFloat() / income.toFloat()) else 0f

                val categories = getCategoriesUseCase().first().associateBy { it.id }
                val topCats = txns.asSequence()
                    .filter { it.type == "EXPENSE" }
                    .groupBy { it.categoryId }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(5)
                    .map { (catId, amt) -> (categories[catId]?.name ?: "Unknown") to amt }

                val deterministic = buildDeterministicInsights(monthLabel, income, expense, savingsRate, topCats)
                _state.value = _state.value.copy(
                    monthLabel = monthLabel,
                    income = income,
                    expense = expense,
                    savingsRate = savingsRate,
                    headline = deterministic.headline,
                    insights = deterministic.insights,
                    rawText = null,
                    usedAi = false
                )

                if (dontCallAi) {
                    _state.value = _state.value.copy(isLoading = false)
                    return@launch
                }

                val provider = aiProviderFactory.create()
                if (provider == null) {
                    _state.value = _state.value.copy(isLoading = false)
                    return@launch
                }

                val aiResult = provider.generateMonthlyReview(
                    MonthlyReviewInput(
                        monthLabel = monthLabel,
                        baseCurrency = localeManager.getCurrency(),
                        income = income,
                        expense = expense,
                        topCategories = topCats,
                        notes = "Portfolio backfill cadence: monthly. Goal: improve savings and investing consistency."
                    )
                )

                _state.value = _state.value.copy(
                    headline = aiResult.headline.ifBlank { _state.value.headline },
                    insights = if (aiResult.insights.isNotEmpty()) aiResult.insights else _state.value.insights,
                    rawText = aiResult.rawText,
                    usedAi = true,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to generate review"
                )
            }
        }
    }

    private fun buildDeterministicInsights(
        monthLabel: String,
        income: Long,
        expense: Long,
        savingsRate: Float,
        topCats: List<Pair<String, Long>>
    ): MonthlyReviewResult {
        val insights = mutableListOf<MonthlyReviewInsight>()

        if (income <= 0L && expense > 0L) {
            insights += MonthlyReviewInsight(
                title = "No income recorded",
                why = "You had expenses in $monthLabel but no income transactions.",
                action = "Record salary/business income so the dashboard metrics become accurate.",
                severity = "WARN"
            )
        }

        if (income > 0L) {
            val srPct = (savingsRate * 100).toInt()
            val severity = when {
                savingsRate < 0f -> "CRITICAL"
                savingsRate < 0.1f -> "WARN"
                else -> "INFO"
            }
            insights += MonthlyReviewInsight(
                title = "Savings rate: $srPct%",
                why = "Savings rate is (income - expense) / income for $monthLabel.",
                action = "Aim for 20%+ by capping top categories and paying yourself first.",
                severity = severity
            )
        }

        if (topCats.isNotEmpty()) {
            val top = topCats.first()
            insights += MonthlyReviewInsight(
                title = "Biggest spend: ${top.first}",
                why = "${top.first} is your largest expense category this month.",
                action = "Set a weekly cap for ${top.first} and review transactions tagged to it.",
                severity = "INFO"
            )
        }

        val headline = when {
            income <= 0L && expense <= 0L -> "No activity recorded for $monthLabel"
            income <= 0L -> "You spent this month—log income for accurate progress"
            savingsRate < 0f -> "Overspent this month—stabilize cashflow first"
            savingsRate < 0.1f -> "Low savings—tighten your top categories"
            else -> "Good month—keep compounding"
        }

        return MonthlyReviewResult(headline = headline, insights = insights)
    }
}
