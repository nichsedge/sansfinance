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
import com.sans.finance.domain.model.Expense
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    private val accountRepository: com.sans.finance.domain.repository.AccountRepository,
    private val checkDuplicateExpenseUseCase: CheckDuplicateExpenseUseCase,
    private val predictTransactionUseCase: com.sans.finance.domain.usecase.PredictTransactionUseCase,
    private val getFrequencyBasedSuggestionsUseCase: com.sans.finance.domain.usecase.GetFrequencyBasedSuggestionsUseCase,
    private val localeManager: com.sans.finance.data.util.LocaleManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

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

    var transactionType by mutableStateOf("EXPENSE") // "EXPENSE", "INCOME", "TRANSFER"

    val categories = combine(allCategories, snapshotFlow { transactionType }) { cats, type ->
        cats.filter { it.type == type || type == "TRANSFER" }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    var amount by mutableStateOf("")
    var title by mutableStateOf("")
    var details by mutableStateOf("")
    var categoryId by mutableLongStateOf(1L)
    var accountId by mutableLongStateOf(1L)
    var toAccountId by mutableLongStateOf(2L)

    // transactionType moved up
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

    var validationMessage by mutableStateOf<String?>(null)
        private set

    var newTagText by mutableStateOf("")

    val allTags = expenseRepository.getAllTags().stateIn(
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
                    amount = kotlin.math.ceil(expense.amount / 100.0).toLong().toString()
                    title = expense.title
                    details = expense.details ?: ""
                    categoryId = expense.categoryId
                    accountId = expense.accountId
                    toAccountId = expense.toAccountId ?: 2L
                    transactionType = expense.type
                    if (expense.isInstallment) paymentType = "INSTALLMENT"
                    else if (expense.isRecurring) paymentType = "RECURRING"
                    else paymentType = "ONE_TIME"

                    recurrenceInterval = expense.recurrenceInterval ?: "MONTHLY"
                    selectedDate = expense.date
                    currency = expense.currency
                    isInstallmentPayment = expense.isInstallmentPayment
                    installmentMonth = expense.installmentMonth
                    installmentTotalMonths = expense.installmentTotalMonths
                    status = expense.status

                    if (expense.isRecurring) {
                        installmentRepository.getInstallmentByExpenseId(id)?.let { installment ->
                            durationMonths = installment.durationMonths.toString()
                        }
                    }
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
        snapshotFlow { transactionType }
            .distinctUntilChanged()
            .onEach { type ->
                // When type changes, try to pick the first category of that type
                val currentCats = allCategories.value
                val firstMatch = currentCats.firstOrNull { it.type == type }
                if (firstMatch != null) {
                    categoryId = firstMatch.id
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
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = selectedDate
            when (recurrenceInterval) {
                "DAILY" -> calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                "WEEKLY" -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1)
                "MONTHLY" -> calendar.add(java.util.Calendar.MONTH, 1)
                "YEARLY" -> calendar.add(java.util.Calendar.YEAR, 1)
            }
            calendar.timeInMillis
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

    private fun String.toSafeLongCents(): Long? {
        return try {
            val cleanStr = this.replace(",", ".")
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
