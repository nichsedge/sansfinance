package com.sans.finance.presentation.expense_list


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.R
import com.sans.finance.domain.model.Expense
import com.sans.finance.presentation.components.ExpenseItem
import com.sans.finance.presentation.components.PrivacyText
import com.sans.finance.presentation.components.SummaryCard
import com.sans.finance.presentation.components.TodaySeparator
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    onAddTransactionClick: () -> Unit,
    onInstallmentsClick: () -> Unit,
    onStatsClick: () -> Unit,
    onRecurringExpensesClick: () -> Unit,
    onSearchClick: () -> Unit,
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
    var showMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSummaryExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }
    val monthYear = remember(state.startDate) {
        if (state.startDate <= 0L) "All Time"
        else {
            val cal = com.sans.finance.core.util.CalendarUtils.getInstance().apply {
                timeInMillis = state.startDate
            }
            java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault()).format(cal.time)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Prev",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { showDatePicker = true },
                            color = Color.Transparent,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                monthYear,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        IconButton(onClick = { viewModel.nextMonth() }) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Next",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_expenses)
                        )
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Statistics") },
                            onClick = {
                                showMenu = false
                                onStatsClick()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Insights,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.recurring_expenses)) },
                            onClick = {
                                showMenu = false
                                onRecurringExpensesClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.active_installments)) },
                            onClick = {
                                showMenu = false
                                onInstallmentsClick()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransactionClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isSummaryExpanded) {
                Box(modifier = Modifier.combinedClickable(
                    onClick = { },
                    onLongClick = { isSummaryExpanded = false }
                )) {
                    SummaryCard(
                        income = state.totalFilteredIncome,
                        expense = state.totalFilteredExpense,
                        total = state.totalFilteredAmount,
                        currencyCode = state.currentCurrency,
                        avgMonthlyExpense = state.avgMonthlyExpense,
                        isPrivacyModeEnabled = state.isPrivacyModeEnabled
                    )
                }
            } else {
                Surface(
                    onClick = { isSummaryExpanded = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Summary", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            val todayMillis = remember {
                com.sans.finance.core.util.CalendarUtils.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = 12.dp,
                    end = 12.dp,
                    bottom = 72.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                var hasShownTodaySeparator = false
                var hasFutureTransactions = false

                state.groupedExpenses.forEach { (date, expenses) ->
                    if (date > todayMillis) {
                        hasFutureTransactions = true
                    }

                    if (!hasShownTodaySeparator && date <= todayMillis && hasFutureTransactions) {
                        item(key = "today-separator") {
                            TodaySeparator()
                        }
                        hasShownTodaySeparator = true
                    }

                    stickyHeader(key = "header-$date") {
                        val cal = com.sans.finance.core.util.CalendarUtils.getInstance()
                            .apply { timeInMillis = date }
                        val day =
                            cal.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                        val dayOfWeek =
                            SimpleDateFormat("EEE", Locale.US).format(cal.time)
                        val monthYear = SimpleDateFormat("MM.yyyy", Locale.US)
                            .format(cal.time)

                        val dayIncome = expenses.filter { it.type == "INCOME" }
                            .sumOf { if (it.isInstallment && it.monthlyPayment > 0) it.monthlyPayment else it.amount }
                        val dayExpense = expenses.filter { it.type != "INCOME" }
                            .sumOf { if (it.isInstallment && it.monthlyPayment > 0) it.monthlyPayment else it.amount }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            tonalElevation = 0.dp
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$day $dayOfWeek",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(
                                            text = monthYear,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (dayIncome > 0) {
                                            PrivacyText(
                                                amount = dayIncome,
                                                currencyCode = state.currentCurrency,
                                                isVisible = !state.isPrivacyModeEnabled,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF4CAF50)
                                            )
                                            if (dayExpense > 0) Spacer(modifier = Modifier.size(8.dp))
                                        }
                                        if (dayExpense > 0) {
                                            PrivacyText(
                                                amount = dayExpense,
                                                currencyCode = state.currentCurrency,
                                                isVisible = !state.isPrivacyModeEnabled,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFE53935)
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth(),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }

                    items(
                        items = expenses,
                        key = { it.id },
                        contentType = { "expense" }
                    ) { expense ->
                        val category = state.categories.find { it.id == expense.categoryId }
                        ExpenseItem(
                            expense = expense,
                            categoryName = category?.name,
                            categoryIcon = category?.icon ?: "",
                            accountName = state.accounts.find { it.id == expense.accountId }?.name,
                            isPrivacyModeEnabled = state.isPrivacyModeEnabled,
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (state.startDate > 0) state.startDate else System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.jumpToDate(it)
                    }
                    showDatePicker = false
                }) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

}
