package com.sans.finance.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    val accounts = accountRepository.getAllAccounts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
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
}
