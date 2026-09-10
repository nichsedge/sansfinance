package com.sans.finance.presentation.expense_list


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sans.finance.R
import com.sans.finance.domain.model.Expense
import com.sans.finance.presentation.components.ExpenseItem
import com.sans.finance.presentation.components.PrivacyText
import com.sans.finance.presentation.components.SummaryCard
import com.sans.finance.presentation.components.TodaySeparator
import com.sans.finance.presentation.installments.InstallmentDetailBottomSheet
import com.sans.finance.presentation.recurring.RecurringDetailBottomSheet
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExpenseListScreen(
    onAddTransactionClick: () -> Unit,
    onStatsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onExpenseClick: (Long) -> Unit,
    viewModel: ExpenseListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
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
            com.sans.finance.core.util.DateFormatterUtils.getMonthYearFormatter().format(cal.time)
        }
    }

    val categoriesMap = remember(state.categories) {
        state.categories.associateBy { it.id }
    }
    val accountsMap = remember(state.accounts) {
        state.accounts.associateBy { it.id }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.previousMonth()
                    }) {
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
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showDatePicker = true
                            },
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
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.nextMonth()
                        }) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Next",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSearchClick()
                    }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_expenses)
                        )
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onStatsClick()
                    }) {
                        Icon(
                            Icons.Default.Insights,
                            contentDescription = "Statistics"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = paddingValues.calculateStartPadding(layoutDirection),
                    top = paddingValues.calculateTopPadding(),
                    end = paddingValues.calculateEndPadding(layoutDirection)
                )
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.activeCommitmentFilter == TimelineCommitmentFilter.ALL,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setCommitmentFilter(TimelineCommitmentFilter.ALL)
                    },
                    label = { Text("All", style = MaterialTheme.typography.labelMedium) },
                    shape = CircleShape
                )
                FilterChip(
                    selected = state.activeCommitmentFilter == TimelineCommitmentFilter.INSTALLMENTS,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val next = if (state.activeCommitmentFilter == TimelineCommitmentFilter.INSTALLMENTS) {
                            TimelineCommitmentFilter.ALL
                        } else {
                            TimelineCommitmentFilter.INSTALLMENTS
                        }
                        viewModel.setCommitmentFilter(next)
                    },
                    label = {
                        val installmentText = if (state.activeInstallmentCount > 0) {
                            "💳 Installments (${state.activeInstallmentCount})"
                        } else {
                            "💳 Installments"
                        }
                        Text(
                            text = installmentText,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    shape = CircleShape
                )
                FilterChip(
                    selected = state.activeCommitmentFilter == TimelineCommitmentFilter.RECURRING,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val next = if (state.activeCommitmentFilter == TimelineCommitmentFilter.RECURRING) {
                            TimelineCommitmentFilter.ALL
                        } else {
                            TimelineCommitmentFilter.RECURRING
                        }
                        viewModel.setCommitmentFilter(next)
                    },
                    label = {
                        val recurringText = if (state.recurringExpenseCount > 0) {
                            "🔄 Recurring (${state.recurringExpenseCount})"
                        } else {
                            "🔄 Recurring"
                        }
                        Text(
                            text = recurringText,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    shape = CircleShape
                )
            }

            if (state.timelineItems.isEmpty() && !state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val emptyMessage = when (state.activeCommitmentFilter) {
                        TimelineCommitmentFilter.INSTALLMENTS -> stringResource(R.string.no_active_installments)
                        TimelineCommitmentFilter.RECURRING -> "No recurring expenses found."
                        TimelineCommitmentFilter.ALL -> stringResource(R.string.no_data_available)
                    }
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        top = 8.dp,
                        end = 12.dp,
                        bottom = paddingValues.calculateBottomPadding() + 72.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.timelineItems.forEach { item ->
                        when (item) {
                            is TimelineItem.TodaySeparator -> {
                                item(key = item.key, contentType = "TodaySeparator") { TodaySeparator() }
                            }

                            is TimelineItem.Header -> {
                                stickyHeader(key = item.key, contentType = "Header") {
                                    TimelineHeader(
                                        header = item,
                                        currentCurrency = state.currentCurrency,
                                        isPrivacyModeEnabled = state.isPrivacyModeEnabled
                                    )
                                }
                            }

                            is TimelineItem.ExpenseItem -> {
                                item(key = item.key, contentType = "ExpenseItem") {
                                    val expense = item.expense
                                    val category = categoriesMap[expense.categoryId]
                                    val fallbackIcon = if (expense.isInstallmentPayment) "💳" else "📁"
                                    val icon = category?.icon ?: expense.categoryIcon ?: fallbackIcon
                                    ExpenseItem(
                                        expense = expense,
                                        categoryName = category?.name ?: expense.categoryName,
                                        categoryIcon = icon,
                                        accountName = accountsMap[expense.accountId]?.name,
                                        isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                                        onClick = {
                                            if (expense.isInstallment || expense.isInstallmentPayment) {
                                                viewModel.openInstallmentDetail(expense)
                                            } else if (expense.isRecurring) {
                                                viewModel.openRecurringDetail(expense)
                                            } else {
                                                onExpenseClick(expense.id)
                                            }
                                        },
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
            }
        }
    }

    if (state.selectedInstallment != null) {
        InstallmentDetailBottomSheet(
            context = com.sans.finance.presentation.installments.InstallmentDetailContext(
                installment = state.selectedInstallment!!,
                items = state.selectedInstallmentItems,
                currencyCode = state.currentCurrency
            ),
            onDismiss = viewModel::closeInstallmentDetail,
            onToggleStatus = viewModel::toggleInstallmentItemStatus,
            onEditExpense = onExpenseClick,
            onDeletePlan = viewModel::deleteInstallmentPlan
        )
    }

    if (state.selectedRecurringExpense != null) {
        val recurringExpense = state.selectedRecurringExpense!!
        val cat = categoriesMap[recurringExpense.categoryId]
        val acc = accountsMap[recurringExpense.accountId]
        RecurringDetailBottomSheet(
            context = com.sans.finance.presentation.recurring.RecurringExpenseContext(
                expense = recurringExpense,
                categoryName = cat?.name ?: recurringExpense.categoryName,
                accountName = acc?.name,
                currencyCode = state.currentCurrency
            ),
            onDismiss = viewModel::closeRecurringDetail,
            onEditExpense = onExpenseClick,
            onDeleteExpense = viewModel::deleteRecurringExpense,
            onTogglePause = viewModel::togglePauseRecurring
        )
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
        val cal = com.sans.finance.core.util.CalendarUtils.getInstance().apply {
            timeInMillis = if (state.startDate > 0) state.startDate else System.currentTimeMillis()
        }
        com.sans.finance.presentation.components.MonthYearPickerDialog(
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { month, year ->
                val jumpCal = com.sans.finance.core.util.CalendarUtils.getInstance().apply {
                    set(java.util.Calendar.YEAR, year)
                    set(java.util.Calendar.MONTH, month)
                    set(java.util.Calendar.DAY_OF_MONTH, 1)
                }
                viewModel.jumpToDate(jumpCal.timeInMillis)
                showDatePicker = false
            },
            initialMonth = cal.get(java.util.Calendar.MONTH),
            initialYear = cal.get(java.util.Calendar.YEAR)
        )
    }
}

@Composable
private fun TimelineHeader(
    header: TimelineItem.Header,
    currentCurrency: String,
    isPrivacyModeEnabled: Boolean
) {
    val dayIncome = header.income
    val dayExpense = header.expense
    val headerTitle = header.formattedDate.ifEmpty {
        val cal = com.sans.finance.core.util.CalendarUtils.getInstance().apply { timeInMillis = header.date }
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val dayOfWeek = com.sans.finance.core.util.DateFormatterUtils.formatDayOfWeek(cal.time)
        "$day $dayOfWeek"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                        text = headerTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (dayIncome > 0) {
                        PrivacyText(
                            amount = dayIncome,
                            currencyCode = currentCurrency,
                            isVisible = !isPrivacyModeEnabled,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF43A047)
                        )
                    }
                    if (dayExpense > 0) {
                        PrivacyText(
                            amount = dayExpense,
                            currencyCode = currentCurrency,
                            isVisible = !isPrivacyModeEnabled,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
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
