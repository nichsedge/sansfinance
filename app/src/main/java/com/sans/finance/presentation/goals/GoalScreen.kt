package com.sans.finance.presentation.goals

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.data.local.entity.GoalEntity
import com.sans.finance.presentation.components.GlassCard
import com.sans.finance.presentation.components.PrivacyText
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
                title = { 
                    Text(
                        "Savings Goals", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(paddingValues),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GoalsSummaryHeader(state)
            }

            if (state.goals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.TrackChanges,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "No savings goals set",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Track your progress towards big targets!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }

            items(state.goals) { goalWithProgress ->
                GoalItem(
                    goalWithProgress = goalWithProgress,
                    isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                    onEdit = { goalToEdit = goalWithProgress.goal },
                    onDelete = { viewModel.deleteGoal(goalWithProgress.goal) }
                )
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
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
fun GoalsSummaryHeader(state: GoalState) {
    val totalProgress = if (state.goals.isNotEmpty()) {
        state.goals.sumOf { (it.currentAmount / it.goal.targetAmount.toDouble()).coerceIn(0.0, 1.0) } / state.goals.size
    } else 0.0
    
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primary,
        alpha = 0.12f
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Average Goal Progress",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                "${(totalProgress * 100).toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = { totalProgress.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun GoalItem(
    goalWithProgress: GoalWithProgress,
    isPrivacyModeEnabled: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val goal = goalWithProgress.goal
    val progress =
        (goalWithProgress.currentAmount.toFloat() / goal.targetAmount.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = when (goal.targetType) {
                            "TOTAL" -> "TOTAL PORTFOLIO"
                            "CATEGORY" -> "CATEGORY: ${goal.targetName}"
                            "ASSET_CLASS" -> "ASSET CLASS: ${goal.targetName}"
                            else -> "PORTFOLIO"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
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
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), CircleShape)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.error.copy(alpha = 0.05f), CircleShape)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = if (progress >= 1f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CURRENT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PrivacyText(
                        amount = (goalWithProgress.currentAmount * 100).toLong(),
                        currencyCode = goal.currency,
                        isVisible = !isPrivacyModeEnabled,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Black,
                        color = if (progress >= 1f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TARGET", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PrivacyText(
                        amount = (goal.targetAmount * 100),
                        currencyCode = goal.currency,
                        isVisible = !isPrivacyModeEnabled,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            if (progress < 1f) {
                val remaining = (goal.targetAmount - goalWithProgress.currentAmount).coerceAtLeast(0.0)
                Text(
                    text = "${(progress * 100).toInt()}% completed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "Goal Achieved! 🎉",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 8.dp),
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        title = { Text(if (isEditing) "Edit Saving Goal" else "New Saving Goal", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name (e.g. Travel)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Target Amount") },
                    visualTransformation = com.sans.finance.core.util.ThousandsSeparatorVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )

                OutlinedTextField(
                    value = deadline?.let { SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(it)) } ?: "",
                    onValueChange = {},
                    label = { Text("Deadline (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    shape = MaterialTheme.shapes.large,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Select Date")
                        }
                    }
                )

                Text("Track Progress From:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

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
                            shape = MaterialTheme.shapes.large,
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
            }, shape = MaterialTheme.shapes.large) {
                Text(if (isEditing) "Save" else "Create", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}
