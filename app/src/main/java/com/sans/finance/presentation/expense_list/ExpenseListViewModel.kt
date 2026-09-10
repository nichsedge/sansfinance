package com.sans.finance.presentation.expense_list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.domain.model.Category
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.model.ExpenseFilter
import com.sans.finance.domain.repository.BudgetRepository
import com.sans.finance.domain.repository.TagRepository
import com.sans.finance.domain.repository.UserPreferencesRepository
import com.sans.finance.domain.usecase.ObserveFilteredExpensesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class DateRangeFilter {
    THIS_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    THIS_YEAR,
    ALL_TIME,
    CUSTOM
}

enum class TimelineCommitmentFilter {
    ALL,
    INSTALLMENTS,
    RECURRING
}

@Immutable
sealed class TimelineItem {
    abstract val key: String

    data class Header(
        val date: Long,
        val income: Long,
        val expense: Long,
        val formattedDate: String = ""
    ) : TimelineItem() {
        override val key: String = "header_$date"
    }

    data class ExpenseItem(val expense: Expense) : TimelineItem() {
        override val key: String = "exp_${expense.id}"
    }

    object TodaySeparator : TimelineItem() {
        override val key: String = "today_separator"
    }
}

@Immutable
data class ExpenseListState(
    val expenses: List<Expense> = emptyList(),
    val groupedExpenses: Map<Long, List<Expense>> = emptyMap(),
    val timelineItems: List<TimelineItem> = emptyList(),
    val thisMonthSpent: Long = 0L,
    val totalFilteredAmount: Long = 0L,
    val totalFilteredIncome: Long = 0L,
    val totalFilteredExpense: Long = 0L,
    val startDate: Long = 0L,
    val endDate: Long = Long.MAX_VALUE,
    val activeDateFilter: DateRangeFilter = DateRangeFilter.THIS_MONTH,
    val activeCommitmentFilter: TimelineCommitmentFilter = TimelineCommitmentFilter.ALL,
    val activeInstallmentCount: Int = 0,
    val recurringExpenseCount: Int = 0,
    val selectedInstallment: com.sans.finance.domain.model.Installment? = null,
    val selectedInstallmentItems: List<com.sans.finance.domain.model.InstallmentItem> = emptyList(),
    val selectedRecurringExpense: Expense? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val accounts: List<com.sans.finance.data.local.entity.AccountEntity> = emptyList(),
    val categories: List<Category> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val selectedTags: Set<String> = emptySet(),
    val searchQuery: String = "",
    val selectedCategoryIds: Set<Long> = emptySet(),
    val selectedAccountIds: Set<Long> = emptySet(),
    val minAmount: Long? = null,
    val maxAmount: Long? = null,
    val dailySpending: Map<Long, Long> = emptyMap(),
    val monthlyBudget: Long = 0L,
    val currentCurrency: String = "USD",
    val avgMonthlyExpense: Long = 0L,
    val isPrivacyModeEnabled: Boolean = false,
    val selectedTypes: Set<String> = emptySet()
)

