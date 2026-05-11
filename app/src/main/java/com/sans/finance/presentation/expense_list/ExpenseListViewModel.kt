package com.sans.finance.presentation.expense_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.BudgetRepository
import com.sans.finance.domain.usecase.GetExpensesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.core.util.DateFormatterUtils
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

enum class DateRangeFilter {
    SEVEN_DAYS,
    THIRTY_DAYS,
    THIS_MONTH,
    ALL_TIME,
    CUSTOM
}

data class ExpenseListState(
    val expenses: List<Expense> = emptyList(),
    val groupedExpenses: Map<Long, List<Expense>> = emptyMap(),
    val thisMonthSpent: Long = 0L,
    val totalFilteredAmount: Long = 0L,
    val totalFilteredIncome: Long = 0L,
    val totalFilteredExpense: Long = 0L,
    val startDate: Long = 0L,
    val endDate: Long = Long.MAX_VALUE,
    val activeDateFilter: DateRangeFilter = DateRangeFilter.THIS_MONTH,
    val isLoading: Boolean = true,
    val error: String? = null,
    val accounts: List<com.sans.finance.data.local.entity.AccountEntity> = emptyList(),
    val categories: List<com.sans.finance.data.local.entity.CategoryEntity> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val selectedTags: Set<String> = emptySet(),
    val searchQuery: String = "",
    val selectedCategoryIds: Set<Long> = emptySet(),
    val minAmount: Long? = null,
    val maxAmount: Long? = null,
    val dailySpending: Map<Long, Long> = emptyMap(),
    val monthlyBudget: Long = 0L
)

