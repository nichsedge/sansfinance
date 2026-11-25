package com.sans.finance.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sans.finance.data.local.entity.ExchangeRateEntity
import java.util.Locale

@Composable
internal fun AutoDetectExpensesGuideDialog(
    onDismiss: () -> Unit,
    onOpenGatewayConfig: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val repoUrl = "https://github.com/nichsedge/ierp"
    val readmeUrl = "https://github.com/nichsedge/ierp/blob/main/scripts/gmail_expenses/README.md"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Auto-Detect Expenses Setup", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "For power users: automatically import bank alerts and e-receipts from Gmail via Google Cloud Pub/Sub.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Architecture",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Gmail → Cloud Pub/Sub → Cloud Run Worker (nichsedge/ierp) → Turso Inbox → Sans Finance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "How to Connect",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "1. Deploy the Gmail Pub/Sub worker from nichsedge/ierp (scripts/gmail_expenses) to Cloud Run.\n" +
                        "2. Set your Gateway URL and Sync Auth Token in Cloud Sync Gateway.\n" +
                        "3. Enable Auto-Sync Daily (runs once daily when internet is connected, 15-min deduplication).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { uriHandler.openUri(readmeUrl) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("Worker README", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { uriHandler.openUri(repoUrl) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("GitHub Repo", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onOpenGatewayConfig) {
                Text("Gateway Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EmailSyncTimeDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily Email Sync Time", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Choose when to automatically check for new email expenses (only runs when internet is available).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun FxRatesDialog(
    baseCurrency: String,
    baseRate: Double,
    rates: List<ExchangeRateEntity>,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val displayCurrencies = listOf("USD", "SGD", "EUR", "JPY", "CNY", "GBP", "IDR").filter { it != baseCurrency }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Live Exchange Rates", fontWeight = FontWeight.Bold) },
        text = {
            if (rates.isEmpty()) {
                Text(
                    "No exchange rates loaded yet. Tap Refresh to sync live rates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayCurrencies.forEach { code ->
                        val r = rates.find { it.code == code }
                        if (r != null) {
                            val multiplier = if (baseRate > 0) r.rateToIdr / baseRate else 1.0
                            val fmt = if (multiplier >= 100) {
                                String.format(Locale.US, "%,.0f", multiplier)
                            } else {
                                String.format(Locale.US, "%,.2f", multiplier)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("1 $code", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("$fmt $baseCurrency", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onRefresh) {
                Text("Refresh")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
internal fun SyncGatewayConfigDialog(
    currentUrl: String,
    currentToken: String,
    onDismiss: () -> Unit,
    onSave: (url: String, token: String) -> Unit
) {
    var url by remember { mutableStateOf(if (currentUrl.isBlank()) "https://gmail-expenses-worker-886416340889.us-central1.run.app" else currentUrl) }
    var token by remember { mutableStateOf(currentToken) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cloud Sync Gateway Configuration", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Gunakan endpoint Cloud Run pribadi untuk sinkronisasi Gmail Inbox dan R2 Backup tanpa perlu master S3 credentials.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Gateway URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Sync Auth Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(url.trim(), token.trim()) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun BackupFrequencyDialog(
    currentFrequency: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val options = listOf(
        "OFF" to "Never / Off",
        "MANUAL" to "Only when I tap \"Back up\"",
        "DAILY" to "Daily",
        "WEEKLY" to "Weekly (Recommended)",
        "MONTHLY" to "Monthly"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Auto-Backup Schedule", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(key) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (key == currentFrequency),
                            onClick = { onSelect(key) }
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (key == currentFrequency) FontWeight.Bold else FontWeight.Normal,
                            color = if (key == currentFrequency) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