@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val observeFilteredExpensesUseCase: ObserveFilteredExpensesUseCase,
    private val repository: com.sans.finance.domain.repository.ExpenseRepository,
    private val accountRepository: com.sans.finance.domain.repository.AccountRepository,
    private val installmentRepository: com.sans.finance.domain.repository.InstallmentRepository,
    private val tagRepository: TagRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val getCategoriesUseCase: com.sans.finance.domain.usecase.GetCategoriesUseCase,
    private val budgetRepository: BudgetRepository,
    private val localeManager: com.sans.finance.data.util.LocaleManager
) : ViewModel() {

    private val _state = MutableStateFlow(ExpenseListState())
    val state: StateFlow<ExpenseListState> = _state.asStateFlow()

    private var workerDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Default

    init {
        val initialCurrency = localeManager.getCurrency()
        val initialRange = calculateDateRange(DateRangeFilter.THIS_MONTH)

        _state.update {
            it.copy(
                currentCurrency = initialCurrency,
                startDate = initialRange.first,
                endDate = initialRange.second,
                activeDateFilter = DateRangeFilter.THIS_MONTH
            )
        }

        setupObservations()
    }

    private fun setupObservations() {
        observePrivacyMode()
        observeInitialData()
        observeHistoricalStats()
        observeExpenses()
    }

    private fun observePrivacyMode() {
        userPreferencesRepository.userPreferences
            .map { it.isPrivacyModeEnabled }
            .distinctUntilChanged()
            .onEach { enabled ->
                _state.update { it.copy(isPrivacyModeEnabled = enabled) } }
            .launchIn(viewModelScope)
    }

    private fun observeInitialData() {
        combine(
            getCategoriesUseCase(),
            accountRepository.getAllAccounts(),
            tagRepository.getAllTags(),
            budgetRepository.getAllBudgets().map { budgets ->
                budgets.find { it.categoryId == null }?.amount ?: 0L
            }
        ) { categories, accounts, tags, budget ->
            _state.update {
                it.copy(
                    categories = categories,
                    accounts = accounts,
                    availableTags = tags,
                    monthlyBudget = budget
                )
            }
        }.launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeExpenses() {
        val filterFlow = _state.map { state ->
            ExpenseFilter(
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
        }.distinctUntilChanged()

        val commitmentFlow = _state.map { it.activeCommitmentFilter }.distinctUntilChanged()

        combine(filterFlow, commitmentFlow) { filter, commitment -> Pair(filter, commitment) }
            .flatMapLatest { (filter, commitmentFilter) ->
                observeFilteredExpensesUseCase(filter).map { result ->
                    val expenses = result.expenses
                    val dailyMap = result.dailySpending.associate { it.day to it.amount }

                    val calendar = CalendarUtils.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val todayMillis = calendar.timeInMillis

                    var totalIncome = 0L
                    var totalExpense = 0L

                    val validExpenses = ArrayList<Expense>(expenses.size)
                    val displayedExpenses = ArrayList<Expense>(expenses.size)

                    for (exp in expenses) {
                        val isValid = !exp.isInstallment || exp.isInstallmentPayment
                        if (isValid) {
                            validExpenses.add(exp)
                            if (exp.type == "INCOME") totalIncome += exp.amount
                            else if (exp.type == "EXPENSE") totalExpense += exp.amount
                        }

                        val shouldDisplay = when (commitmentFilter) {
                            TimelineCommitmentFilter.ALL -> isValid
                            TimelineCommitmentFilter.INSTALLMENTS -> isValid && (exp.isInstallment || exp.isInstallmentPayment)
                            TimelineCommitmentFilter.RECURRING -> isValid && exp.isRecurring
                        }
                        if (shouldDisplay) {
                            displayedExpenses.add(exp)
                        }
                    }

                    val grouped = groupExpensesByDate(displayedExpenses)
                    val timelineItems = ArrayList<TimelineItem>(displayedExpenses.size + grouped.size + 1)

                    var hasShownTodaySeparator = false
                    val hasFutureTransactions = grouped.keys.any { it > todayMillis }

                    for ((date, dayExpenses) in grouped) {
                        if (!hasShownTodaySeparator && date <= todayMillis && hasFutureTransactions) {
                            timelineItems.add(TimelineItem.TodaySeparator)
                            hasShownTodaySeparator = true
                        }

                        var dayIncome = 0L
                        var dayExpense = 0L
                        for (exp in dayExpenses) {
                            val amount = if (exp.isInstallment && exp.monthlyPayment > 0) exp.monthlyPayment else exp.amount
                            if (exp.type == "INCOME") dayIncome += amount
                            else if (exp.type == "EXPENSE") dayExpense += amount
                        }

                        val headerCal = CalendarUtils.getInstance().apply { timeInMillis = date }
                        val day = headerCal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                        val dayOfWeek = com.sans.finance.core.util.DateFormatterUtils.formatDayOfWeek(headerCal.time)
                        val formattedDate = "$day $dayOfWeek"

                        timelineItems.add(TimelineItem.Header(date, dayIncome, dayExpense, formattedDate))
                        for (exp in dayExpenses) {
                            timelineItems.add(TimelineItem.ExpenseItem(exp))
                        }
                    }

                    val periodTotal = totalIncome - totalExpense

                    var avgMonthlyExpense = 0L
                    if (_state.value.activeDateFilter == DateRangeFilter.ALL_TIME && validExpenses.isNotEmpty()) {
                        val firstDate = validExpenses.last().date // Sorted DESC, so last is oldest
                        val lastDate = System.currentTimeMillis()
                        val diff = lastDate - firstDate
                        val months = (diff / (1000L * 60 * 60 * 24 * 30)).coerceAtLeast(1L)
                        avgMonthlyExpense = totalExpense / months
                    }

                    val installmentCount = validExpenses.count { it.isInstallment || it.isInstallmentPayment }
                    val recurringCount = validExpenses.count { it.isRecurring }

                    ProcessedExpenses(
                        expenses = expenses,
                        grouped = grouped,
                        timelineItems = timelineItems,
                        periodTotal = periodTotal,
                        income = totalIncome,
                        expense = totalExpense,
                        dailyMap = dailyMap,
                        avgMonthlyExpense = avgMonthlyExpense,
                        installmentCount = installmentCount,
                        recurringCount = recurringCount
                    )
                }
            }
            .flowOn(workerDispatcher)
            .onEach { processed ->
                _state.update {
                    it.copy(
                        expenses = processed.expenses,
                        groupedExpenses = processed.grouped,
                        timelineItems = processed.timelineItems,
                        totalFilteredAmount = processed.periodTotal,
                        totalFilteredIncome = processed.income,
                        totalFilteredExpense = processed.expense,
                        dailySpending = processed.dailyMap,
                        avgMonthlyExpense = processed.avgMonthlyExpense,
                        activeInstallmentCount = processed.installmentCount,
                        recurringExpenseCount = processed.recurringCount,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private data class ProcessedExpenses(
        val expenses: List<Expense>,
        val grouped: Map<Long, List<Expense>>,
        val timelineItems: List<TimelineItem>,
        val periodTotal: Long,
        val income: Long,
        val expense: Long,
        val dailyMap: Map<Long, Long>,
        val avgMonthlyExpense: Long,
        val installmentCount: Int,
        val recurringCount: Int
    )

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

    fun toggleAccountFilter(accountId: Long) {
        _state.update { currentState ->
            val newSelectedAccountIds =
                if (currentState.selectedAccountIds.contains(accountId)) {
                    currentState.selectedAccountIds - accountId
                } else {
                    currentState.selectedAccountIds + accountId
                }
            currentState.copy(selectedAccountIds = newSelectedAccountIds)
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
                selectedAccountIds = emptySet(),
                minAmount = null,
                maxAmount = null,
                selectedTags = emptySet(),
                startDate = start,
                endDate = end,
                activeDateFilter = DateRangeFilter.THIS_MONTH,
                selectedTypes = emptySet()
            )
        }
    }

    fun toggleTypeFilter(type: String) {
        _state.update { currentState ->
            val newSelectedTypes = if (currentState.selectedTypes.contains(type)) {
                currentState.selectedTypes - type
            } else {
                currentState.selectedTypes + type
            }
            currentState.copy(selectedTypes = newSelectedTypes)
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

    private fun groupExpensesByDate(expenses: List<Expense>): Map<Long, List<Expense>> {
        if (expenses.isEmpty()) return emptyMap()
        val calendar = CalendarUtils.getInstance()
        val grouped = LinkedHashMap<Long, MutableList<Expense>>()

        for (expense in expenses) {
            calendar.timeInMillis = expense.date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val dayStart = calendar.timeInMillis
            grouped.getOrPut(dayStart) { ArrayList() }.add(expense)
        }
        return grouped
    }

    fun updateCustomDateRange(start: Long, end: Long) {
        _state.update {
            it.copy(
                startDate = start,
                endDate = end,
                activeDateFilter = DateRangeFilter.CUSTOM,
                isLoading = true
            )
        }
    }

    fun updateDateRange(filter: DateRangeFilter) {
        val (start, end) = calculateDateRange(filter)
        _state.update {
            it.copy(
                startDate = start,
                endDate = end,
                activeDateFilter = filter,
                isLoading = true
            )
        }
    }

    private fun calculateDateRange(filter: DateRangeFilter): Pair<Long, Long> {
        val calendar = CalendarUtils.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return when (filter) {
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

            DateRangeFilter.ALL_TIME -> {
                Pair(0L, Long.MAX_VALUE)
            }

            DateRangeFilter.CUSTOM -> {
                Pair(_state.value.startDate, _state.value.endDate)
            }
        }
    }

    private fun observeHistoricalStats() {
        val calendar = CalendarUtils.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val endCal = calendar.clone() as Calendar
        endCal.add(Calendar.MONTH, 1)

        val monthExpensesFlow =
            repository.getTotalSpentBetween(calendar.timeInMillis, endCal.timeInMillis)
        val monthInstallmentsFlow =
            installmentRepository.getTotalPaidAmountBetween(
                calendar.timeInMillis,
                endCal.timeInMillis
            )

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

    fun restoreExpense(expense: Expense) {
        viewModelScope.launch {
            repository.insertExpense(expense)
        }
    }

    fun addQuickExpense(
        title: String,
        amount: Long,
        categoryId: Long?,
        accountId: Long
    ) {
        viewModelScope.launch {
            val expense = Expense(
                date = System.currentTimeMillis(),
                title = title,
                amount = amount,
                categoryId = categoryId ?: 1L,
                accountId = accountId,
                type = "EXPENSE",
                currency = localeManager.getCurrency()
            )
            repository.insertExpense(expense)
        }
    }

    fun togglePrivacyMode() {
        viewModelScope.launch {
            userPreferencesRepository.setPrivacyModeEnabled(!_state.value.isPrivacyModeEnabled)
        }
    }

    fun previousMonth() {
        val calendar = CalendarUtils.getInstance()
        calendar.timeInMillis = if (_state.value.startDate == 0L) System.currentTimeMillis() else _state.value.startDate
        resetToFirstOfMonth(calendar)

        calendar.add(Calendar.MONTH, -1)
        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val end = calendar.timeInMillis

        updateCustomDateRange(start, end)
    }

    fun nextMonth() {
        val calendar = CalendarUtils.getInstance()
        calendar.timeInMillis = if (_state.value.startDate == 0L) System.currentTimeMillis() else _state.value.startDate
        resetToFirstOfMonth(calendar)

        calendar.add(Calendar.MONTH, 1)
        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val end = calendar.timeInMillis

        updateCustomDateRange(start, end)
    }

    fun jumpToDate(millis: Long) {
        val calendar = CalendarUtils.getInstance()
        calendar.timeInMillis = millis
        resetToFirstOfMonth(calendar)

        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val end = calendar.timeInMillis

        updateCustomDateRange(start, end)
    }

    private fun resetToStartOfDay(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    private fun resetToFirstOfMonth(calendar: Calendar) {
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        resetToStartOfDay(calendar)
    }

    fun setCommitmentFilter(filter: TimelineCommitmentFilter) {
        _state.update { it.copy(activeCommitmentFilter = filter) }
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

    fun deleteInstallmentPlan(installment: com.sans.finance.domain.model.Installment) {
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
