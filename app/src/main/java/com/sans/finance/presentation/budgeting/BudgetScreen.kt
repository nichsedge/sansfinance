package com.sans.finance.presentation.budgeting

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.data.local.entity.CategoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val budgetStatuses by viewModel.budgetStatuses.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Budget")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (budgetStatuses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outlineVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No budgets set yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                "Plan your spending better!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }

            items(budgetStatuses) { status ->
                BudgetItem(
                    status = status,
                    onDelete = { viewModel.deleteBudget(status.budget) }
                )
            }
        }

        if (showAddDialog) {
            AddBudgetDialog(
                categories = categories,
                onDismiss = { showAddDialog = false },
                onConfirm = { amount, categoryId ->
                    viewModel.addBudget(amount, categoryId)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun BudgetItem(
    status: BudgetStatus,
    onDelete: () -> Unit
) {
    val progress = (status.spent.toFloat() / status.budget.amount.toFloat()).coerceIn(0f, 1.2f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceAtMost(1f),
        label = "budgetProgress"
    )

    val isOverBudget = status.spent > status.budget.amount
    val remaining = (status.budget.amount - status.spent).coerceAtLeast(0L)
    val overspent = (status.spent - status.budget.amount).coerceAtLeast(0L)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (isOverBudget)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        status.categoryName ?: "Global Budget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Monthly limit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    CurrencyFormatter.formatAmount(status.spent),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "of ${CurrencyFormatter.formatAmount(status.budget.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isOverBudget) MaterialTheme.colorScheme.error
                else if (progress > 0.8f) Color(0xFFFF9800)
                else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isOverBudget) {
                    Text(
                        "Overspent by ${CurrencyFormatter.formatAmount(overspent)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        "${CurrencyFormatter.formatAmount(remaining)} left",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set New Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() } },
                    label = { Text("Monthly Limit") },
                    placeholder = { Text("e.g. 5,000,000") },
                    prefix = { Text("Rp ") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = com.sans.finance.core.util.ThousandsSeparatorVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    shape = MaterialTheme.shapes.large
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = categories.find { it.id == selectedCategoryId }?.name
                            ?: "Global (All Categories)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Apply To") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Global (All Categories)") },
                            onClick = {
                                selectedCategoryId = null
                                expanded = false
                            },
                            leadingIcon = { Icon(Icons.Default.Language, null) }
                        )
                        HorizontalDivider()
                        categories.filter { it.type == "EXPENSE" }.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    expanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.Category, null) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = amount.toLongOrNull() ?: 0L
                    if (target > 0) {
                        onConfirm(target * 100, selectedCategoryId)
                    }
                },
                shape = MaterialTheme.shapes.large
            ) {
                Text("Set Budget")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
