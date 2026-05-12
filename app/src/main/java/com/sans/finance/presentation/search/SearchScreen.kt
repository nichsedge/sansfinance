package com.sans.finance.presentation.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.R
import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.presentation.components.ExpenseItem
import com.sans.finance.presentation.components.SummaryCard
import com.sans.finance.presentation.expense_list.DateRangeFilter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onExpenseClick: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<com.sans.finance.domain.model.Expense?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    if (state.searchQuery.isEmpty()) {
                                        Text(
                                            "Search...",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.updateSearchQuery("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        val isFiltered = state.selectedCategoryIds.isNotEmpty() ||
                                state.selectedAccountIds.isNotEmpty() ||
                                state.minAmount != null ||
                                state.maxAmount != null ||
                                state.selectedTags.isNotEmpty() ||
                                state.selectedTypes.isNotEmpty() ||
                                state.activeDateFilter != DateRangeFilter.ALL_TIME
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Filters",
                            tint = if (isFiltered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (showDeleteDialog && expenseToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    expenseToDelete = null
                },
                title = { Text(stringResource(R.string.delete_confirmation_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.delete_confirmation_msg))
                        if (expenseToDelete?.isInstallmentPayment == true) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "This is an installment payment. Deleting it will mark it as unpaid in the plan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val exp: com.sans.finance.domain.model.Expense? = expenseToDelete
                            if (exp != null) {
                                viewModel.deleteExpense(exp)
                            }
                            showDeleteDialog = false
                            expenseToDelete = null
                        }
                    ) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        expenseToDelete = null
                    }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.expenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (state.searchQuery.isEmpty() && !isFiltered(state)) "Type to search" else "No transactions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                SummaryCard(
                    income = state.totalIncome,
                    expense = state.totalExpense,
                    total = state.netAmount,
                    currencyCode = state.currentCurrency,
                    isPrivacyModeEnabled = state.isPrivacyModeEnabled
                )

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    state.groupedExpenses.forEach { (dateMillis, expenses) ->
                        stickyHeader {
                            DateHeader(dateMillis)
                        }
                        items(expenses, key = { it.id }) { expense ->
                            val category = state.categories.find { it.id == expense.categoryId }
                            val account = state.accounts.find { it.id == expense.accountId }
                            ExpenseItem(
                                expense = expense,
                                categoryName = category?.name,
                                categoryIcon = category?.icon ?: (if (expense.isInstallmentPayment) "💳" else "📁"),
                                accountName = account?.name,
                                isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                                onClick = { onExpenseClick(expense.id) },
                                onLongClick = {
                                    expenseToDelete = expense
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        // Reuse AdvancedFilterSheet from ExpenseListScreen
        // Since it's currently private to ExpenseListScreen, we'll need to move it or duplicate it for now.
        // I'll duplicate it for now to avoid breaking ExpenseListScreen while I refactor.
        SearchFilterSheet(
            state = state,
            onDismiss = { showFilterSheet = false },
            onCategoryToggle = { viewModel.toggleCategoryFilter(it) },
            onAccountToggle = { viewModel.toggleAccountFilter(it) },
            onAmountFilterChanged = { min, max -> viewModel.updateAmountFilter(min, max) },
            onTagToggle = { viewModel.toggleTagFilter(it) },
            onTypeToggle = { viewModel.toggleTypeFilter(it) },
            onDateRangeSelected = { start, end -> viewModel.updateDateRange(DateRangeFilter.CUSTOM, start, end) },
            onQuickDateSelected = { viewModel.updateDateRange(it) },
            onClearFilters = { viewModel.clearFilters() }
        )
    }
}

private fun isFiltered(state: SearchState): Boolean {
    return state.selectedCategoryIds.isNotEmpty() ||
            state.selectedAccountIds.isNotEmpty() ||
            state.minAmount != null ||
            state.maxAmount != null ||
            state.selectedTags.isNotEmpty() ||
            state.selectedTypes.isNotEmpty() ||
            state.activeDateFilter != DateRangeFilter.ALL_TIME
}

@Composable
fun DateHeader(dateMillis: Long) {
    val calendar = CalendarUtils.getInstance()
    calendar.timeInMillis = dateMillis
    val dateStr = com.sans.finance.core.util.DateFormatterUtils.getStandardFormatter().format(calendar.time)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = dateStr,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// Simplified version of AdvancedFilterSheet for Search
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchFilterSheet(
    state: SearchState,
    onDismiss: () -> Unit,
    onCategoryToggle: (Long) -> Unit,
    onAccountToggle: (Long) -> Unit,
    onAmountFilterChanged: (Long?, Long?) -> Unit,
    onTagToggle: (String) -> Unit,
    onTypeToggle: (String) -> Unit,
    onDateRangeSelected: (Long, Long) -> Unit,
    onQuickDateSelected: (DateRangeFilter) -> Unit,
    onClearFilters: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var minAmountStr by remember {
        mutableStateOf(state.minAmount?.let {
            kotlin.math.ceil(it / 100.0).toLong().toString()
        } ?: "")
    }
    var maxAmountStr by remember {
        mutableStateOf(state.maxAmount?.let {
            kotlin.math.ceil(it / 100.0).toLong().toString()
        } ?: "")
    }

    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Filters",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearFilters) {
                    Text("Clear All")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Date Range",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dateFilters = listOf(
                    DateRangeFilter.THIS_WEEK to "This Week",
                    DateRangeFilter.THIS_MONTH to "This Month",
                    DateRangeFilter.LAST_MONTH to "Last Month",
                    DateRangeFilter.THIS_YEAR to "This Year",
                    DateRangeFilter.ALL_TIME to "All Time"
                )
                dateFilters.forEach { (filter, label) ->
                    FilterChip(
                        selected = state.activeDateFilter == filter,
                        onClick = { onQuickDateSelected(filter) },
                        label = { Text(label) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dateText = if (state.activeDateFilter == DateRangeFilter.CUSTOM) {
                    val startStr =
                        com.sans.finance.core.util.DateFormatterUtils.getStandardFormatter()
                            .format(java.util.Date(state.startDate))
                    val endStr =
                        com.sans.finance.core.util.DateFormatterUtils.getStandardFormatter()
                            .format(java.util.Date(state.endDate - 1))
                    "$startStr - $endStr"
                } else {
                    "Select Date Range"
                }

                OutlinedTextField(
                    value = dateText,
                    onValueChange = {},
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    singleLine = true,
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                    )
                )

                TextButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Select")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Account",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.accounts.forEach { account ->
                    FilterChip(
                        selected = state.selectedAccountIds.contains(account.id),
                        onClick = { onAccountToggle(account.id) },
                        label = { Text(account.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Transaction Type",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val types = listOf("EXPENSE", "INCOME", "TRANSFER")
                types.forEach { type ->
                    FilterChip(
                        selected = state.selectedTypes.contains(type),
                        onClick = { onTypeToggle(type) },
                        label = {
                            Text(
                                when (type) {
                                    "EXPENSE" -> "Expense"
                                    "INCOME" -> "Income"
                                    "TRANSFER" -> "Transfer"
                                    else -> type
                                }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Category",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.categories.forEach { category ->
                    FilterChip(
                        selected = state.selectedCategoryIds.contains(category.id),
                        onClick = { onCategoryToggle(category.id) },
                        label = { Text(category.name) },
                        leadingIcon = {
                            com.sans.finance.presentation.components.CategoryIcon(icon = category.icon, fontSize = 14.sp)
                        }
                    )
                }
            }

            if (state.availableTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Tags",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.availableTags.forEach { tag ->
                        FilterChip(
                            selected = state.selectedTags.contains(tag),
                            onClick = { onTagToggle(tag) },
                            label = { Text(tag) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Amount Range",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = minAmountStr,
                    onValueChange = {
                        minAmountStr = it
                        onAmountFilterChanged(
                            it.toLongOrNull()?.let { v -> v * 100 },
                            maxAmountStr.toLongOrNull()?.let { v -> v * 100 })
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Min") },
                    visualTransformation = com.sans.finance.core.util.ThousandsSeparatorVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = maxAmountStr,
                    onValueChange = {
                        maxAmountStr = it
                        onAmountFilterChanged(
                            minAmountStr.toLongOrNull()?.let { v -> v * 100 },
                            it.toLongOrNull()?.let { v -> v * 100 })
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Max") },
                    visualTransformation = com.sans.finance.core.util.ThousandsSeparatorVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            ) {
                Text("Apply Filters")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = if (state.activeDateFilter == DateRangeFilter.CUSTOM) state.startDate else null,
            initialSelectedEndDateMillis = if (state.activeDateFilter == DateRangeFilter.CUSTOM) state.endDate - 1 else null
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = datePickerState.selectedStartDateMillis
                        val end = datePickerState.selectedEndDateMillis
                        if (start != null && end != null) {
                            onDateRangeSelected(start, end)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = datePickerState,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
