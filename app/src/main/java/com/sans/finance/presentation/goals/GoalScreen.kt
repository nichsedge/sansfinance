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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.data.local.entity.GoalEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalScreen(
    onBack: () -> Unit,
    viewModel: GoalViewModel = hiltViewModel()
) {
    val goals by viewModel.goals.collectAsState()
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
            items(goals) { goal ->
                GoalItem(
                    goal = goal,
                    onEdit = { goalToEdit = goal },
                    onDelete = { viewModel.deleteGoal(goal) },
                    onAddContribution = { viewModel.updateProgress(goal, it) }
                )
            }
        }

        if (showAddDialog || goalToEdit != null) {
            AddGoalDialog(
                goalToEdit = goalToEdit,
                onDismiss = {
                    showAddDialog = false
                    goalToEdit = null
                },
                onConfirm = { name, amount ->
                    if (goalToEdit != null) {
                        viewModel.updateGoalDetails(goalToEdit!!, name, amount)
                    } else {
                        viewModel.addGoal(name, amount)
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
    goal: GoalEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddContribution: (Long) -> Unit
) {
    val progress = (goal.currentAmount.toFloat() / goal.targetAmount.toFloat()).coerceIn(0f, 1f)
    var showContributionDialog by remember { mutableStateOf(false) }

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
                Text(
                    goal.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
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
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    CurrencyFormatter.formatAmount(goal.currentAmount),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Target: ${CurrencyFormatter.formatAmount(goal.targetAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showContributionDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Contribution")
            }
        }
    }

    if (showContributionDialog) {
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showContributionDialog = false },
            title = { Text("Contribute to Goal") },
            text = {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() } },
                    label = { Text("Amount") },
                    visualTransformation = com.sans.finance.core.util.ThousandsSeparatorVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            },
            confirmButton = {
                Button(onClick = {
                    val contribution = amount.toLongOrNull() ?: 0L
                    onAddContribution(contribution * 100) // Assuming user inputs in whole currency
                    showContributionDialog = false
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showContributionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddGoalDialog(
    goalToEdit: GoalEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    val isEditing = goalToEdit != null
    var name by remember(goalToEdit) { mutableStateOf(goalToEdit?.name ?: "") }
    var amount by remember(goalToEdit) {
        mutableStateOf(goalToEdit?.targetAmount?.let { (it / 100).toString() } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Saving Goal" else "New Saving Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name (e.g. Travel)") }
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() } },
                    label = { Text("Target Amount") },
                    visualTransformation = com.sans.finance.core.util.ThousandsSeparatorVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val target = amount.toLongOrNull() ?: 0L
                onConfirm(name, target * 100)
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
