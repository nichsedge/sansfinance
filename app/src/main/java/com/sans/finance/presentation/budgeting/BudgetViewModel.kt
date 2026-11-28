package com.sans.finance.presentation.budgeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.core.util.CurrencyConverter
import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.data.local.entity.BudgetEntity
import com.sans.finance.domain.model.Category
import com.sans.finance.domain.repository.BudgetRepository
import com.sans.finance.domain.repository.CategoryRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.UserPreferencesRepository
import com.sans.finance.domain.usecase.BudgetSuggestion
import com.sans.finance.domain.usecase.SuggestBudgetsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class BudgetStatus(
    val budget: BudgetEntity,
    val spent: Long,
    val categoryName: String? = null
)

data class BudgetState(
    val budgetStatuses: List<BudgetStatus> = emptyList(),
    val categories: List<Category> = emptyList(),
    val suggestions: List<BudgetSuggestion> = emptyList(),
    val safeToSpend: com.sans.finance.domain.model.DailySafeToSpend? = null,
    val currentCurrency: String = "USD",
    val isLoading: Boolean = true,
    val isPrivacyModeEnabled: Boolean = false
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository,
    private val suggestBudgetsUseCase: SuggestBudgetsUseCase,
    private val getCashFlowPacingUseCase: com.sans.finance.domain.usecase.GetCashFlowPacingUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val currencyDao: CurrencyDao,
    private val localeManager: com.sans.finance.data.util.LocaleManager
) : ViewModel() {

    private val _categories = categoryRepository.getAllCategories().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _suggestions = MutableStateFlow<List<BudgetSuggestion>>(emptyList())

    init {
        viewModelScope.launch {
            _suggestions.value = suggestBudgetsUseCase()
        }
    }

    val state = combine(
        budgetRepository.getAllBudgets(),
        _categories,
        _suggestions,
        getCashFlowPacingUseCase(),
        combine(
            userPreferencesRepository.userPreferences.map { it.isPrivacyModeEnabled },
            currencyDao.getAllRates()
        ) { privacyMode, rates -> Pair(privacyMode, rates) }
    ) { budgets, categories, suggestions, pacing, (privacyMode, rates) ->
        val baseCurrency = localeManager.getCurrency()
        val ratesMap = rates.associate { it.code to it.rateToIdr }
        val calendar = CalendarUtils.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val end = calendar.timeInMillis

        val statuses = budgets.map { budget ->
            val spentFlow = if (budget.categoryId != null) {
                expenseRepository.getSpendingByCategoryBetween(start, end)
                    .map { categorySpents ->
                        categorySpents.find { it.categoryId == budget.categoryId }?.totalAmount
                            ?: 0L
                    }
            } else {
                expenseRepository.getTotalSpentBetween(start, end).map { it ?: 0L }
            }

            val spentIdr = spentFlow.first()
            val spent = CurrencyConverter.convertFromIdr(spentIdr, baseCurrency, ratesMap)

            BudgetStatus(
                budget = budget,
                spent = spent,
                categoryName = categories.find { it.id == budget.categoryId }?.name
            )
        }

        BudgetState(
            budgetStatuses = statuses,
            categories = categories,
            suggestions = suggestions,
            safeToSpend = pacing,
            currentCurrency = baseCurrency,
            isLoading = false,
            isPrivacyModeEnabled = privacyMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetState()
    )

    fun addBudget(amount: Long, categoryId: Long? = null, accountId: Long? = null) {
        viewModelScope.launch {
            budgetRepository.insertBudget(
                BudgetEntity(
                    amount = amount,
                    categoryId = categoryId,
                    accountId = accountId
                )
            )
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(budget)
        }
    }

    fun togglePrivacyMode() {
        viewModelScope.launch {
            userPreferencesRepository.setPrivacyModeEnabled(!state.value.isPrivacyModeEnabled)
        }
    }
}
