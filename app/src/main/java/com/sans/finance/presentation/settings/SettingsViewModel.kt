package com.sans.finance.presentation.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.local.AppDatabase
import com.sans.finance.data.local.entity.BudgetEntity
import com.sans.finance.domain.model.ReSyncMode
import com.sans.finance.domain.model.UserPreferences
import com.sans.finance.domain.repository.BudgetRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.TagRepository
import com.sans.finance.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val tagRepository: TagRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
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

    private val _isPrivacyModeEnabled = mutableStateOf(false)
    val isPrivacyModeEnabled: State<Boolean> = _isPrivacyModeEnabled

    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences()
    )

    init {
        viewModelScope.launch {
            userPreferences.collect { prefs ->
                _isPrivacyModeEnabled.value = prefs.isPrivacyModeEnabled
            }
        }
    }

    val supportedLanguages = com.sans.finance.data.util.LocaleManager.SUPPORTED_LANGUAGES
    val commonCurrencies = com.sans.finance.data.util.LocaleManager.COMMON_CURRENCIES
    val allAvailableCurrencies = com.sans.finance.data.util.LocaleManager.getAllAvailableCurrencies()

    fun setLanguage(lang: String) {
        localeManager.setLocale(lang)
        _currentLanguage.value = lang
    }

    fun setCurrency(currency: String) {
        localeManager.setCurrency(currency)
        _currentCurrency.value = currency
    }

    fun toggleCurrency() {
        val enabled = _enabledCurrencies.value
        val currentIndex = enabled.indexOf(_currentCurrency.value)
        val next =
            if (currentIndex != -1 && currentIndex + 1 < enabled.size) enabled[currentIndex + 1] else enabled.firstOrNull()
                ?: "USD"
        setCurrency(next)
    }

    fun restartApp(context: android.content.Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
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

    fun togglePrivacyMode() {
        viewModelScope.launch {
            val next = !userPreferences.value.isPrivacyModeEnabled
            userPreferencesRepository.setPrivacyModeEnabled(next)
        }
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

    fun cleanTags() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                tagRepository.cleanOrphanedTags()
                _syncMessage.value = "Tags cleaned successfully"
            } catch (e: Exception) {
                _error.value = "Failed to clean tags: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun reSyncBalances() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                repository.reSyncAccountBalances(ReSyncMode.TRANSACTIONS_AS_TRUTH, System.currentTimeMillis())
                _syncMessage.value = "Balances synchronized successfully"
            } catch (e: Exception) {
                _error.value = "Failed to sync balances: ${e.message}"
            } finally {
                _isLoading.value = false
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
                    return@launch
                }

                val snapshotName = "sans_finance_db_snapshot.sqlite"
                val resolver = context.contentResolver
                val collectionUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    android.provider.MediaStore.Downloads.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
                }

                // Delete any existing snapshot files (including old duplicate numbered files)
                try {
                    val projection = arrayOf(android.provider.MediaStore.MediaColumns._ID)
                    val selection = "${android.provider.MediaStore.MediaColumns.DISPLAY_NAME} LIKE 'sans_finance_db_snapshot%.sqlite'"
                    resolver.query(collectionUri, projection, selection, null, null)?.use { cursor ->
                        val idColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns._ID)
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idColumn)
                            val itemUri = android.content.ContentUris.withAppendedId(collectionUri, id)
                            try {
                                resolver.delete(itemUri, null, null)
                            } catch (_: Exception) {
                            }
                        }
                    }
                } catch (_: Exception) {
                }

                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, snapshotName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/x-sqlite3")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "${android.os.Environment.DIRECTORY_DOWNLOADS}/")
                        put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val targetUri = resolver.insert(collectionUri, contentValues)

                targetUri?.let { uri ->
                    resolver.openOutputStream(uri, "wt")?.use { outputStream ->
                        java.io.FileInputStream(dbFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val done = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                        }
                        resolver.update(uri, done, null, null)
                    }
                    _syncMessage.value = "Snapshot Saved: $snapshotName"
                } ?: run {
                    _error.value = "Failed to create snapshot"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    val backupFrequency: StateFlow<String> = userPreferences.map { it.backupFrequency }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "WEEKLY")
    val backupWifiOnly: StateFlow<Boolean> = userPreferences.map { it.backupWifiOnly }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val backupRequiresCharging: StateFlow<Boolean> = userPreferences.map { it.backupRequiresCharging }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val lastBackupTime: StateFlow<Long> = userPreferences.map { it.lastBackupTime }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val lastBackupSizeBytes: StateFlow<Long> = userPreferences.map { it.lastBackupSizeBytes }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val cloudBackupProvider: StateFlow<String> = userPreferences.map { it.cloudBackupProvider }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "CLOUDFLARE_R2")

    fun getCloudBackupProvider(): String = userPreferences.value.cloudBackupProvider

    fun setCloudBackupProvider(provider: String) {
        viewModelScope.launch {
            userPreferencesRepository.setCloudBackupProvider(provider)
        }
    }

    fun getR2AccountId(): String = userPreferences.value.r2AccountId
    fun getR2AccessKeyId(): String = userPreferences.value.r2AccessKeyId
    fun getR2SecretAccessKey(): String = userPreferences.value.r2SecretAccessKey
    fun getR2BucketName(): String = userPreferences.value.r2BucketName

    fun saveR2Config(accountId: String, accessKeyId: String, secretAccessKey: String, bucketName: String) {
        viewModelScope.launch {
            userPreferencesRepository.setR2Config(accountId, accessKeyId, secretAccessKey, bucketName)
        }
    }

    fun setBackupFrequency(freq: String, context: android.content.Context) {
        viewModelScope.launch {
            userPreferencesRepository.setBackupFrequency(freq)
            com.sans.finance.SansFinanceApp.rescheduleBackupWork(context, localeManager, userPreferencesRepository)
        }
    }

    fun setBackupWifiOnly(wifiOnly: Boolean, context: android.content.Context) {
        viewModelScope.launch {
            userPreferencesRepository.setBackupWifiOnly(wifiOnly)
            com.sans.finance.SansFinanceApp.rescheduleBackupWork(context, localeManager, userPreferencesRepository)
        }
    }

    fun setBackupRequiresCharging(requiresCharging: Boolean, context: android.content.Context) {
        viewModelScope.launch {
            userPreferencesRepository.setBackupRequiresCharging(requiresCharging)
            com.sans.finance.SansFinanceApp.rescheduleBackupWork(context, localeManager, userPreferencesRepository)
        }
    }

    fun uploadBackupToCloud(context: android.content.Context) {
        _isLoading.value = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val snapshotFile = java.io.File(context.cacheDir, "sans_finance_backup.sqlite")
            try {
                db.createBackupSnapshot(snapshotFile)
                if (!snapshotFile.exists() || snapshotFile.length() == 0L) {
                    _error.value = "Failed to create database snapshot"
                    _isLoading.value = false
                    return@launch
                }

                val fileSize = snapshotFile.length()
                val result = com.sans.finance.data.util.CloudStorageSyncer.uploadDatabaseBackup(context, snapshotFile, userPreferences.value)
                result.fold(
                    onSuccess = { msg ->
                        _syncMessage.value = msg
                        viewModelScope.launch {
                            userPreferencesRepository.setLastBackupTime(System.currentTimeMillis())
                            userPreferencesRepository.setLastBackupSizeBytes(fileSize)
                        }
                    },
                    onFailure = { err ->
                        _error.value = err.message ?: "Failed to upload to cloud"
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                if (snapshotFile.exists()) {
                    snapshotFile.delete()
                }
                _isLoading.value = false
            }
        }
    }

    fun restoreBackupFromCloud(context: android.content.Context) {
        _isLoading.value = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val downloadDest = java.io.File(context.cacheDir, "sans_finance_restore.sqlite")
            try {
                val result = com.sans.finance.data.util.CloudStorageSyncer.downloadDatabaseBackup(context, downloadDest, userPreferences.value)
                result.fold(
                    onSuccess = { downloadedFile ->
                        val isValid = com.sans.finance.data.util.CloudStorageSyncer.isDatabasePopulated(downloadedFile)
                        if (!isValid) {
                            _error.value = "Downloaded database is empty or corrupt (0 expenses)."
                            return@fold
                        }

                        // Close DB and replace files
                        db.close()
                        val currentDb = context.getDatabasePath("sans_finance_db")
                        val walFile = java.io.File(currentDb.path + "-wal")
                        val shmFile = java.io.File(currentDb.path + "-shm")

                        walFile.delete()
                        shmFile.delete()
                        downloadedFile.copyTo(currentDb, overwrite = true)
                        downloadedFile.delete()

                        _syncMessage.value = "Database restored successfully! Restarting app..."
                        kotlinx.coroutines.delay(1000)
                        restartApp(context)
                    },
                    onFailure = { err ->
                        _error.value = err.message ?: "Failed to download backup from cloud"
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                if (downloadDest.exists()) {
                    downloadDest.delete()
                }
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _error.value = null
        _syncMessage.value = null
    }
}
