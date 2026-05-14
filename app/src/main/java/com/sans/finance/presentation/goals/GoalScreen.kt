package com.sans.finance.presentation.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.data.local.entity.GoalEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalScreen(
    onBack: () -> Unit,
    viewModel: GoalViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<GoalEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Savings Goals", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
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
            items(state.goals) { goalWithProgress ->
                GoalItem(
                    goalWithProgress = goalWithProgress,
                    onEdit = { goalToEdit = goalWithProgress.goal },
                    onDelete = { viewModel.deleteGoal(goalWithProgress.goal) }
                )
            }
        }

        if (showAddDialog || goalToEdit != null) {
            AddGoalDialog(
                goalToEdit = goalToEdit,
                categories = state.categories,
                assetClasses = state.assetClasses,
                onDismiss = {
                    showAddDialog = false
                    goalToEdit = null
                },
                onConfirm = { name, amount, type, targetName, deadline ->
                    if (goalToEdit != null) {
                        viewModel.updateGoalDetails(goalToEdit!!, name, amount, type, targetName, deadline)
                    } else {
                        viewModel.addGoal(name, amount, type, targetName, deadline)
                    }
                    showAddDialog = false
                    goalToEdit = null
                }
            )
        }
    }
}

@Composable
fun GoalItem(
    goalWithProgress: GoalWithProgress,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val goal = goalWithProgress.goal
    val progress =
        (goalWithProgress.currentAmount.toFloat() / goal.targetAmount.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        goal.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (goal.targetType) {
                            "TOTAL" -> "Total Portfolio"
                            "CATEGORY" -> "Category: ${goal.targetName}"
                            "ASSET_CLASS" -> "Asset Class: ${goal.targetName}"
                            else -> "Portfolio"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    goal.deadline?.let { deadline ->
                        Text(
                            text = "By ${SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(deadline))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    CurrencyFormatter.formatAmount(
                        (goalWithProgress.currentAmount * 100).toLong(),
                        goal.currency
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Target: ${
                        CurrencyFormatter.formatAmount(
                            (goal.targetAmount * 100),
                            goal.currency
                        )
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AddGoalDialog(
    goalToEdit: GoalEntity? = null,
    categories: List<String> = emptyList(),
    assetClasses: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String?, Long?) -> Unit
) {
    val isEditing = goalToEdit != null
    var name by remember(goalToEdit) { mutableStateOf(goalToEdit?.name ?: "") }
    var amount by remember(goalToEdit) {
        mutableStateOf(goalToEdit?.targetAmount?.toString() ?: "")
    }
    var targetType by remember(goalToEdit) { mutableStateOf(goalToEdit?.targetType ?: "TOTAL") }
    var targetName by remember(goalToEdit) { mutableStateOf(goalToEdit?.targetName ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var deadline by remember(goalToEdit) { mutableStateOf(goalToEdit?.deadline) }

    if (showDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = deadline ?: System.currentTimeMillis()
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    deadline = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            androidx.compose.material3.DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Saving Goal" else "New Saving Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name (e.g. Travel)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() } },
                    label = { Text("Target Amount") },
                    visualTransformation = com.sans.finance.core.util.ThousandsSeparatorVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = deadline?.let { SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(it)) } ?: "",
                    onValueChange = {},
                    label = { Text("Deadline (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Select Date")
                        }
                    }
                )

                Text("Track Progress From:", style = MaterialTheme.typography.labelMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("TOTAL", "CATEGORY", "ASSET_CLASS").forEach { type ->
                        val isSelected = targetType == type
                        androidx.compose.material3.FilterChip(
                            selected = isSelected,
                            onClick = {
                                targetType = type
                                if (type == "TOTAL") targetName = ""
                                else if (type == "CATEGORY" && !categories.contains(targetName)) targetName =
                                    categories.firstOrNull() ?: ""
                                else if (type == "ASSET_CLASS" && !assetClasses.contains(targetName)) targetName =
                                    assetClasses.firstOrNull() ?: ""
                            },
                            label = {
                                Text(
                                    when (type) {
                                        "TOTAL" -> "Portfolio"
                                        "CATEGORY" -> "Category"
                                        "ASSET_CLASS" -> "Asset Class"
                                        else -> type
                                    }
                                )
                            }
                        )
                    }
                }

                if (targetType != "TOTAL") {
                    val options = if (targetType == "CATEGORY") categories else assetClasses
                    var expanded by remember { mutableStateOf(false) }

                    Column {
                        OutlinedTextField(
                            value = targetName,
                            onValueChange = { targetName = it },
                            label = { Text(if (targetType == "CATEGORY") "Select Category" else "Select Asset Class") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { expanded = !expanded }) {
                                    Icon(
                                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                        androidx.compose.material3.DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            options.forEach { option ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        targetName = option
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val target = amount.toDoubleOrNull() ?: 0.0
                onConfirm(name, target, targetType, if (targetType == "TOTAL") null else targetName, deadline)
            }) {
                Text(if (isEditing) "Save" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
