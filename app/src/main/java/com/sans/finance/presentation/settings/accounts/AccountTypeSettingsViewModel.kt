package com.sans.finance.presentation.settings.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.local.entity.AccountTypeEntity
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.AccountTypeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountTypeSettingsViewModel @Inject constructor(
    private val repository: AccountTypeRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    val accountTypes = repository.getAllAccountTypes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        viewModelScope.launch {
            ensureAccountTypesSyncedWithAccounts()
        }
    }

    fun addAccountType(name: String, icon: String, isLiability: Boolean) {
        viewModelScope.launch {
            val currentMax = accountTypes.value.maxOfOrNull { it.displayOrder } ?: 0
            repository.insertAccountType(
                AccountTypeEntity(
                    name = name,
                    icon = icon,
                    isLiability = isLiability,
                    displayOrder = currentMax + 1
                )
            )
        }
    }

    fun updateAccountType(accountType: AccountTypeEntity, newName: String, newIcon: String, newIsLiability: Boolean) {
        viewModelScope.launch {
            if (newName != accountType.name) {
                accountRepository.renameTypeForAccounts(accountType.name, newName)
            }
            repository.updateAccountType(
                accountType.copy(
                    name = newName,
                    icon = newIcon,
                    isLiability = newIsLiability
                )
            )
        }
    }

    fun deleteAccountType(id: Long) {
        viewModelScope.launch {
            val type = accountTypes.value.firstOrNull { it.id == id } ?: return@launch
            val inUse = accountRepository.countAccountsByType(type.name)
            if (inUse > 0) {
                _message.value = "Can't delete '${type.name}'. $inUse accounts still use it."
                return@launch
            }
            repository.deleteAccountTypeById(id)
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private suspend fun ensureAccountTypesSyncedWithAccounts() {
        val existingTypes = repository.getAllAccountTypes().first()
        val existingNames = existingTypes.map { it.name }.toMutableSet()
        val maxOrder = existingTypes.maxOfOrNull { it.displayOrder } ?: 0

        val accountTypeNames = accountRepository.getAllAccounts().first()
            .map { it.type.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        var nextOrder = maxOrder + 1
        accountTypeNames.forEach { typeName ->
            if (typeName !in existingNames) {
                repository.insertAccountType(
                    AccountTypeEntity(
                        name = typeName,
                        icon = "AccountBalance",
                        isLiability = false,
                        displayOrder = nextOrder++
                    )
                )
                existingNames.add(typeName)
            }
        }
    }

    fun moveUp(type: AccountTypeEntity) {
        val list = accountTypes.value
        val index = list.indexOf(type)
        if (index > 0) {
            viewModelScope.launch {
                // Ensure all items have unique indices first if they are all 0
                val prev = list[index - 1]
                if (type.displayOrder == prev.displayOrder) {
                    list.forEachIndexed { i, item ->
                        repository.updateAccountType(item.copy(displayOrder = i))
                    }
                    // After re-indexing, perform the swap
                    val refreshedList = list.mapIndexed { i, item -> item.copy(displayOrder = i) }
                    val currentType = refreshedList[index]
                    val previousType = refreshedList[index - 1]
                    repository.updateAccountType(currentType.copy(displayOrder = index - 1))
                    repository.updateAccountType(previousType.copy(displayOrder = index))
                } else {
                    repository.updateAccountType(type.copy(displayOrder = prev.displayOrder))
                    repository.updateAccountType(prev.copy(displayOrder = type.displayOrder))
                }
            }
        }
    }

    fun moveDown(type: AccountTypeEntity) {
        val list = accountTypes.value
        val index = list.indexOf(type)
        if (index < list.size - 1) {
            viewModelScope.launch {
                val next = list[index + 1]
                if (type.displayOrder == next.displayOrder) {
                    list.forEachIndexed { i, item ->
                        repository.updateAccountType(item.copy(displayOrder = i))
                    }
                    val refreshedList = list.mapIndexed { i, item -> item.copy(displayOrder = i) }
                    val currentType = refreshedList[index]
                    val nextType = refreshedList[index + 1]
                    repository.updateAccountType(currentType.copy(displayOrder = index + 1))
                    repository.updateAccountType(nextType.copy(displayOrder = index))
                } else {
                    repository.updateAccountType(type.copy(displayOrder = next.displayOrder))
                    repository.updateAccountType(next.copy(displayOrder = type.displayOrder))
                }
            }
        }
    }
}
