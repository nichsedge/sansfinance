package com.sans.finance.presentation.add_transaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.BudgetRepository
import com.sans.finance.domain.repository.CategoryRepository
import com.sans.finance.domain.repository.TagRepository
import com.sans.finance.domain.usecase.AddTransactionUseCase
import com.sans.finance.domain.usecase.CheckDuplicateExpenseUseCase
import com.sans.finance.domain.usecase.DeleteExpenseUseCase
import com.sans.finance.domain.usecase.GetDetailsSuggestionsUseCase
import com.sans.finance.domain.usecase.GetExpenseByIdUseCase
import com.sans.finance.domain.usecase.GetTitleSuggestionsUseCase
import com.sans.finance.domain.usecase.UpdateExpenseUseCase
import com.sans.finance.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class CategoryBudgetStatus(
    val hasBudget: Boolean = false,
    val budgetAmount: Long = 0L,
    val spentAmount: Long = 0L,
    val remainingAmount: Long = 0L,
    val isExceeded: Boolean = false,
    val willExceed: Boolean = false
)

data class FxConversionInfo(
    val isForeign: Boolean = false,
    val rateFormatted: String = "",
    val convertedAmountFormatted: String = ""
)

@OptIn(FlowPreview::class)
@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase,
    private val getCategoriesUseCase: com.sans.finance.domain.usecase.GetCategoriesUseCase,
    private val createInstallmentPlanUseCase: com.sans.finance.domain.usecase.CreateInstallmentPlanUseCase,
    private val getTitleSuggestionsUseCase: GetTitleSuggestionsUseCase,
    private val getDetailsSuggestionsUseCase: GetDetailsSuggestionsUseCase,
    private val installmentRepository: com.sans.finance.domain.repository.InstallmentRepository,
    private val expenseRepository: com.sans.finance.domain.repository.ExpenseRepository,
    private val tagRepository: TagRepository,
    private val accountRepository: com.sans.finance.domain.repository.AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val currencyDao: CurrencyDao,
    private val checkDuplicateExpenseUseCase: CheckDuplicateExpenseUseCase,
    private val predictTransactionUseCase: com.sans.finance.domain.usecase.PredictTransactionUseCase,
    private val getFrequencyBasedSuggestionsUseCase: com.sans.finance.domain.usecase.GetFrequencyBasedSuggestionsUseCase,
    private val localeManager: com.sans.finance.data.util.LocaleManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val addTransactionRoute: Screen.AddTransaction? = try {
        savedStateHandle.toRoute<Screen.AddTransaction>()
    } catch (e: Exception) {
        null
    }

    private val editExpenseId: Long? = try {
        savedStateHandle.toRoute<Screen.EditExpense>().expenseId
    } catch (e: Exception) {
        null
    }

    val isEditMode get() = editExpenseId != null

    val allCategories = getCategoriesUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    var transactionType by mutableStateOf(
        addTransactionRoute?.transactionType?.ifBlank { "EXPENSE" } ?: "EXPENSE"
    )

    val categories = combine(allCategories, snapshotFlow { transactionType }) { cats, type ->
        cats.filter { it.type == type || type == "TRANSFER" }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    var amount by mutableStateOf(addTransactionRoute?.initialAmount ?: "")
    var title by mutableStateOf(addTransactionRoute?.initialTitle ?: "")
    var details by mutableStateOf(addTransactionRoute?.initialNotes ?: "")
    var categoryId by mutableLongStateOf(
        if (addTransactionRoute != null && addTransactionRoute.categoryId > 0) addTransactionRoute.categoryId else 1L
    )
    var accountId by mutableLongStateOf(
        if (addTransactionRoute != null && addTransactionRoute.initialAccountId > 0) addTransactionRoute.initialAccountId else 1L
    )
    var toAccountId by mutableLongStateOf(2L)

    var paymentType by mutableStateOf("ONE_TIME") // "ONE_TIME", "RECURRING", "INSTALLMENT"
    var isInstallmentPayment by mutableStateOf(false)
        private set
    var installmentMonth by mutableIntStateOf(0)
        private set
    var installmentTotalMonths by mutableIntStateOf(0)
        private set
    var status by mutableStateOf("Paid")
        private set
    val isInstallment get() = paymentType == "INSTALLMENT"
    val isRecurring get() = paymentType == "RECURRING"
    var recurrenceInterval by mutableStateOf("MONTHLY")
    var recurrenceEndType by mutableStateOf("NEVER") // "NEVER", "UNTIL_DATE", "AFTER_COUNT"
    var recurrenceEndDate by mutableStateOf<Long?>(null)
    var recurrenceTotalOccurrences by mutableStateOf("12")
    var recurrenceIntervalMultiplier by mutableIntStateOf(1)
    var recurrenceStatus by mutableStateOf("ACTIVE") // "ACTIVE", "PAUSED"
    var durationMonths by mutableStateOf("")
    var selectedDate by mutableLongStateOf(System.currentTimeMillis())
    var selectedTags by mutableStateOf(listOf<String>())
    var currency by mutableStateOf(localeManager.getCurrency())
    val enabledCurrencies = localeManager.getEnabledCurrencies()

    var titleSuggestions by mutableStateOf(emptyList<String>())
        private set
    var duplicateFound by mutableStateOf<Expense?>(null)
    var showDuplicateDialog by mutableStateOf(false)
    var detailsSuggestions by mutableStateOf(emptyList<String>())
        private set

    val baseCurrency = localeManager.getCurrency()

    val fxConversionInfo = combine(
        currencyDao.getAllRates(),
        snapshotFlow { currency },
        snapshotFlow { amount }
    ) { rates, curr, amtStr ->
        if (curr == baseCurrency || amtStr.isBlank()) {
            FxConversionInfo(isForeign = false)
        } else {
            val ratesMap = rates.associate { it.code to it.rateToIdr }
            val rateToIdr = if (curr == "IDR") 1.0 else (ratesMap[curr] ?: 1.0)
            val baseRateToIdr = if (baseCurrency == "IDR") 1.0 else (ratesMap[baseCurrency] ?: 1.0)
            val multiplier = rateToIdr / baseRateToIdr

            val parsedAmount = amtStr.replace(",", "").toDoubleOrNull() ?: 0.0
            val convertedCents = (parsedAmount * 100 * multiplier).toLong()
            val convertedFormatted = CurrencyFormatter.formatAmount(convertedCents, baseCurrency)
            val rateFormatted = if (multiplier >= 100) {
                String.format(java.util.Locale.US, "%,.0f", multiplier)
            } else {
                String.format(java.util.Locale.US, "%,.2f", multiplier)
            }
            FxConversionInfo(
                isForeign = true,
                rateFormatted = "1 $curr ≈ $rateFormatted $baseCurrency",
                convertedAmountFormatted = "≈ $convertedFormatted"
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FxConversionInfo()
    )

    val categoryBudgetStatus = combine(
        budgetRepository.getAllBudgets(),
        expenseRepository.getAllExpenses(),
        snapshotFlow { categoryId },
        snapshotFlow { amount },
        snapshotFlow { selectedDate }
    ) { budgets, allExpenses, catId, amtStr, dateMillis ->
        val cal = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val endOfMonth = cal.timeInMillis

        val matchingBudget = budgets.find { it.categoryId == catId }
        if (matchingBudget == null) {
            CategoryBudgetStatus(hasBudget = false)
        } else {
            val spentInMonth = allExpenses
                .filter { it.categoryId == catId && it.type == "EXPENSE" && !it.isInstallment && it.date in startOfMonth until endOfMonth }
                .sumOf { it.amount }
            val budgetCents = matchingBudget.amount
            val remainingCents = budgetCents - spentInMonth
            val parsedAmt = (amtStr.replace(",", "").toDoubleOrNull() ?: 0.0) * 100
            val willExceed = (spentInMonth + parsedAmt.toLong()) > budgetCents

            CategoryBudgetStatus(
                hasBudget = true,
                budgetAmount = budgetCents,
                spentAmount = spentInMonth,
                remainingAmount = remainingCents,
                isExceeded = remainingCents < 0,
                willExceed = willExceed && !isEditMode
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoryBudgetStatus()
    )

    var validationMessage by mutableStateOf<String?>(null)
        private set

    var newTagText by mutableStateOf("")

    val allTags = tagRepository.getVisibleTags().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val accounts = accountRepository.getAllAccounts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            accounts
                .filter { it.isNotEmpty() }
                .collect { list ->
                    if (isEditMode) return@collect

                    val primary = list.firstOrNull() ?: return@collect

                    if (accountId == 1L && list.none { it.id == accountId }) {
                        accountId = primary.id
                    }
                    if (currency.isBlank()) {
                        currency = primary.currency
                    }
                    if (toAccountId == 2L && list.size >= 2) {
                        toAccountId = list[1].id
                    }
                }
        }

        editExpenseId?.let { id ->
            viewModelScope.launch {
                getExpenseByIdUseCase(id)?.let { expense ->
                    amount = if (expense.amount % 100 == 0L) {
                        (expense.amount / 100).toString()
                    } else {
                        String.format(java.util.Locale.US, "%.2f", expense.amount / 100.0).trimEnd('0').trimEnd('.')
                    }
                    title = expense.title
                    details = expense.details ?: ""
                    categoryId = expense.categoryId
                    accountId = expense.accountId
                    toAccountId = expense.toAccountId ?: 2L
                    transactionType = expense.type
                    if (expense.isInstallment || expense.isInstallmentPayment) {
                        paymentType = "INSTALLMENT"
                        val allInstallments = installmentRepository
                            .getAllInstallments().firstOrNull() ?: emptyList()
                        val matchedInstallment = allInstallments.firstOrNull {
                            it.expenseId == expense.id || it.expenseName == expense.title
                        } ?: installmentRepository.getInstallmentByExpenseId(id)

                        durationMonths = matchedInstallment?.durationMonths?.toString()
                            ?: if (expense.installmentTotalMonths > 0) {
                                expense.installmentTotalMonths.toString()
                            } else {
                                ""
                            }
                    } else if (expense.isRecurring) {
                        paymentType = "RECURRING"
                    } else {
                        paymentType = "ONE_TIME"
                    }

                    recurrenceInterval = expense.recurrenceInterval ?: "MONTHLY"
                    recurrenceEndType = expense.recurrenceEndType ?: "NEVER"
                    recurrenceEndDate = expense.recurrenceEndDate
                    recurrenceTotalOccurrences = expense.recurrenceTotalOccurrences?.toString() ?: "12"
                    recurrenceIntervalMultiplier = expense.recurrenceIntervalMultiplier.coerceAtLeast(1)
                    recurrenceStatus = expense.recurrenceStatus
                    selectedDate = expense.date
                    currency = expense.currency
                    isInstallmentPayment = expense.isInstallmentPayment
                    installmentMonth = expense.installmentMonth
                    installmentTotalMonths = expense.installmentTotalMonths
                    status = expense.status
                    selectedTags = expense.tags
                }
            }
        }

        snapshotFlow { title }
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.length >= 2) {
                    titleSuggestions = getTitleSuggestionsUseCase(query)
                } else if (query.isEmpty()) {
                    titleSuggestions = getFrequencyBasedSuggestionsUseCase()
                } else {
                    titleSuggestions = emptyList()
                }
            }
            .launchIn(viewModelScope)

        snapshotFlow { details }
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.length >= 2) {
                    detailsSuggestions = getDetailsSuggestionsUseCase(query)
                } else {
                    detailsSuggestions = emptyList()
                }
            }
            .launchIn(viewModelScope)

        var previousType = transactionType
        snapshotFlow { transactionType }
            .distinctUntilChanged()
            .onEach { type ->
                if (previousType != type) {
                    previousType = type
                    val currentCats = allCategories.value
                    val currentCatMatches = currentCats.any { it.id == categoryId && (it.type == type || type == "TRANSFER") }
                    if (!currentCatMatches) {
                        val firstMatch = currentCats.firstOrNull { it.type == type }
                        if (firstMatch != null) {
                            categoryId = firstMatch.id
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun toggleTag(tagName: String) {
        selectedTags = if (selectedTags.contains(tagName)) {
            selectedTags.filter { it != tagName }
        } else {
            selectedTags + tagName
        }
    }

    fun addNewTag() {
        val tagToAdd = newTagText.trim()
        if (tagToAdd.isNotBlank() && !selectedTags.contains(tagToAdd)) {
            selectedTags = selectedTags + tagToAdd
            newTagText = ""
        }
    }

    fun onDeleteClick(deleteEntirePlan: Boolean, onSuccess: () -> Unit) {
        editExpenseId?.let { id ->
            viewModelScope.launch {
                getExpenseByIdUseCase(id)?.let { expense ->
                    deleteExpenseUseCase(expense, deleteEntirePlan)
                    onSuccess()
                }
            }
        }
    }

    fun onStatusChange(newStatus: String) {
        status = newStatus
    }

    fun applyPrediction(title: String) {
        viewModelScope.launch {
            predictTransactionUseCase(title)?.let { prediction ->
                categoryId = prediction.categoryId
                accountId = prediction.accountId
                transactionType = prediction.type
                selectedTags = prediction.tags
            }
        }
    }

    fun onSaveClick(onSuccess: () -> Unit) {
        val amountInCents = amount.toSafeLongCents() ?: 0L
        val effectiveTitle = title.trim().ifBlank { buildDefaultTitle() }

        if (amountInCents <= 0L) {
            validationMessage = "Amount must be greater than 0"
            return
        }
        if (transactionType == "TRANSFER" && accountId == toAccountId) {
            validationMessage = "Transfer account cannot be the same"
            return
        }
        title = effectiveTitle

        if (!isEditMode && !showDuplicateDialog) {
            viewModelScope.launch {
                val duplicate = checkDuplicateExpenseUseCase(
                    title = title,
                    amount = amountInCents,
                    date = selectedDate,
                    accountId = accountId
                )
                if (duplicate != null) {
                    duplicateFound = duplicate
                    showDuplicateDialog = true
                    return@launch
                }
                saveTransaction(onSuccess)
            }
        } else {
            saveTransaction(onSuccess)
        }
    }

    fun clearValidationMessage() {
        validationMessage = null
    }

    private fun saveTransaction(onSuccess: () -> Unit) {
        val amountInCents = amount.toSafeLongCents() ?: 0L
        val effectiveTitle = title.trim().ifBlank { buildDefaultTitle() }

        val nextDueDateVal = if (isRecurring) {
            com.sans.finance.core.util.RecurringOccurrenceCalculator.calculateNextDueDate(
                startDate = selectedDate,
                interval = recurrenceInterval,
                multiplier = recurrenceIntervalMultiplier,
                endType = recurrenceEndType,
                endDate = recurrenceEndDate,
                totalOccurrences = recurrenceTotalOccurrences.toIntOrNull(),
                status = recurrenceStatus,
                afterTime = selectedDate + 1000L
            )
        } else null

        viewModelScope.launch {
            val expense = Expense(
                id = editExpenseId ?: 0,
                date = selectedDate,
                title = effectiveTitle,
                amount = amountInCents,
                categoryId = categoryId,
                accountId = accountId,
                toAccountId = if (transactionType == "TRANSFER") toAccountId else null,
                type = transactionType,
                isInstallment = isInstallment,
                isRecurring = isRecurring,
                recurrenceInterval = if (isRecurring) recurrenceInterval else null,
                nextDueDate = nextDueDateVal,
                recurrenceEndType = if (isRecurring) recurrenceEndType else "NEVER",
                recurrenceEndDate = if (isRecurring && recurrenceEndType == "UNTIL_DATE") recurrenceEndDate else null,
                recurrenceTotalOccurrences = if (isRecurring && recurrenceEndType == "AFTER_COUNT") recurrenceTotalOccurrences.toIntOrNull() else null,
                recurrenceIntervalMultiplier = if (isRecurring) recurrenceIntervalMultiplier else 1,
                recurrenceStatus = if (isRecurring) recurrenceStatus else "ACTIVE",
                details = details.ifBlank { null },
                tags = selectedTags,
                currency = currency,
                isInstallmentPayment = isInstallmentPayment,
                installmentMonth = installmentMonth,
                installmentTotalMonths = installmentTotalMonths,
                status = status
            )

            if (editExpenseId == null) {
                val expenseId = addTransactionUseCase(expense)
                if (isInstallment && durationMonths.isNotBlank()) {
                    val duration = durationMonths.toIntOrNull() ?: 0
                    createInstallmentPlanUseCase(expenseId, amountInCents, duration, selectedDate)
                }
            } else {
                updateExpenseUseCase(expense, durationMonths.toIntOrNull())
            }
            onSuccess()
        }
    }

    private fun buildDefaultTitle(): String {
        if (transactionType == "TRANSFER") {
            val toName = accounts.value.firstOrNull { it.id == toAccountId }?.name?.takeIf { it.isNotBlank() }
            return if (toName != null) "Transfer → $toName" else "Transfer"
        }

        val categoryName = allCategories.value
            .firstOrNull { it.id == categoryId }
            ?.name
            ?.trim()
            .orEmpty()

        return categoryName.ifBlank {
            transactionType.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    fun evaluateAmountExpression() {
        if (com.sans.finance.core.util.MathExpressionEvaluator.hasArithmetic(amount)) {
            com.sans.finance.core.util.MathExpressionEvaluator.evaluate(amount)?.let { result ->
                amount = if (result % 1.0 == 0.0) {
                    result.toLong().toString()
                } else {
                    String.format(java.util.Locale.US, "%.2f", result).trimEnd('0').trimEnd('.')
                }
            }
        }
    }

    private fun String.toSafeLongCents(): Long? {
        val trimmed = this.trim().replace(" ", "").replace("\u00A0", "")
        if (com.sans.finance.core.util.MathExpressionEvaluator.hasArithmetic(trimmed)) {
            val evaluatedCents = com.sans.finance.core.util.MathExpressionEvaluator.evaluateToCents(trimmed)
            if (evaluatedCents != null) return evaluatedCents
        }

        return try {
            val cleanStr = trimmed.replace(",", ".")
            val pieces = cleanStr.split(".")
            val major = pieces[0].toLongOrNull() ?: 0L
            val minor = if (pieces.size > 1) {
                pieces[1].take(2).padEnd(2, '0').toLongOrNull() ?: 0L
            } else 0L
            (major * 100) + minor
        } catch (e: Exception) {
            null
        }
    }
}
