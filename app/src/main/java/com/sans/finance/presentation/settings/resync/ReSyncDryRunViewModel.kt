package com.sans.finance.presentation.settings.resync

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.domain.model.AccountSyncDryRunResult
import com.sans.finance.domain.model.ReSyncMode
import com.sans.finance.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReSyncDryRunViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val localeManager: com.sans.finance.data.util.LocaleManager
) : ViewModel() {

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _dryRunResults = mutableStateOf<List<AccountSyncDryRunResult>>(emptyList())
    val dryRunResults: State<List<AccountSyncDryRunResult>> = _dryRunResults

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _successMessage = mutableStateOf<String?>(null)
    val successMessage: State<String?> = _successMessage

    val currentCurrency = localeManager.getCurrency()
    val isPrivacyModeEnabled: Boolean get() = localeManager.isPrivacyModeEnabled()

    private val _selectedMode = mutableStateOf(ReSyncMode.TRANSACTIONS_AS_TRUTH)
    val selectedMode: State<ReSyncMode> = _selectedMode

    private val _selectedDateOption = mutableStateOf("TODAY") // "TODAY", "EPOCH", "CUSTOM"
    val selectedDateOption: State<String> = _selectedDateOption

    private val _customDate = mutableStateOf(System.currentTimeMillis())
    val customDate: State<Long> = _customDate

    fun setSyncMode(mode: ReSyncMode) {
        _selectedMode.value = mode
    }

    fun setDateOption(option: String) {
        _selectedDateOption.value = option
    }

    fun setCustomDate(timestamp: Long) {
        _customDate.value = timestamp
    }

    init {
        loadDryRunData()
    }

    fun loadDryRunData() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val results = repository.getReSyncBalancesDryRun()
                _dryRunResults.value = results
            } catch (e: Exception) {
                _error.value = "Failed to calculate dry run: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun applyReSync() {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val adjustmentDate = when (_selectedDateOption.value) {
                    "EPOCH" -> 0L
                    "CUSTOM" -> _customDate.value
                    else -> System.currentTimeMillis()
                }
                repository.reSyncAccountBalances(_selectedMode.value, adjustmentDate)
                _successMessage.value = "Balances synchronized successfully"
                // Reload the dry run data to show the new synchronized state
                val results = repository.getReSyncBalancesDryRun()
                _dryRunResults.value = results
            } catch (e: Exception) {
                _error.value = "Failed to sync balances: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }
}