@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val repository: com.sans.finance.domain.repository.ExpenseRepository,
    private val accountRepository: com.sans.finance.domain.repository.AccountRepository,
    private val installmentRepository: com.sans.finance.domain.repository.InstallmentRepository,
    private val getCategoriesUseCase: com.sans.finance.domain.usecase.GetCategoriesUseCase,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ExpenseListState())
    val state: StateFlow<ExpenseListState> = _state.asStateFlow()

    private val dateFormat
        get() = DateFormatterUtils.getStandardFormatter()

    init {
        updateDateRange(DateRangeFilter.THIS_MONTH)

        loadExpenses()
        loadHistoricalStats()
        loadCategories()
        loadAccounts()
        loadTags()
        loadBudget()
    }

    private fun loadBudget() {
        budgetRepository.getAllBudgets()
            .map { budgets -> budgets.find { it.categoryId == null }?.amount ?: 0L }
            .onEach { budget ->
                _state.update { it.copy(monthlyBudget = budget) }
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


    private fun loadAccounts() {
        accountRepository.getAllAccounts()
            .onEach { accounts ->
                _state.update { it.copy(accounts = accounts) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadCategories() {
        getCategoriesUseCase()
            .onEach { categories ->
                _state.update { it.copy(categories = categories) }
            }
            .launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadExpenses() {
        _state
            .map {
                listOf(
                    it.startDate,
                    it.endDate,
                    it.searchQuery,
                    it.selectedCategoryIds,
                    it.minAmount,
                    it.maxAmount,
                    it.selectedTags
                )
            }
            .distinctUntilChanged()
            .flatMapLatest { _ ->
                val s = _state.value
                val expensesFlow = repository.getFilteredExpenses(
                    query = s.searchQuery,
                    categoryIds = s.selectedCategoryIds.toList(),
                    since = s.startDate,
                    until = s.endDate,
                    minAmount = s.minAmount,
                    maxAmount = s.maxAmount,
                    tags = s.selectedTags.toList()
                )

                val dailyFlow = repository.getDailySpendingBetween(s.startDate, s.endDate)

                expensesFlow.combine(dailyFlow) { e, d -> Pair(e, d) }
            }
            .onEach { (expenses, dailySpending) ->
                val dailyMap = dailySpending.associate { it.day to it.amount }
                val grouped = groupExpensesByDate(expenses, dailyMap)
                // Filtered item totals: normal items + installment payments (already in the list)
                // We only exclude 'parent' installment plans to avoid double counting with their sub-payments
                val validExpenses = expenses.filter { !it.isInstallment || it.isInstallmentPayment }
                val income = validExpenses.filter { it.type == "INCOME" }.sumOf { it.amount }
                val expense = validExpenses.filter { it.type != "INCOME" }.sumOf { it.amount }
                val periodTotal = income - expense
                _state.update {
                    it.copy(
                        expenses = expenses,
                        groupedExpenses = grouped,
                        totalFilteredAmount = periodTotal,
                        totalFilteredIncome = income,
                        totalFilteredExpense = expense,
                        dailySpending = dailyMap,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun toggleCategoryFilter(categoryId: Long) {
        _state.update { currentState ->
            val newSelectedCategoryIds =
                if (currentState.selectedCategoryIds.contains(categoryId)) {
                    currentState.selectedCategoryIds - categoryId
                } else {
                    currentState.selectedCategoryIds + categoryId
                }
            currentState.copy(selectedCategoryIds = newSelectedCategoryIds)
        }
    }

    fun updateAmountFilter(min: Long?, max: Long?) {
        _state.update { it.copy(minAmount = min, maxAmount = max) }
    }

    fun clearFilters() {
        _state.update { currentState ->
            val calendar = CalendarUtils.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val start = calendar.timeInMillis
            val endCal = calendar.clone() as Calendar
            endCal.add(Calendar.MONTH, 1)
            val end = endCal.timeInMillis

            currentState.copy(
                searchQuery = "",
                selectedCategoryIds = emptySet(),
                minAmount = null,
                maxAmount = null,
                selectedTags = emptySet(),
                startDate = start,
                endDate = end,
                activeDateFilter = DateRangeFilter.THIS_MONTH
            )
        }
    }

    fun toggleTagFilter(tag: String) {
        _state.update { currentState ->
            val newSelectedTags = if (currentState.selectedTags.contains(tag)) {
                currentState.selectedTags - tag
            } else {
                currentState.selectedTags + tag
            }
            currentState.copy(selectedTags = newSelectedTags)
        }
    }

    private fun groupExpensesByDate(
        expenses: List<Expense>,
        dailySpendingMap: Map<Long, Long> = emptyMap()
    ): Map<Long, List<Expense>> {
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

    fun updateCustomDateRange(start: Long, end: Long) {
        val utcCalendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))

        utcCalendar.timeInMillis = start
        val sYear = utcCalendar.get(Calendar.YEAR)
        val sMonth = utcCalendar.get(Calendar.MONTH)
        val sDay = utcCalendar.get(Calendar.DAY_OF_MONTH)

        val localCalendar = CalendarUtils.getInstance()
        localCalendar.set(sYear, sMonth, sDay, 0, 0, 0)
        localCalendar.set(Calendar.MILLISECOND, 0)
        val localStart = localCalendar.timeInMillis

        utcCalendar.timeInMillis = end
        val eYear = utcCalendar.get(Calendar.YEAR)
        val eMonth = utcCalendar.get(Calendar.MONTH)
        val eDay = utcCalendar.get(Calendar.DAY_OF_MONTH)

        localCalendar.set(eYear, eMonth, eDay, 0, 0, 0)
        localCalendar.set(Calendar.MILLISECOND, 0)
        localCalendar.add(Calendar.DAY_OF_MONTH, 1)
        val localEnd = localCalendar.timeInMillis

        _state.update {
            it.copy(
                startDate = localStart,
                endDate = localEnd,
                activeDateFilter = DateRangeFilter.CUSTOM,
                isLoading = true
            )
        }
    }

    fun updateDateRange(filter: DateRangeFilter) {
        val calendar = CalendarUtils.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)


        val (start, end) = when (filter) {
            DateRangeFilter.SEVEN_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val endCal = CalendarUtils.getInstance()
                endCal.add(Calendar.DAY_OF_YEAR, 1)
                endCal.set(Calendar.HOUR_OF_DAY, 0)
                endCal.set(Calendar.MINUTE, 0)
                endCal.set(Calendar.SECOND, 0)
                endCal.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, endCal.timeInMillis)
            }

            DateRangeFilter.THIRTY_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                val endCal = CalendarUtils.getInstance()
                endCal.add(Calendar.DAY_OF_YEAR, 1)
                endCal.set(Calendar.HOUR_OF_DAY, 0)
                endCal.set(Calendar.MINUTE, 0)
                endCal.set(Calendar.SECOND, 0)
                endCal.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, endCal.timeInMillis)
            }

            DateRangeFilter.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val endCal = calendar.clone() as Calendar
                endCal.add(Calendar.MONTH, 1)
                Pair(calendar.timeInMillis, endCal.timeInMillis)
            }

            DateRangeFilter.ALL_TIME -> {
                Pair(0L, Long.MAX_VALUE)
            }

            DateRangeFilter.CUSTOM -> {
                Pair(_state.value.startDate, _state.value.endDate)
            }
        }

        _state.update {
            it.copy(
                startDate = start,
                endDate = end,
                activeDateFilter = filter,
                isLoading = true
            )
        }
    }

    private fun loadHistoricalStats() {
        // This Month
        val calendar = CalendarUtils.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val endCal = calendar.clone() as Calendar
        endCal.add(Calendar.MONTH, 1)

        val monthExpensesFlow = repository.getTotalSpentBetween(calendar.timeInMillis, endCal.timeInMillis)
        val monthInstallmentsFlow =
            installmentRepository.getTotalPaidAmountBetween(calendar.timeInMillis, endCal.timeInMillis)

        monthExpensesFlow.combine(monthInstallmentsFlow) { exp, inst ->
            (exp ?: 0L) + (inst ?: 0L)
        }.onEach { total ->
            _state.update { it.copy(thisMonthSpent = total) }
        }.launchIn(viewModelScope)


    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }
}
