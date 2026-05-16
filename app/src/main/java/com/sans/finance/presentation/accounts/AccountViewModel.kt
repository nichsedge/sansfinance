package com.sans.finance.presentation.accounts


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountScreenState(
    val assets: Long = 0L,
    val liabilities: Long = 0L,
    val total: Long = 0L,
    val accountsByType: Map<String, List<AccountEntity>> = emptyMap(),
    val accountTypes: List<com.sans.finance.data.local.entity.AccountTypeEntity> = emptyList(),
    val currentCurrency: String = "USD",
    val isLoading: Boolean = true,
    val isPrivacyModeEnabled: Boolean = false
)


@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val expenseRepository: ExpenseRepository,
    private val accountTypeRepository: com.sans.finance.domain.repository.AccountTypeRepository,
    private val localeManager: com.sans.finance.data.util.LocaleManager
) : ViewModel() {

    val state = combine(
        accountRepository.getAllAccounts(),
        expenseRepository.getExpensesBetween(0, Long.MAX_VALUE),
        accountTypeRepository.getAllAccountTypes(),
        localeManager.privacyMode
    ) { accountsList, expensesList, accountTypesList, privacyMode ->
        val liabilityTypeNames = accountTypesList.filter { it.isLiability }.map { it.name }.toSet()
        
        val assets = accountsList.filter { it.type !in liabilityTypeNames }
            .sumOf { it.balance }
        val liabilities = accountsList.filter { it.type in liabilityTypeNames }
            .sumOf { it.balance }
        val total = assets - liabilities

        val grouped = accountsList.groupBy { it.type }

        AccountScreenState(
            assets = assets,
            liabilities = liabilities,
            total = total,
            accountsByType = grouped,
            accountTypes = accountTypesList,
            currentCurrency = localeManager.getCurrency(),
            isLoading = false,
            isPrivacyModeEnabled = privacyMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountScreenState()
    )

    fun addAccount(
        name: String,
        type: String,
        initialBalance: Long,
        currency: String = "IDR",
        interestRate: Double = 0.0,
        minPayment: Long = 0
    ) {
        viewModelScope.launch {
            val currentMax = state.value.accountsByType[type]?.maxOfOrNull { it.displayOrder } ?: 0
            accountRepository.insertAccount(
                AccountEntity(
                    name = name,
                    type = type,
                    balance = initialBalance,
                    currency = currency,
                    interestRate = interestRate,
                    minPayment = minPayment,
                    displayOrder = currentMax + 1
                )
            )
        }
    }

    fun updateAccount(
        account: AccountEntity,
        newName: String,
        newType: String,
        newBalance: Long,
        recordAdjustment: Boolean = false,
        interestRate: Double = 0.0,
        minPayment: Long = 0
    ) {
        viewModelScope.launch {
            val diff = newBalance - account.balance
            if (recordAdjustment && diff != 0L) {
                val isIncome = diff > 0
                val amount = if (isIncome) diff else -diff
                val type = if (isIncome) "INCOME" else "EXPENSE"

                expenseRepository.insertExpense(
                    com.sans.finance.domain.model.Expense(
                        date = System.currentTimeMillis(),
                        title = "Adjustment Balance",
                        amount = amount,
                        categoryId = 1L, // Default category
                        accountId = account.id,
                        type = type,
                        currency = account.currency,
                        details = "Manual balance adjustment for ${account.name}"
                    )
                )
                // Also update the account details
                accountRepository.updateAccount(
                    account.copy(
                        name = newName,
                        type = newType,
                        balance = newBalance,
                        interestRate = interestRate,
                        minPayment = minPayment,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                // If not recording adjustment, just update the account directly
                accountRepository.updateAccount(
                    account.copy(
                        name = newName,
                        type = newType,
                        balance = newBalance,
                        interestRate = interestRate,
                        minPayment = minPayment,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            accountRepository.deleteAccountById(id)
        }
    }

    fun togglePrivacyMode() {
        localeManager.setPrivacyModeEnabled(!localeManager.isPrivacyModeEnabled())
    }

    fun moveAccountUp(account: AccountEntity) {
        val list = state.value.accountsByType[account.type] ?: return
        val index = list.indexOf(account)
        if (index > 0) {
            viewModelScope.launch {
                val prev = list[index - 1]
                if (account.displayOrder == prev.displayOrder) {
                    list.forEachIndexed { i, item ->
                        accountRepository.updateAccount(item.copy(displayOrder = i))
                    }
                    val refreshedList = list.mapIndexed { i, item -> item.copy(displayOrder = i) }
                    val current = refreshedList[index]
                    val previous = refreshedList[index - 1]
                    accountRepository.updateAccount(current.copy(displayOrder = index - 1))
                    accountRepository.updateAccount(previous.copy(displayOrder = index))
                } else {
                    accountRepository.updateAccount(account.copy(displayOrder = prev.displayOrder))
                    accountRepository.updateAccount(prev.copy(displayOrder = account.displayOrder))
                }
            }
        }
    }

    fun moveAccountDown(account: AccountEntity) {
        val list = state.value.accountsByType[account.type] ?: return
        val index = list.indexOf(account)
        if (index < list.size - 1) {
            viewModelScope.launch {
                val next = list[index + 1]
                if (account.displayOrder == next.displayOrder) {
                    list.forEachIndexed { i, item ->
                        accountRepository.updateAccount(item.copy(displayOrder = i))
                    }
                    val refreshedList = list.mapIndexed { i, item -> item.copy(displayOrder = i) }
                    val current = refreshedList[index]
                    val nextItem = refreshedList[index + 1]
                    accountRepository.updateAccount(current.copy(displayOrder = index + 1))
                    accountRepository.updateAccount(nextItem.copy(displayOrder = index))
                } else {
                    accountRepository.updateAccount(account.copy(displayOrder = next.displayOrder))
                    accountRepository.updateAccount(next.copy(displayOrder = account.displayOrder))
                }
            }
        }
    }
}
