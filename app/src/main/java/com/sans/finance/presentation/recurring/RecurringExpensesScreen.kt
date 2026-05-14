package com.sans.finance.presentation.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.sans.finance.presentation.components.ExpenseItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpensesScreen(
    onNavigateBack: () -> Unit,
    onExpenseClick: (Long) -> Unit,
    viewModel: RecurringExpensesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val expenseToDelete = remember {
        androidx.compose.runtime.mutableStateOf<com.sans.finance.domain.model.Expense?>(null)
    }
    var showDeleteDialog by remember { androidx.compose.runtime.mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.recurring_expenses),
                        fontWeight = FontWeight.Bold
                    )
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
        if (showDeleteDialog && expenseToDelete.value != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                    expenseToDelete.value = null
                },
                title = { Text("Delete Recurring Expense") },
                text = { Text("Are you sure you want to delete this recurring expense definition? Future transactions will no longer be tracked.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val exp: com.sans.finance.domain.model.Expense? = expenseToDelete.value
                            if (exp != null) {
                                viewModel.deleteExpense(exp)
                            }
                            showDeleteDialog = false
                            expenseToDelete.value = null
                        }
                    ) {
                        Text(
                            stringResource(R.string.delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        expenseToDelete.value = null
                    }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {

            // Total card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
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
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
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
                    .padding(bottom = 8.dp),
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            categoryName = category?.name,
                            categoryIcon = category?.icon ?: "",
                            showNextDueDate = true,
                            overrideAmount = displayAmount,
                            overrideLabel = overrideLabel,
                            onClick = { onExpenseClick(expense.id) },
                            onLongClick = {
                                expenseToDelete.value = expense
                                showDeleteDialog = true
                            }
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
