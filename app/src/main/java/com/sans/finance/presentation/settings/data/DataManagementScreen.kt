package com.sans.finance.presentation.settings.data

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val context = androidx.compose.ui.platform.LocalContext.current
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
                    },
                    onShareStatement = { viewModel.shareTransactionsStatementCsv(context) }
                )
            }

            item {
                DataSection(
                    title = "Assets & Portfolio",
                    description = "Import portfolio snapshots or export your complete assets (investments & account balances).",
                    icon = Icons.Default.PieChart,
                    onImport = {
                        viewModel.setImportType(ImportExportType.PORTFOLIO)
                        importLauncher.launch("*/*")
                    },
                    onExportCsv = {
                        viewModel.setExportType(ImportExportType.PORTFOLIO, ExportFormat.CSV)
                        exportLauncher.launch("assets_${System.currentTimeMillis()}.csv")
                    },
                    onExportJson = {
                        viewModel.setExportType(ImportExportType.PORTFOLIO, ExportFormat.JSON)
                        exportJsonLauncher.launch("assets_${System.currentTimeMillis()}.json")
                    }
                )
            }

            item {
                DataSection(
                    title = "App Settings",
                    description = "Backup your preferences, currency settings, and sync configuration.",
                    icon = Icons.Default.Settings,
                    onImport = {
                        viewModel.setImportType(ImportExportType.SETTINGS)
                        importLauncher.launch("*/*")
                    },
                    onExportCsv = null,
                    onExportJson = {
                        viewModel.setExportType(ImportExportType.SETTINGS, ExportFormat.JSON)
                        exportJsonLauncher.launch("settings_${System.currentTimeMillis()}.json")
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
                DatabaseMaintenanceCard(
                    isMaintaining = state.isMaintainingDb,
                    report = state.dbHealthReport,
                    dbVersion = state.dbVersion,
                    onRunMaintenance = { viewModel.runDatabaseMaintenance() }
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "About Formats",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
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
    onExportJson: (() -> Unit)? = null,
    onShareStatement: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
            if (onShareStatement != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onShareStatement,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share Multi-Currency Statement (CSV)")
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
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isStale) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isStale) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Automation status",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
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
                    color = MaterialTheme.colorScheme.error
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onHowToUpdate) { Text("How to update") }
            }
        }
    }
}

@Composable
fun DatabaseMaintenanceCard(
    isMaintaining: Boolean,
    report: com.sans.finance.domain.usecase.DatabaseHealthReport?,
    dbVersion: Int,
    onRunMaintenance: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Database Health & Optimization",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "Room v$dbVersion",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Text(
                "Runs SQLite VACUUM defragmentation, query analyzer optimization, removes orphaned cross-references, and verifies ledger consistency.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (report != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "✅ VACUUM & ANALYZE completed (${report.executionTimeMs} ms)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "🧹 Cleaned ${report.orphanedTagsCleaned} orphaned tag references",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "🔍 Audited ${report.totalTransactionsChecked} transactions across database",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "✨ Schema & Index Optimization: All ${report.analyzedTablesCount} tables analyzed (Schema v${report.dbVersion})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = onRunMaintenance,
                enabled = !isMaintaining,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isMaintaining) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Optimizing Database...")
                } else {
                    Text("Optimize & Audit Database")
                }
            }
        }
    }
}

