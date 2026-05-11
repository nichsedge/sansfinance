package com.sans.finance.presentation.settings.categories

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.R
import com.sans.finance.data.local.entity.CategoryEntity
import com.sans.finance.presentation.settings.CategoryEditDialog
import com.sans.finance.presentation.settings.SettingsItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategorySettingsScreen(
    onBack: () -> Unit,
    viewModel: CategorySettingsViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddCategoryDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                PrimaryTabRow(selectedTabIndex = if (selectedType == "EXPENSE") 0 else 1) {
                    Tab(
                        selected = selectedType == "EXPENSE",
                        onClick = { viewModel.setType("EXPENSE") },
                        text = { Text("Expense") }
                    )
                    Tab(
                        selected = selectedType == "INCOME",
                        onClick = { viewModel.setType("INCOME") },
                        text = { Text("Income") }
                    )
                }
            }

            itemsIndexed(categories, key = { _, item -> item.id }) { index, category ->
                SettingsItem(
                    title = category.name,
                    icon = category.icon,
                    onEdit = { categoryToEdit = category },
                    onDelete = { categoryToDelete = category },
                    onMoveUp = if (index > 0) {
                        {
                            val mutable = categories.toMutableList()
                            val item = mutable.removeAt(index)
                            mutable.add(index - 1, item)
                            viewModel.onCategoriesReordered(mutable)
                        }
                    } else null,
                    onMoveDown = if (index < categories.size - 1) {
                        {
                            val mutable = categories.toMutableList()
                            val item = mutable.removeAt(index)
                            mutable.add(index + 1, item)
                            viewModel.onCategoriesReordered(mutable)
                        }
                    } else null,
                    modifier = Modifier.animateItem()
                )
            }
        }
    }

    if (showAddCategoryDialog) {
        CategoryEditDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name, icon ->
                viewModel.addCategory(name, icon, selectedType)
                showAddCategoryDialog = false
            }
        )
    }

    categoryToEdit?.let { category ->
        CategoryEditDialog(
            category = category,
            onDismiss = { categoryToEdit = null },
            onConfirm = { name, icon ->
                viewModel.updateCategory(category.copy(name = name, icon = icon))
                categoryToEdit = null
            }
        )
    }

    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete category '${category.name}'? All expenses in this category will become uncategorized.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(category)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
