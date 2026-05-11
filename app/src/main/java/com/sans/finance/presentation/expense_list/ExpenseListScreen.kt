package com.sans.finance.presentation.expense_list


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.R
import com.sans.finance.domain.model.Expense
import com.sans.finance.presentation.components.CategoryIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    onAddTransactionClick: () -> Unit,

    onInstallmentsClick: () -> Unit,
    onStatsClick: () -> Unit,
    onExpenseClick: (Long) -> Unit,
    viewModel: ExpenseListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val haptic = LocalHapticFeedback.current
    LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }


    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onStatsClick) {
                        Icon(Icons.Default.QueryStats, contentDescription = "Statistics")
                    }
                    IconButton(onClick = onInstallmentsClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = "Active Installments"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.search_expenses)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showFilterSheet = true }) {
                            val isFiltered = state.selectedCategoryIds.isNotEmpty() ||
                                    state.minAmount != null ||
                                    state.maxAmount != null ||
                                    state.selectedTags.isNotEmpty()
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Filters",
                                tint = if (isFiltered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            FilterTabs(
                activeFilter = state.activeDateFilter,
                onFilterSelected = { filter ->
                    viewModel.updateDateRange(filter)
                }
            )
            SummaryCard(
                income = state.totalFilteredIncome,
                expense = state.totalFilteredExpense,
                total = state.totalFilteredAmount,
                currencyCode = state.currentCurrency,
                avgMonthlyExpense = state.avgMonthlyExpense,
            )



            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.groupedExpenses.forEach { (date, expenses) ->
                    item(key = "header-$date") {
                        val cal = com.sans.finance.core.util.CalendarUtils.getInstance()
                            .apply { timeInMillis = date }
                        val day =
                            cal.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                        val dayOfWeek =
                            java.text.SimpleDateFormat("EEE", java.util.Locale.US).format(cal.time)
                        val monthYear = java.text.SimpleDateFormat("MM.yyyy", java.util.Locale.US)
                            .format(cal.time)

                        val dayIncome = expenses.filter { it.type == "INCOME" }
                            .sumOf { if (it.isInstallment && it.monthlyPayment > 0) it.monthlyPayment else it.amount }
                        val dayExpense = expenses.filter { it.type != "INCOME" }
                            .sumOf { if (it.isInstallment && it.monthlyPayment > 0) it.monthlyPayment else it.amount }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = day,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = dayOfWeek,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondary,
                                            modifier = Modifier.padding(
                                                horizontal = 4.dp,
                                                vertical = 2.dp
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(
                                        text = monthYear,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (dayIncome > 0) {
                                        Text(
                                            text = com.sans.finance.core.util.CurrencyFormatter.formatAmount(
                                                dayIncome,
                                                state.currentCurrency
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF4CAF50)
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                    }
                                    if (dayExpense > 0) {
                                        Text(
                                            text = com.sans.finance.core.util.CurrencyFormatter.formatAmount(
                                                dayExpense,
                                                state.currentCurrency
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFE53935)
                                        )
                                    }
                                    val dayTransfer = expenses.filter { it.type == "TRANSFER" }
                                        .sumOf { it.amount }
                                    if (dayTransfer > 0) {
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(
                                            text = com.sans.finance.core.util.CurrencyFormatter.formatAmount(
                                                dayTransfer,
                                                state.currentCurrency
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF2196F3)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(
                        items = expenses,
                        key = { it.id },
                        contentType = { "expense" }
                    ) { expense ->
                        ExpenseItem(
                            expense = expense,
                            category = state.categories.find { it.id == expense.categoryId },
                            account = state.accounts.find { it.id == expense.accountId },
                            onClick = { onExpenseClick(expense.id) },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                expenseToDelete = expense
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog && expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                expenseToDelete = null
            },
            title = { Text(stringResource(R.string.delete_confirmation_title)) },
            text = { Text(stringResource(R.string.delete_confirmation_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        expenseToDelete?.let { viewModel.deleteExpense(it) }
                        showDeleteDialog = false
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
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

    if (showFilterSheet) {
        AdvancedFilterSheet(
            state = state,
            onDismiss = { showFilterSheet = false },
            onCategoryToggle = { viewModel.toggleCategoryFilter(it) },
            onAmountFilterChanged = { min, max -> viewModel.updateAmountFilter(min, max) },
            onTagToggle = { viewModel.toggleTagFilter(it) },
            onDateRangeSelected = { start, end -> viewModel.updateCustomDateRange(start, end) },
            onClearFilters = {
                viewModel.clearFilters()
                showFilterSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedFilterSheet(
    state: ExpenseListState,
    onDismiss: () -> Unit,
    onCategoryToggle: (Long) -> Unit,
    onAmountFilterChanged: (Long?, Long?) -> Unit,
    onTagToggle: (String) -> Unit,
    onDateRangeSelected: (Long, Long) -> Unit,
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
                    stringResource(R.string.filters),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearFilters) {
                    Text(stringResource(R.string.clear_filters))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Date Range",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

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
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                stringResource(R.string.category),
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
                            CategoryIcon(icon = category.icon, fontSize = 14.sp)
                        }
                    )
                }
            }

            if (state.availableTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.tags),
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
                stringResource(R.string.amount_spent),
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
                    label = { Text(stringResource(R.string.min_amount)) },
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
                    label = { Text(stringResource(R.string.max_amount)) },
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
                Text(stringResource(R.string.apply_filters))
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
                    Text(stringResource(R.string.cancel))
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

@Composable
fun SummaryCard(
    income: Long,
    expense: Long,
    total: Long,
    currencyCode: String,
    avgMonthlyExpense: Long = 0L
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Income",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        com.sans.finance.core.util.CurrencyFormatter.formatAmount(income, currencyCode),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF4CAF50)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Expenses",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        com.sans.finance.core.util.CurrencyFormatter.formatAmount(expense, currencyCode),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFE53935)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        com.sans.finance.core.util.CurrencyFormatter.formatAmount(total, currencyCode),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (avgMonthlyExpense > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Avg. Monthly Expense: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = com.sans.finance.core.util.CurrencyFormatter.formatAmount(avgMonthlyExpense, currencyCode),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            androidx.compose.material3.HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpenseItem(
    expense: Expense,
    category: com.sans.finance.data.local.entity.CategoryEntity?,
    account: com.sans.finance.data.local.entity.AccountEntity? = null,
    showNextDueDate: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val icon = category?.icon ?: ""

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = !expense.isInstallmentPayment,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = if (expense.isInstallmentPayment) MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = 0.5f
        ) else MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.weight(0.35f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryIcon(
                        icon = icon,
                        fontSize = 16.sp
                    )
                    Text(
                        category?.name ?: stringResource(R.string.uncategorized),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Column(modifier = Modifier.weight(0.65f)) {
                    Text(
                        expense.note.ifBlank { expense.description ?: "" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (expense.isInstallmentPayment) MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.7f
                        ) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        account?.name ?: "Unknown",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    val displayAmount =
                        if (expense.isInstallment && expense.monthlyPayment > 0) expense.monthlyPayment else expense.amount
                    val amountColor = when (expense.type) {
                        "INCOME" -> Color(0xFF4CAF50)
                        "EXPENSE" -> Color(0xFFE53935)
                        "TRANSFER" -> Color(0xFF2196F3)
                        else -> Color(0xFFE53935)
                    }
                    Text(
                        com.sans.finance.core.util.CurrencyFormatter.formatAmount(displayAmount, expense.currency),
                        style = MaterialTheme.typography.bodyMedium,
                        color = amountColor
                    )
                }
            }
            androidx.compose.material3.HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DateRangeFilterBar(
    activeFilter: DateRangeFilter,
    onFilterSelected: (DateRangeFilter) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = activeFilter == DateRangeFilter.SEVEN_DAYS,
            onClick = { onFilterSelected(DateRangeFilter.SEVEN_DAYS) },
            label = { Text(stringResource(R.string.filter_7d)) }
        )
        FilterChip(
            selected = activeFilter == DateRangeFilter.THIRTY_DAYS,
            onClick = { onFilterSelected(DateRangeFilter.THIRTY_DAYS) },
            label = { Text(stringResource(R.string.filter_30d)) }
        )
        FilterChip(
            selected = activeFilter == DateRangeFilter.THIS_MONTH,
            onClick = { onFilterSelected(DateRangeFilter.THIS_MONTH) },
            label = { Text(stringResource(R.string.filter_month)) }
        )
        FilterChip(
            selected = activeFilter == DateRangeFilter.ALL_TIME,
            onClick = { onFilterSelected(DateRangeFilter.ALL_TIME) },
            label = { Text(stringResource(R.string.filter_all)) }
        )
    }
}


@Composable
fun FilterTabs(
    activeFilter: DateRangeFilter,
    onFilterSelected: (DateRangeFilter) -> Unit
) {
    val tabIndex = when (activeFilter) {
        DateRangeFilter.SEVEN_DAYS -> 0
        DateRangeFilter.THIRTY_DAYS -> 1
        DateRangeFilter.THIS_MONTH -> 2
        DateRangeFilter.ALL_TIME -> 3
        else -> 0
    }

    androidx.compose.material3.SecondaryScrollableTabRow(
        selectedTabIndex = tabIndex,
        edgePadding = 0.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        divider = {
            androidx.compose.material3.HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )
        }
    ) {
        androidx.compose.material3.Tab(
            selected = tabIndex == 0,
            onClick = { onFilterSelected(DateRangeFilter.SEVEN_DAYS) },
            text = {
                Text(
                    "Daily (7d)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (tabIndex == 0) FontWeight.Bold else FontWeight.Normal
                )
            }
        )
        androidx.compose.material3.Tab(
            selected = tabIndex == 1,
            onClick = { onFilterSelected(DateRangeFilter.THIRTY_DAYS) },
            text = {
                Text(
                    "Calendar (30d)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (tabIndex == 1) FontWeight.Bold else FontWeight.Normal
                )
            }
        )
        androidx.compose.material3.Tab(
            selected = tabIndex == 2,
            onClick = { onFilterSelected(DateRangeFilter.THIS_MONTH) },
            text = {
                Text(
                    "Monthly",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (tabIndex == 2) FontWeight.Bold else FontWeight.Normal
                )
            }
        )
        androidx.compose.material3.Tab(
            selected = tabIndex == 3,
            onClick = { onFilterSelected(DateRangeFilter.ALL_TIME) },
            text = {
                Text(
                    "Total",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (tabIndex == 3) FontWeight.Bold else FontWeight.Normal
                )
            }
        )
    }
}
