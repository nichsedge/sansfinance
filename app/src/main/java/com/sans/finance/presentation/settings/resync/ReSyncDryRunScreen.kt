package com.sans.finance.presentation.settings.resync

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.core.util.DateFormatterUtils
import com.sans.finance.domain.model.AccountSyncDryRunResult
import com.sans.finance.presentation.components.PrivacyText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReSyncDryRunScreen(
    onBack: () -> Unit,
    viewModel: ReSyncDryRunViewModel = hiltViewModel()
) {
    val haptic = LocalHapticFeedback.current
    val isLoading by viewModel.isLoading
    val dryRunResults by viewModel.dryRunResults
    val error by viewModel.error
    val successMessage by viewModel.successMessage
    val isPrivacyMode by viewModel.isPrivacyModeEnabled.collectAsStateWithLifecycle()
    val currentCurrency = viewModel.currentCurrency

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(successMessage) {
        successMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                delay(1200)
                onBack()
            }
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearMessages()
        }
    }

    val differencesCount = remember(dryRunResults) {
        dryRunResults.count { it.isDifferenceExist }
    }
    val inSyncCount = remember(dryRunResults) {
        dryRunResults.count { !it.isDifferenceExist }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Re-sync Dry Run",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadDryRunData() },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        val selectedMode by viewModel.selectedMode
        val selectedDateOption by viewModel.selectedDateOption
        val customDate by viewModel.customDate

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        // Info Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Choose your source of truth. You can either fix account balances to match your transaction history, or keep balances and add automatic adjustments.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    item {
                        // Mode Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val isFixMode = selectedMode == com.sans.finance.domain.model.ReSyncMode.TRANSACTIONS_AS_TRUTH
                            val isKeepMode = selectedMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH

                            Card(
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.setSyncMode(com.sans.finance.domain.model.ReSyncMode.TRANSACTIONS_AS_TRUTH) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isFixMode) {
                                        Color(0xFFF43F5E).copy(alpha = 0.16f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    }
                                ),
                                border = if (isFixMode) {
                                    BorderStroke(2.dp, Color(0xFFF43F5E))
                                } else {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                },
                                shape = MaterialTheme.shapes.large
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Fix Balances",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFixMode) Color(0xFFF43F5E) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Adjust accounts to match transaction sum",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isFixMode) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        minLines = 2
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.setSyncMode(com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isKeepMode) {
                                        Color(0xFF10B981).copy(alpha = 0.16f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    }
                                ),
                                border = if (isKeepMode) {
                                    BorderStroke(2.dp, Color(0xFF10B981))
                                } else {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                },
                                shape = MaterialTheme.shapes.large
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Keep Balances",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isKeepMode) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Add adjustments to match account balances",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isKeepMode) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        minLines = 2
                                    )
                                }
                            }
                        }
                    }

                    if (selectedMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH) {
                        item {
                            // Date Picker row
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Adjustment Date Option",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("TODAY" to "Today", "EPOCH" to "Epoch (1970)", "CUSTOM" to "Custom...").forEach { (opt, label) ->
                                            val isSelected = selectedDateOption == opt
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { viewModel.setDateOption(opt) },
                                                label = { Text(label) }
                                            )
                                        }
                                    }

                                    if (selectedDateOption == "CUSTOM") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Selected Date: " + DateFormatterUtils.getStandardFormatter().format(Date(customDate)),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            var showDatePicker by remember { mutableStateOf(false) }
                                            OutlinedButton(
                                                onClick = { showDatePicker = true },
                                                shape = MaterialTheme.shapes.medium
                                            ) {
                                                Text("Select Date", style = MaterialTheme.typography.bodySmall)
                                            }

                                            if (showDatePicker) {
                                                val datePickerState = rememberDatePickerState(
                                                    initialSelectedDateMillis = customDate
                                                )
                                                DatePickerDialog(
                                                    onDismissRequest = { showDatePicker = false },
                                                    confirmButton = {
                                                        TextButton(
                                                            onClick = {
                                                                datePickerState.selectedDateMillis?.let {
                                                                    viewModel.setCustomDate(it)
                                                                }
                                                                showDatePicker = false
                                                            }
                                                        ) {
                                                            Text("OK")
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
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // Summary Stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (differencesCount > 0) {
                                        if (selectedMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        } else {
                                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                        }
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    }
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (selectedMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH) "Need Adjustment" else "Out of Sync",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (differencesCount > 0) {
                                            if (selectedMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        } else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$differencesCount Accounts",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (differencesCount > 0) {
                                            if (selectedMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        } else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "In Sync",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Text(
                                        text = "$inSyncCount Accounts",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }

                    if (isLoading && dryRunResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (dryRunResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No accounts found.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(dryRunResults, key = { it.accountId }) { result ->
                            AccountDryRunItem(
                                result = result,
                                isPrivacyMode = isPrivacyMode,
                                syncMode = selectedMode
                            )
                        }
                    }
                }

                // Bottom Buttons Sticky Footer
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onBack()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Cancel",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val isBalanceMode = selectedMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH
                        val actionColor = if (isBalanceMode) Color(0xFF10B981) else Color(0xFFF43F5E)

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.applyReSync()
                            },
                            enabled = differencesCount > 0 && !isLoading,
                            modifier = Modifier
                                .weight(1.75f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 0.dp
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = actionColor,
                                contentColor = Color.White,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp,
                                    color = Color.White
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (differencesCount == 0) {
                                            Icons.Default.CheckCircle
                                        } else if (isBalanceMode) {
                                            Icons.Default.Check
                                        } else {
                                            Icons.Default.Sync
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (differencesCount > 0) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (differencesCount == 0) {
                                            "All in Sync"
                                        } else if (isBalanceMode) {
                                            "Apply Adjustments"
                                        } else {
                                            "Apply Re-sync"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        letterSpacing = 0.2.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountDryRunItem(
    result: AccountSyncDryRunResult,
    isPrivacyMode: Boolean,
    syncMode: com.sans.finance.domain.model.ReSyncMode
) {
    val needsSync = result.isDifferenceExist

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (needsSync) {
                if (syncMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                }
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        border = if (needsSync) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                if (syncMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                }
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (needsSync) {
                            if (syncMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            } else {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                            }
                        } else {
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                        },
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (needsSync) {
                        if (syncMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH) {
                            Icons.Default.Info
                        } else {
                            Icons.Default.Warning
                        }
                    } else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (needsSync) {
                        if (syncMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    } else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Name & Sync state text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = result.accountName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = if (needsSync) {
                            if (syncMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH) "Needs Adjustment" else "Needs Sync"
                        } else "In Sync",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (needsSync) {
                            if (syncMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        } else MaterialTheme.colorScheme.tertiary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Balances Comparison Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: current vs calculated
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Current: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            PrivacyText(
                                amount = result.currentBalance,
                                currencyCode = result.currency,
                                isVisible = !isPrivacyMode,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (syncMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (syncMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Calculated: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            PrivacyText(
                                amount = result.calculatedBalance,
                                currencyCode = result.currency,
                                isVisible = !isPrivacyMode,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (syncMode == com.sans.finance.domain.model.ReSyncMode.TRANSACTIONS_AS_TRUTH) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (syncMode == com.sans.finance.domain.model.ReSyncMode.TRANSACTIONS_AS_TRUTH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Right: Delta
                    Box(
                        modifier = Modifier
                            .background(
                                color = when {
                                    result.delta > 0L -> if (syncMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH) Color(0xFFF43F5E).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f) // Red adjustment (Expense) / Green sync
                                    result.delta < 0L -> if (syncMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF43F5E).copy(alpha = 0.15f) // Green adjustment (Income) / Red sync
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                },
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        val deltaText = when {
                            result.delta > 0L -> {
                                if (syncMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH) {
                                    "Adjustment: -" + if (isPrivacyMode) "••••" else CurrencyFormatter.formatAmount(result.delta, result.currency) + " (Expense)"
                                } else {
                                    "+" + if (isPrivacyMode) "••••" else CurrencyFormatter.formatAmount(result.delta, result.currency)
                                }
                            }
                            result.delta < 0L -> {
                                if (syncMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH) {
                                    "Adjustment: +" + if (isPrivacyMode) "••••" else CurrencyFormatter.formatAmount(-result.delta, result.currency) + " (Income)"
                                } else {
                                    if (isPrivacyMode) "••••" else CurrencyFormatter.formatAmount(result.delta, result.currency)
                                }
                            }
                            else -> "No Change"
                        }
                        Text(
                            text = deltaText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                result.delta > 0L -> if (syncMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH) Color(0xFFF43F5E) else Color(0xFF10B981)
                                result.delta < 0L -> if (syncMode == com.sans.finance.domain.model.ReSyncMode.BALANCE_AS_TRUTH) Color(0xFF10B981) else Color(0xFFF43F5E)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}
