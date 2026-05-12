package com.sans.finance.presentation.settings.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.domain.model.Category
import com.sans.finance.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategorySettingsViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _selectedType = MutableStateFlow("EXPENSE")
    val selectedType: StateFlow<String> = _selectedType.asStateFlow()

    val categories = _selectedType.flatMapLatest { type ->
        repository.getCategoriesByType(type)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setType(type: String) {
        _selectedType.value = type
    }

    fun addCategory(name: String, icon: String, type: String) {
        viewModelScope.launch {
            repository.insertCategory(Category(name = name, icon = icon, type = type))
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun onCategoriesReordered(reorderedCategories: List<Category>) {
        viewModelScope.launch {
            val updatedCategories = reorderedCategories.mapIndexed { index, category ->
                category.copy(orderIndex = index)
            }
            repository.updateCategories(updatedCategories)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }
}
