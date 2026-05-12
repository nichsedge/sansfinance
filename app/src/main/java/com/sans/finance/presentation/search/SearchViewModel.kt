package com.sans.finance.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.data.local.entity.CategoryEntity
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.usecase.GetCategoriesUseCase
import com.sans.finance.presentation.expense_list.DateRangeFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class SearchState(
    val expenses: List<Expense> = emptyList(),
    val groupedExpenses: Map<Long, List<Expense>> = emptyMap(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val selectedCategoryIds: Set<Long> = emptySet(),
    val selectedAccountIds: Set<Long> = emptySet(),
    val minAmount: Long? = null,
    val maxAmount: Long? = null,
    val selectedTags: Set<String> = emptySet(),
    val selectedTypes: Set<String> = emptySet(),
    val startDate: Long = 0L,
    val endDate: Long = Long.MAX_VALUE,
    val activeDateFilter: DateRangeFilter = DateRangeFilter.ALL_TIME,
    val currentCurrency: String = "IDR",
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val netAmount: Long = 0L,
    val availableTags: List<String> = emptyList(),
    val isPrivacyModeEnabled: Boolean = false,
    val categories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val accountRepository: AccountRepository,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val localeManager: LocaleManager
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    init {
        loadCategories()
        loadAccounts()
        loadTags()
        observePrivacyMode()
        observeFilters()
        
        _state.update { it.copy(currentCurrency = localeManager.getCurrency()) }
    }

    private fun loadCategories() {
        getCategoriesUseCase()
            .onEach { categories ->
                _state.update { it.copy(categories = categories) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadAccounts() {
        accountRepository.getAllAccounts()
            .onEach { accounts ->
                _state.update { it.copy(accounts = accounts) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadTags() {
        repository.getAllTags()
            .onEach { tags ->
                _state.update { it.copy(availableTags = tags) }
            }
            .launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeFilters() {
        _state
            .map {
                listOf(
                    it.searchQuery,
                    it.selectedCategoryIds,
                    it.selectedAccountIds,
                    it.minAmount,
                    it.maxAmount,
                    it.selectedTags,
                    it.selectedTypes,
                    it.startDate,
                    it.endDate
                )
            }
            .distinctUntilChanged()
            .flatMapLatest { _ ->
                val s = _state.value
                repository.getFilteredExpenses(
                    query = s.searchQuery,
                    categoryIds = s.selectedCategoryIds.toList(),
                    accountIds = s.selectedAccountIds.toList(),
                    since = s.startDate,
                    until = s.endDate,
                    minAmount = s.minAmount,
                    maxAmount = s.maxAmount,
                    tags = s.selectedTags.toList(),
                    types = s.selectedTypes.toList()
                )
            }
            .onEach { expenses ->
                val grouped = groupExpensesByDate(expenses)
                val income = expenses.filter { it.type == "INCOME" }.sumOf { it.amount }
                val expense = expenses.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                _state.update {
                    it.copy(
                        expenses = expenses,
                        groupedExpenses = grouped,
                        totalIncome = income,
                        totalExpense = expense,
                        netAmount = income - expense,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query, isLoading = true) }
    }

    fun toggleCategoryFilter(categoryId: Long) {
        _state.update { currentState ->
            val newSelected = if (currentState.selectedCategoryIds.contains(categoryId)) {
                currentState.selectedCategoryIds - categoryId
            } else {
                currentState.selectedCategoryIds + categoryId
            }
            currentState.copy(selectedCategoryIds = newSelected, isLoading = true)
        }
    }

    fun toggleAccountFilter(accountId: Long) {
        _state.update { currentState ->
            val newSelected = if (currentState.selectedAccountIds.contains(accountId)) {
                currentState.selectedAccountIds - accountId
            } else {
                currentState.selectedAccountIds + accountId
            }
            currentState.copy(selectedAccountIds = newSelected, isLoading = true)
        }
    }

    fun updateAmountFilter(min: Long?, max: Long?) {
        _state.update { it.copy(minAmount = min, maxAmount = max, isLoading = true) }
    }

    fun toggleTagFilter(tag: String) {
        _state.update { currentState ->
            val newSelected = if (currentState.selectedTags.contains(tag)) {
                currentState.selectedTags - tag
            } else {
                currentState.selectedTags + tag
            }
            currentState.copy(selectedTags = newSelected, isLoading = true)
        }
    }

    fun toggleTypeFilter(type: String) {
        _state.update { currentState ->
            val newSelected = if (currentState.selectedTypes.contains(type)) {
                currentState.selectedTypes - type
            } else {
                currentState.selectedTypes + type
            }
            currentState.copy(selectedTypes = newSelected, isLoading = true)
        }
    }

    fun updateDateRange(filter: DateRangeFilter, start: Long = 0L, end: Long = Long.MAX_VALUE) {
        val calendar = CalendarUtils.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val (s, e) = when (filter) {
            DateRangeFilter.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val endCal = calendar.clone() as Calendar
                endCal.add(Calendar.WEEK_OF_YEAR, 1)
                Pair(calendar.timeInMillis, endCal.timeInMillis)
            }
            DateRangeFilter.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val endCal = calendar.clone() as Calendar
                endCal.add(Calendar.MONTH, 1)
                Pair(calendar.timeInMillis, endCal.timeInMillis)
            }
            DateRangeFilter.LAST_MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val endCal = calendar.clone() as Calendar
                endCal.add(Calendar.MONTH, 1)
                Pair(calendar.timeInMillis, endCal.timeInMillis)
            }
            DateRangeFilter.THIS_YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                val endCal = calendar.clone() as Calendar
                endCal.add(Calendar.YEAR, 1)
                Pair(calendar.timeInMillis, endCal.timeInMillis)
            }
            DateRangeFilter.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
            DateRangeFilter.CUSTOM -> Pair(start, end)
        }

        _state.update {
            it.copy(
                startDate = s,
                endDate = e,
                activeDateFilter = filter,
                isLoading = true
            )
        }
    }

    fun clearFilters() {
        _state.update {
            it.copy(
                searchQuery = "",
                selectedCategoryIds = emptySet(),
                selectedAccountIds = emptySet(),
                minAmount = null,
                maxAmount = null,
                selectedTags = emptySet(),
                selectedTypes = emptySet(),
                startDate = 0L,
                endDate = Long.MAX_VALUE,
                activeDateFilter = DateRangeFilter.ALL_TIME,
                isLoading = true
            )
        }
    }

    private fun observePrivacyMode() {
        localeManager.privacyMode
            .onEach { isEnabled ->
                _state.update { it.copy(isPrivacyModeEnabled = isEnabled) }
            }
            .launchIn(viewModelScope)
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    private fun groupExpensesByDate(expenses: List<Expense>): Map<Long, List<Expense>> {
        val calendar = CalendarUtils.getInstance()
        return expenses.groupBy { expense ->
            calendar.timeInMillis = expense.date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }.toSortedMap(compareByDescending { it })
    }
}
