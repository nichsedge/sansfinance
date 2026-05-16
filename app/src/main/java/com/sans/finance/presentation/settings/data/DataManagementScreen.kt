package com.sans.finance.presentation.settings.data

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    onBack: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAutomationHelp by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onImportFileSelected(it) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.onExportFileSelected(it) }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.onExportFileSelected(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Import & Export", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                DataSection(
                    title = "Transactions",
                    description = "Import or export your daily expenses and income.",
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    onImport = {
                        viewModel.setImportType(ImportExportType.TRANSACTIONS)
                        importLauncher.launch("*/*")
                    },
                    onExportCsv = {
                        viewModel.setExportType(ImportExportType.TRANSACTIONS, ExportFormat.CSV)
                        exportLauncher.launch("transactions_${System.currentTimeMillis()}.csv")
                    },
                    onExportJson = {
                        viewModel.setExportType(ImportExportType.TRANSACTIONS, ExportFormat.JSON)
                        exportJsonLauncher.launch("transactions_${System.currentTimeMillis()}.json")
                    }
                )
            }

            item {
                DataSection(
                    title = "Portfolio",
                    description = "Manage your investment snapshots and asset holdings.",
                    icon = Icons.Default.PieChart,
                    onImport = {
                        viewModel.setImportType(ImportExportType.PORTFOLIO)
                        importLauncher.launch("*/*")
                    },
                    onExportCsv = {
                        viewModel.setExportType(ImportExportType.PORTFOLIO, ExportFormat.CSV)
                        exportLauncher.launch("portfolio_${System.currentTimeMillis()}.csv")
                    },
                    onExportJson = {
                        viewModel.setExportType(ImportExportType.PORTFOLIO, ExportFormat.JSON)
                        exportJsonLauncher.launch("portfolio_${System.currentTimeMillis()}.json")
                    }
                )
            }

            item {
                PortfolioAutomationCard(
                    snapshotDate = state.latestPortfolioSnapshotDate,
                    holdingsCount = state.latestPortfolioHoldingsCount,
                    sources = state.latestPortfolioSources,
                    isStale = state.isPortfolioStale,
                    onHowToUpdate = { showAutomationHelp = true }
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "About Formats",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "CSV is best for spreadsheets (Excel, Google Sheets). JSON is recommended for backups or transferring data between devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (state.isLoading) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Processing...") },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        )
    }

    if (showAutomationHelp) {
        AlertDialog(
            onDismissRequest = { showAutomationHelp = false },
            confirmButton = {
                TextButton(onClick = { showAutomationHelp = false }) { Text("OK") }
            },
            title = { Text("Portfolio Automation (Your Setup)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Your fastest loop is to backfill snapshots directly into the on-device DB, then just open the app.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "From your computer (repo root):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "make backfill-portfolio",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text(
                        "If portfolio sources are missing/stale, fix the upstream pipeline (KSEI/Binance/wallets/manual CSV) and rerun.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

@Composable
fun DataSection(
    title: String,
    description: String,
    icon: ImageVector,
    onImport: () -> Unit,
    onExportCsv: (() -> Unit)? = null,
    onExportJson: (() -> Unit)? = null
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onImport,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Import")
            }

            if (onExportCsv != null) {
                OutlinedButton(
                    onClick = onExportCsv,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("CSV")
                }
            }

            if (onExportJson != null) {
                OutlinedButton(
                    onClick = onExportJson,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("JSON")
                }
            }
        }
    }
}

@Composable
private fun PortfolioAutomationCard(
    snapshotDate: Long?,
    holdingsCount: Int,
    sources: List<Pair<String, Int>>,
    isStale: Boolean,
    onHowToUpdate: () -> Unit
) {
    val dateText = snapshotDate?.let {
        val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        df.format(Date(it))
    } ?: "No snapshot yet"

    val sourcesText = if (sources.isEmpty()) {
        "—"
    } else {
        sources.take(4).joinToString(" • ") { (src, count) -> "$src ($count)" }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isStale) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Automation status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "Latest snapshot: $dateText • $holdingsCount holdings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Sources: $sourcesText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isStale) {
                Text(
                    "Snapshot looks older than a month. Run your backfill pipeline to refresh.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onHowToUpdate) { Text("How to update") }
            }
        }
    }
}
