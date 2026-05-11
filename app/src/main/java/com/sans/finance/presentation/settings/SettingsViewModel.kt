package com.sans.finance.presentation.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.local.AppDatabase
import com.sans.finance.data.local.entity.BudgetEntity
import com.sans.finance.domain.repository.BudgetRepository
import com.sans.finance.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val localeManager: com.sans.finance.data.util.LocaleManager,
    private val db: AppDatabase,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _syncMessage = mutableStateOf<String?>(null)
    val syncMessage: State<String?> = _syncMessage

    private val _currentLanguage = mutableStateOf(localeManager.getLocale())
    val currentLanguage: State<String> = _currentLanguage

    private val _currentCurrency = mutableStateOf(localeManager.getCurrency())
    val currentCurrency: State<String> = _currentCurrency

    private val _enabledCurrencies = mutableStateOf(localeManager.getEnabledCurrencies())
    val enabledCurrencies: State<List<String>> = _enabledCurrencies

    fun updateLanguage(lang: String) {
        _currentLanguage.value = lang
    }

    fun toggleCurrency() {
        val enabled = _enabledCurrencies.value
        val currentIndex = enabled.indexOf(_currentCurrency.value)
        val next = if (currentIndex != -1 && currentIndex + 1 < enabled.size) enabled[currentIndex + 1] else enabled.firstOrNull() ?: "USD"
        localeManager.setCurrency(next)
        _currentCurrency.value = next
    }

    fun toggleEnabledCurrency(currency: String) {
        val currentList = _enabledCurrencies.value.toMutableList()
        if (currentList.contains(currency)) {
            if (currentList.size > 1) { // Keep at least one
                currentList.remove(currency)
            }
        } else {
            currentList.add(currency)
        }
        localeManager.setEnabledCurrencies(currentList)
        _enabledCurrencies.value = currentList
    }

    val monthlyBudget = budgetRepository.getAllBudgets().map { budgets ->
        budgets.find { it.categoryId == null }?.amount ?: 0L
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    fun updateMonthlyBudget(amount: Long) {
        viewModelScope.launch {
            val budgets = budgetRepository.getAllBudgets().first()
            val existingGlobal = budgets.find { it.categoryId == null }
            if (existingGlobal != null) {
                budgetRepository.insertBudget(existingGlobal.copy(amount = amount))
            } else {
                budgetRepository.insertBudget(BudgetEntity(amount = amount, categoryId = null))
            }
        }
    }


    fun exportFullBackup(context: android.content.Context) {
        _isLoading.value = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                db.checkpoint()
                val dbName = "sans_finance_db"
                val dbFile = context.getDatabasePath(dbName)

                if (!dbFile.exists()) {
                    _error.value = "Database not found"
                    _isLoading.value = false
                    return@launch
                }

                val snapshotName = "sans_finance_db_snapshot.sqlite"
                val resolver = context.contentResolver
                val relativePath = "${android.os.Environment.DIRECTORY_DOWNLOADS}/"

                val selection = "${android.provider.MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf(snapshotName)
                resolver.delete(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    selection,
                    selectionArgs
                )

                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, snapshotName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/x-sqlite3")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val uri = resolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                uri?.let {
                    resolver.openOutputStream(it, "wt")?.use { outputStream ->
                        java.io.FileInputStream(dbFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val done = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                        }
                        resolver.update(it, done, null, null)
                    }
                    _syncMessage.value = "Snapshot Saved: $snapshotName"
                    _isLoading.value = false
                } ?: run {
                    _error.value = "Failed to create snapshot"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _error.value = null
        _syncMessage.value = null
    }
}
