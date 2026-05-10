@file:Suppress("unused")

package com.sans.finance.presentation.add_expense

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.usecase.AddExpenseUseCase
import androidx.compose.runtime.snapshotFlow
import com.sans.finance.domain.usecase.GetExpenseByIdUseCase
import com.sans.finance.domain.usecase.GetItemNameSuggestionsUseCase
import com.sans.finance.domain.usecase.GetMerchantSuggestionsUseCase
import com.sans.finance.domain.usecase.UpdateExpenseUseCase
import com.sans.finance.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.FlowPreview

@OptIn(FlowPreview::class)
@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val addExpenseUseCase: AddExpenseUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase,
    private val getCategoriesUseCase: com.sans.finance.domain.usecase.GetCategoriesUseCase,
    private val createInstallmentPlanUseCase: com.sans.finance.domain.usecase.CreateInstallmentPlanUseCase,
    private val getItemNameSuggestionsUseCase: GetItemNameSuggestionsUseCase,
    private val getMerchantSuggestionsUseCase: GetMerchantSuggestionsUseCase,
    private val installmentRepository: com.sans.finance.domain.repository.InstallmentRepository,
    private val expenseRepository: com.sans.finance.domain.repository.ExpenseRepository,
    private val accountRepository: com.sans.finance.domain.repository.AccountRepository,
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
    var itemName by mutableStateOf("")
    var merchant by mutableStateOf("")
    var categoryId by mutableLongStateOf(1L)
    var accountId by mutableLongStateOf(1L)
    // transactionType moved up
    var isInstallment by mutableStateOf(false)
    var durationMonths by mutableStateOf("")
    var selectedDate by mutableLongStateOf(System.currentTimeMillis())
    var selectedTags by mutableStateOf(listOf<String>())

    var itemNameSuggestions by mutableStateOf(emptyList<String>())
        private set
    var merchantSuggestions by mutableStateOf(emptyList<String>())
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
        editExpenseId?.let { id ->
            viewModelScope.launch {
                getExpenseByIdUseCase(id)?.let { expense ->
                    amount = kotlin.math.ceil(expense.amount / 100.0).toLong().toString()
                    itemName = expense.itemName
                    merchant = expense.merchant ?: ""
                    categoryId = expense.categoryId
                    accountId = expense.accountId
                    transactionType = expense.type
                    isInstallment = expense.isInstallment
                    selectedDate = expense.date
                    selectedTags = expense.tags

                    if (expense.isInstallment) {
                        installmentRepository.getInstallmentByExpenseId(id)?.let { installment ->
                            durationMonths = installment.durationMonths.toString()
                        }
                    }
                }
            }
        }

        snapshotFlow { itemName }
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.length >= 2) {
                    itemNameSuggestions = getItemNameSuggestionsUseCase(query)
                } else {
                    itemNameSuggestions = emptyList()
                }
            }
            .launchIn(viewModelScope)

        snapshotFlow { merchant }
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.length >= 2) {
                    merchantSuggestions = getMerchantSuggestionsUseCase(query)
                } else {
                    merchantSuggestions = emptyList()
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
        val tagToAdd = newTagText.trim().lowercase()
        if (tagToAdd.isNotBlank() && !selectedTags.contains(tagToAdd)) {
            selectedTags = selectedTags + tagToAdd
            newTagText = ""
        }
    }

    fun onSaveClick(onSuccess: () -> Unit) {
        val amountInCents = amount.toSafeLongCents() ?: 0L

        viewModelScope.launch {
            val expense = Expense(
                id = editExpenseId ?: 0,
                date = selectedDate,
                itemName = itemName.ifBlank { "Uncategorized Item" },
                amount = amountInCents,
                categoryId = categoryId,
                accountId = accountId,
                type = transactionType,
                isInstallment = isInstallment,
                merchant = merchant.ifBlank { null },
                tags = selectedTags,
                quantity = 1
            )

            if (editExpenseId == null) {
                val expenseId = addExpenseUseCase(expense)
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
