package com.sans.finance.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.core.util.CurrencyConverter
import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.model.Category
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.model.Installment
import com.sans.finance.domain.model.InstallmentItem
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.InstallmentRepository
import com.sans.finance.domain.repository.TagRepository
import com.sans.finance.domain.repository.UserPreferencesRepository
import com.sans.finance.domain.usecase.GetCategoriesUseCase
import com.sans.finance.presentation.expense_list.DateRangeFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
    val categories: List<Category> = emptyList(),
    val accounts: List<com.sans.finance.data.local.entity.AccountEntity> = emptyList(),
    val selectedInstallment: Installment? = null,
    val selectedInstallmentItems: List<InstallmentItem> = emptyList(),
    val selectedRecurringExpense: Expense? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val accountRepository: AccountRepository,
    private val installmentRepository: InstallmentRepository,
    private val tagRepository: TagRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val currencyDao: CurrencyDao,
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
        tagRepository.getAllTags()
            .onEach { tags ->
                _state.update { it.copy(availableTags = tags) }
            }
            .launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeFilters() {
        _state
            .map { state ->
                com.sans.finance.domain.model.ExpenseFilter(
                    query = state.searchQuery,
                    categoryIds = state.selectedCategoryIds,
                    accountIds = state.selectedAccountIds,
                    since = state.startDate,
                    until = state.endDate,
                    minAmount = state.minAmount,
                    maxAmount = state.maxAmount,
                    tags = state.selectedTags,
                    types = state.selectedTypes
                )
            }
            .distinctUntilChanged()
            .flatMapLatest { filter ->
                kotlinx.coroutines.flow.combine(
                    repository.getFilteredExpenses(
                        query = filter.query,
                        categoryIds = filter.categoryIds.toList(),
                        accountIds = filter.accountIds.toList(),
                        since = filter.since,
                        until = filter.until,
                        minAmount = filter.minAmount,
                        maxAmount = filter.maxAmount,
                        tags = filter.tags.toList(),
                        types = filter.types.toList()
                    ),
                    currencyDao.getAllRates()
                ) { expenses, rates -> Pair(expenses, rates) }
            }
            .onEach { (expenses, rates) ->
                val baseCurrency = localeManager.getCurrency()
                val ratesMap = rates.associate { it.code to it.rateToIdr }
                val validExpenses = expenses.filter { !it.isInstallment || it.isInstallmentPayment }
                val grouped = groupExpensesByDate(validExpenses)
                val income = validExpenses
                    .filter { it.type == "INCOME" }
                    .sumOf { CurrencyConverter.convert(it.amount, it.currency, baseCurrency, ratesMap) }
                val expense = validExpenses
                    .filter { it.type == "EXPENSE" }
                    .sumOf { CurrencyConverter.convert(it.amount, it.currency, baseCurrency, ratesMap) }
                _state.update {
                    it.copy(
                        expenses = validExpenses,
                        groupedExpenses = grouped,
                        totalIncome = income,
                        totalExpense = expense,
                        netAmount = income - expense,
                        currentCurrency = baseCurrency,
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
        userPreferencesRepository.userPreferences
            .map { it.isPrivacyModeEnabled }
            .distinctUntilChanged()
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

    fun openInstallmentDetail(expense: Expense) {
        viewModelScope.launch {
            val all = installmentRepository.getAllInstallments().first()
            val installment = all.firstOrNull { it.expenseId == expense.id || it.expenseName == expense.title }
                ?: (if (expense.isInstallmentPayment) all.firstOrNull { it.expenseName == expense.title } else null)
                ?: installmentRepository.getInstallmentByExpenseId(expense.id)

            if (installment != null) {
                val items = installmentRepository.getInstallmentItems(installment.id).first()
                _state.update {
                    it.copy(
                        selectedInstallment = installment,
                        selectedInstallmentItems = items
                    )
                }
            }
        }
    }

    fun closeInstallmentDetail() {
        _state.update { it.copy(selectedInstallment = null, selectedInstallmentItems = emptyList()) }
    }

    fun openRecurringDetail(expense: Expense) {
        _state.update { it.copy(selectedRecurringExpense = expense) }
    }

    fun closeRecurringDetail() {
        _state.update { it.copy(selectedRecurringExpense = null) }
    }

    fun toggleInstallmentItemStatus(itemId: Long, currentStatus: String) {
        viewModelScope.launch {
            val nextStatus = if (currentStatus == "Paid") "Pending" else "Paid"
            installmentRepository.updateInstallmentItemStatus(itemId, nextStatus)
            _state.value.selectedInstallment?.let { inst ->
                val updatedItems = installmentRepository.getInstallmentItems(inst.id).first()
                _state.update { it.copy(selectedInstallmentItems = updatedItems) }
            }
        }
    }

    fun deleteInstallmentPlan(installment: Installment) {
        viewModelScope.launch {
            repository.getExpenseById(installment.expenseId)?.let { expense ->
                repository.deleteExpense(expense)
            }
            closeInstallmentDetail()
        }
    }

    fun deleteRecurringExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            closeRecurringDetail()
        }
    }

    fun togglePauseRecurring(expense: Expense) {
        viewModelScope.launch {
            val nextStatus = if (expense.recurrenceStatus.equals("PAUSED", ignoreCase = true)) "ACTIVE" else "PAUSED"
            val targetExpense = if (expense.isRecurringInstance && expense.parentRecurringId != null) {
                repository.getExpenseById(expense.parentRecurringId) ?: expense
            } else {
                expense
            }
            repository.updateExpense(targetExpense.copy(recurrenceStatus = nextStatus))
            closeRecurringDetail()
        }
    }
}
