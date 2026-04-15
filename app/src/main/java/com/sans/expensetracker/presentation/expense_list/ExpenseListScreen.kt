package com.sans.expensetracker.presentation.expense_list

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberDateRangePickerState
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
import com.sans.expensetracker.R
import com.sans.expensetracker.domain.model.Expense
import com.sans.expensetracker.presentation.expense_list.DateRangeFilter
import com.sans.expensetracker.presentation.components.CategoryIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    onAddExpenseClick: () -> Unit,
    onScanReceiptClick: () -> Unit,
    onInstallmentsClick: () -> Unit,
    onStatsClick: () -> Unit,
    onSettingsClick: () -> Unit,
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
                        Icon(Icons.Default.Payments, contentDescription = "Active Installments")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                ExtendedFloatingActionButton(
                    onClick = onScanReceiptClick,
                    icon = { Icon(Icons.Default.DocumentScanner, contentDescription = stringResource(R.string.scan_receipt)) },
                    text = { Text(stringResource(R.string.scan_receipt)) },
                    modifier = Modifier.padding(bottom = 16.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                ExtendedFloatingActionButton(
                    onClick = onAddExpenseClick,
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Expense") },
                    text = { Text("Add Expense") }
                )
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
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            SummaryCard(
                periodTotal = state.totalFilteredAmount,
                budget = state.monthlyBudget
            )

            Text(
                stringResource(R.string.recent_transactions),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
                fontWeight = FontWeight.Bold
            )

            DateRangeFilterBar(
                activeFilter = state.activeDateFilter,
                onFilterSelected = { filter ->
                    viewModel.updateDateRange(filter)
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.groupedExpenses.forEach { (date, expenses) ->
                    item(key = "header-$date") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            color = Color.Transparent
                        ) {
                            Text(
                                date,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
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
                    val startStr = com.sans.expensetracker.core.util.DateFormatterUtils.getStandardFormatter().format(java.util.Date(state.startDate))
                    val endStr = com.sans.expensetracker.core.util.DateFormatterUtils.getStandardFormatter().format(java.util.Date(state.endDate - 1))
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
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
fun SummaryCard(periodTotal: Long, budget: Long = 0L) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                stringResource(R.string.total_filtered),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                com.sans.expensetracker.core.util.CurrencyFormatter.formatAmount(periodTotal),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (budget > 0L) {
                Spacer(modifier = Modifier.height(16.dp))
                val progress = (periodTotal.toFloat() / budget.toFloat()).coerceIn(0f, 1f)
                val isOverBudget = periodTotal > budget
                val progressColor =
                    if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Monthly Budget Progress",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = progressColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = progressColor,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${
                            com.sans.expensetracker.core.util.CurrencyFormatter.formatAmount(
                                periodTotal
                            )
                        } of ${
                            com.sans.expensetracker.core.util.CurrencyFormatter.formatAmount(
                                budget
                            )
                        }",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpenseItem(
    expense: Expense,
    category: com.sans.expensetracker.data.local.entity.CategoryEntity?,
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = if (expense.isInstallmentPayment) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = if (expense.isInstallmentPayment) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CategoryIcon(
                        icon = icon,
                        fontSize = 20.sp
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    expense.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = if (expense.isInstallmentPayment) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface
                )
                val merchantDisplay = when {
                    !expense.merchant.isNullOrBlank() -> "${expense.merchant} • "
                    expense.tags.isNotEmpty() -> "${expense.tags.joinToString(", ")} • "
                    else -> ""
                }

                Text(
                    "$merchantDisplay${category?.name ?: stringResource(R.string.uncategorized)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (expense.isInstallmentPayment) {
                    Text(
                        "Monthly Installment",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                } else if (expense.isInstallment && expense.monthlyPayment > 0) {
                    val totalPaid =
                        com.sans.expensetracker.core.util.CurrencyFormatter.formatAmount(expense.totalPaid)
                    val totalAmount =
                        com.sans.expensetracker.core.util.CurrencyFormatter.formatAmount(expense.amount)
                    Text(
                        "Paid: $totalPaid / $totalAmount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val displayAmount =
                    if (expense.isInstallment && expense.monthlyPayment > 0) expense.monthlyPayment else expense.amount
                Text(
                    com.sans.expensetracker.core.util.CurrencyFormatter.formatAmount(displayAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
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
