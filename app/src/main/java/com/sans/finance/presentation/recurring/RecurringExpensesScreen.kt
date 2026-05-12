package com.sans.finance.presentation.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.R
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.domain.model.Expense
import com.sans.finance.presentation.expense_list.ExpenseItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpensesScreen(
    onNavigateBack: () -> Unit,
    onExpenseClick: (Long) -> Unit,
    onInstallmentsClick: () -> Unit,
    viewModel: RecurringExpensesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showViewMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showViewMenu = true }
                    ) {
                        Text(
                            stringResource(R.string.recurring_expenses),
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Switch View",
                            modifier = Modifier.size(20.dp)
                        )

                        DropdownMenu(
                            expanded = showViewMenu,
                            onDismissRequest = { showViewMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.recurring_expenses)) },
                                onClick = { showViewMenu = false },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Sync,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.active_installments)) },
                                onClick = {
                                    showViewMenu = false
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
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {

            // Total card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    val totalLabel = when (state.viewMode) {
                        RecurringViewMode.MONTHLY -> "Total Monthly Recurring"
                        RecurringViewMode.ANNUAL -> "Total Annual Cost"
                        RecurringViewMode.OPPORTUNITY_COST_10Y -> "10-Year Opportunity Cost"
                    }
                    val totalAmount = when (state.viewMode) {
                        RecurringViewMode.MONTHLY -> state.totalMonthlyRecurring
                        RecurringViewMode.ANNUAL -> state.totalMonthlyRecurring * 12
                        RecurringViewMode.OPPORTUNITY_COST_10Y -> (state.totalMonthlyRecurring * 173.0848).toLong()
                    }

                    Text(
                        totalLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        CurrencyFormatter.formatAmount(totalAmount, state.currentCurrency),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    if (state.viewMode == RecurringViewMode.OPPORTUNITY_COST_10Y) {
                        Text(
                            "Potential wealth if invested at 7% ROI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // View Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.viewMode == RecurringViewMode.MONTHLY,
                    onClick = { if (state.viewMode != RecurringViewMode.MONTHLY) viewModel.toggleViewMode() },
                    label = { Text("Monthly") }
                )
                FilterChip(
                    selected = state.viewMode == RecurringViewMode.ANNUAL,
                    onClick = { if (state.viewMode != RecurringViewMode.ANNUAL) viewModel.toggleViewMode() },
                    label = { Text("Annual") }
                )
                FilterChip(
                    selected = state.viewMode == RecurringViewMode.OPPORTUNITY_COST_10Y,
                    onClick = { if (state.viewMode != RecurringViewMode.OPPORTUNITY_COST_10Y) viewModel.toggleViewMode() },
                    label = { Text("10-Year Wealth") }
                )
            }

            if (state.recurringExpenses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No recurring expenses found.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.recurringExpenses, key = { it.id }) { expense ->
                        val category = state.categories.find { it.id == expense.categoryId }
                        val monthly = calculateMonthlyAmount(expense)
                        val displayAmount = when (state.viewMode) {
                            RecurringViewMode.MONTHLY -> null // Use default
                            RecurringViewMode.ANNUAL -> monthly * 12
                            RecurringViewMode.OPPORTUNITY_COST_10Y -> (monthly * 173.0848).toLong()
                        }
                        val overrideLabel = when (state.viewMode) {
                            RecurringViewMode.MONTHLY -> null
                            RecurringViewMode.ANNUAL -> "per year"
                            RecurringViewMode.OPPORTUNITY_COST_10Y -> "10y impact"
                        }

                        ExpenseItem(
                            expense = expense,
                            category = category,
                            showNextDueDate = true,
                            overrideAmount = displayAmount,
                            overrideLabel = overrideLabel,
                            onClick = { onExpenseClick(expense.id) },
                            onLongClick = {}
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

private fun calculateMonthlyAmount(expense: Expense): Long {
    return when (expense.recurrenceInterval) {
        "DAILY" -> expense.amount * 30
        "WEEKLY" -> expense.amount * 4
        "MONTHLY" -> expense.amount
        "YEARLY" -> expense.amount / 12
        else -> expense.amount
    }
}
