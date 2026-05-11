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
    val currentCurrency: String = "USD",
    val isLoading: Boolean = true,
    val isPrivacyModeEnabled: Boolean = false
)


@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val expenseRepository: ExpenseRepository,
    private val localeManager: com.sans.finance.data.util.LocaleManager
) : ViewModel() {

    val state = combine(
        accountRepository.getAllAccounts(),
        expenseRepository.getExpensesBetween(0, Long.MAX_VALUE),
        localeManager.privacyMode
    ) { accountsList, expensesList, privacyMode ->
        val assets = accountsList.filter { it.type != "Credit Card" && it.type != "Loan" }
            .sumOf { it.balance }
        val liabilities = accountsList.filter { it.type == "Credit Card" || it.type == "Loan" }
            .sumOf { it.balance }
        val total = assets - liabilities

        val grouped = accountsList.groupBy { it.type }

        AccountScreenState(
            assets = assets,
            liabilities = liabilities,
            total = total,
            accountsByType = grouped,
            currentCurrency = localeManager.getCurrency(),
            isLoading = false,
            isPrivacyModeEnabled = privacyMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountScreenState()
    )

    fun addAccount(name: String, type: String, initialBalance: Long, currency: String = "IDR") {
        viewModelScope.launch {
            accountRepository.insertAccount(
                AccountEntity(
                    name = name,
                    type = type,
                    balance = initialBalance,
                    currency = currency
                )
            )
        }
    }

    fun updateAccount(account: AccountEntity, newName: String, newType: String, newBalance: Long) {
        viewModelScope.launch {
            accountRepository.updateAccount(
                account.copy(
                    name = newName,
                    type = newType,
                    balance = newBalance,
                    updatedAt = System.currentTimeMillis()
                )
            )
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
}
