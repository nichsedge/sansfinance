package com.sans.expensetracker.presentation.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.expensetracker.data.local.AppDatabase
import com.sans.expensetracker.data.local.entity.CategoryEntity
import com.sans.expensetracker.data.local.entity.TagEntity
import com.sans.expensetracker.domain.preferences.BudgetPreferences
import com.sans.expensetracker.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val localeManager: com.sans.expensetracker.data.util.LocaleManager,
    private val db: AppDatabase,
    private val budgetPreferences: BudgetPreferences
) : ViewModel() {

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _syncMessage = mutableStateOf<String?>(null)
    val syncMessage: State<String?> = _syncMessage

    val categories = repository.getAllCategories().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val tags = repository.getAllTagEntities().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentLanguage = mutableStateOf(localeManager.getLocale())
    val currentLanguage: State<String> = _currentLanguage

    fun updateLanguage(lang: String) {
        _currentLanguage.value = lang
    }

    val monthlyBudget = budgetPreferences.getMonthlyBudget().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    fun updateMonthlyBudget(amount: Long) {
        viewModelScope.launch {
            budgetPreferences.setMonthlyBudget(amount)
        }
    }

    // Category CRUD
    fun addCategory(name: String, icon: String) {
        viewModelScope.launch {
            repository.insertCategory(CategoryEntity(name = name, icon = icon))
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }


    fun onCategoriesReordered(reorderedCategories: List<CategoryEntity>) {
        viewModelScope.launch {
            val updatedCategories = reorderedCategories.mapIndexed { index, category ->
                category.copy(orderIndex = index)
            }
            repository.updateCategories(updatedCategories)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // Tag CRUD
    fun updateTag(tag: TagEntity) {
        viewModelScope.launch {
            repository.updateTag(tag)
        }
    }


    fun onTagsReordered(reorderedTags: List<TagEntity>) {
        viewModelScope.launch {
            val updatedTags = reorderedTags.mapIndexed { index, tag ->
                tag.copy(orderIndex = index)
            }
            repository.updateTags(updatedTags)
        }
    }

    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch {
            repository.deleteTag(tag)
        }
    }

    fun exportFullBackup(context: android.content.Context) {
        _isLoading.value = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                db.checkpoint()
                val dbName = "expense_tracker_db"
                val dbFile = context.getDatabasePath(dbName)

                if (!dbFile.exists()) {
                    _error.value = "Database not found"
                    _isLoading.value = false
                    return@launch
                }

                val snapshotName = "expense_tracker_db_snapshot.sqlite"
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
