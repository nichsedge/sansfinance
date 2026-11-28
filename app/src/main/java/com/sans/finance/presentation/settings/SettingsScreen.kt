package com.sans.finance.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sans.finance.R
import com.sans.finance.domain.model.Category
import com.sans.finance.domain.model.Tag
import com.sans.finance.presentation.components.AppTopBar
import com.sans.finance.presentation.components.CategoryIcon
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: (() -> Unit)? = null,
    onNavigateToGoals: (() -> Unit)? = null,
    onNavigateToBudgets: (() -> Unit)? = null,
    onNavigateToCategories: () -> Unit,
    onNavigateToTags: () -> Unit,
    onNavigateToAccountTypes: () -> Unit,
    onNavigateToRecurringExpenses: (() -> Unit)? = null,
    onNavigateToDataManagement: () -> Unit,
    onNavigateToAiSettings: () -> Unit,
    onNavigateToReSyncDryRun: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentLanguage = viewModel.currentLanguage.value
    val snackbarHostState = remember { SnackbarHostState() }
    val currentBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()

    val isLoading by viewModel.isLoading
    val backupFrequency by viewModel.backupFrequency.collectAsStateWithLifecycle()
    val backupWifiOnly by viewModel.backupWifiOnly.collectAsStateWithLifecycle()
    val backupRequiresCharging by viewModel.backupRequiresCharging.collectAsStateWithLifecycle()
    val lastBackupTime by viewModel.lastBackupTime.collectAsStateWithLifecycle()
    val lastBackupSizeBytes by viewModel.lastBackupSizeBytes.collectAsStateWithLifecycle()
    val emailSyncEnabled by viewModel.emailSyncEnabled.collectAsStateWithLifecycle()
    val emailSyncHour by viewModel.emailSyncHour.collectAsStateWithLifecycle()
    val emailSyncMinute by viewModel.emailSyncMinute.collectAsStateWithLifecycle()
    val emailSyncWifiOnly by viewModel.emailSyncWifiOnly.collectAsStateWithLifecycle()
    val lastEmailSyncTime by viewModel.lastEmailSyncTime.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val listState = rememberLazyListState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var showAllCurrenciesDialog by remember { mutableStateOf(false) }
    var showBackupFrequencyDialog by remember { mutableStateOf(false) }
    var showSyncGatewayConfigDialog by remember { mutableStateOf(false) }
    var showRestoreCloudConfirmDialog by remember { mutableStateOf(false) }
    var showEmailSyncTimeDialog by remember { mutableStateOf(false) }
    var showEmailSyncGuideDialog by remember { mutableStateOf(false) }
    var showFxRatesDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.syncMessage.value) {
        viewModel.syncMessage.value?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(viewModel.error.value) {
        viewModel.error.value?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings),
                onBack = onBack
            )
        }
    ) { paddingValues ->
        SettingsContent(
            paddingValues = paddingValues,
            listState = listState,
            viewModel = viewModel,
            onNavigateToCategories = onNavigateToCategories,
            onNavigateToTags = onNavigateToTags,
            onNavigateToAccountTypes = onNavigateToAccountTypes,
            onNavigateToDataManagement = onNavigateToDataManagement,
            onNavigateToAiSettings = onNavigateToAiSettings,
            exportBackup = { viewModel.exportFullBackup(it) },
            onLanguageToggle = { showLanguageDialog = true },
            onCurrencyToggle = { showCurrencyDialog = true },
            currentLanguage = currentLanguage,
            currentCurrency = viewModel.currentCurrency.value,
            isLoading = isLoading,
            isPrivacyModeEnabled = viewModel.isPrivacyModeEnabled.value,
            onTogglePrivacyMode = { viewModel.togglePrivacyMode() },
            onReSync = onNavigateToReSyncDryRun,
            supportedLanguages = viewModel.supportedLanguages,
            commonCurrencies = viewModel.commonCurrencies,
            enabledCurrencies = viewModel.enabledCurrencies.value,
            onToggleEnabledCurrency = { viewModel.toggleEnabledCurrency(it) },
            onShowAllCurrencies = { showAllCurrenciesDialog = true },
            backupFrequency = backupFrequency,
            backupWifiOnly = backupWifiOnly,
            backupRequiresCharging = backupRequiresCharging,
            lastBackupTime = lastBackupTime,
            lastBackupSizeBytes = lastBackupSizeBytes,
            onFrequencyClick = { showBackupFrequencyDialog = true },
            onConfigureGatewayClick = { showSyncGatewayConfigDialog = true },
            onShowFxRates = { showFxRatesDialog = true },
            onWifiOnlyToggle = { viewModel.setBackupWifiOnly(it, context) },
            onRequiresChargingToggle = { viewModel.setBackupRequiresCharging(it, context) },
            onBackupNow = { viewModel.uploadBackupToCloud(context) },
            onRestoreFromCloud = { showRestoreCloudConfirmDialog = true },
            onSyncEmailInbox = { viewModel.syncEmailExpenses() },
            isSyncingInbox = viewModel.isSyncingInbox.value,
            emailSyncEnabled = emailSyncEnabled,
            emailSyncHour = emailSyncHour,
            emailSyncMinute = emailSyncMinute,
            emailSyncWifiOnly = emailSyncWifiOnly,
            lastEmailSyncTime = lastEmailSyncTime,
            onEmailSyncEnabledToggle = { viewModel.setEmailSyncEnabled(it, context) },
            onEmailSyncTimeClick = { showEmailSyncTimeDialog = true },
            onEmailSyncWifiOnlyToggle = { viewModel.setEmailSyncWifiOnly(it, context) },
            onEmailSyncGuideClick = { showEmailSyncGuideDialog = true }
        )
    }

    if (showEmailSyncGuideDialog) {
        AutoDetectExpensesGuideDialog(
            onDismiss = { showEmailSyncGuideDialog = false },
            onOpenGatewayConfig = {
                showEmailSyncGuideDialog = false
                showSyncGatewayConfigDialog = true
            }
        )
    }

    if (showEmailSyncTimeDialog) {
        EmailSyncTimeDialog(
            initialHour = emailSyncHour,
            initialMinute = emailSyncMinute,
            onDismiss = { showEmailSyncTimeDialog = false },
            onConfirm = { hour, minute ->
                viewModel.setEmailSyncTime(hour, minute, context)
                showEmailSyncTimeDialog = false
            }
        )
    }

    if (showBackupFrequencyDialog) {
        BackupFrequencyDialog(
            currentFrequency = backupFrequency,
            onDismiss = { showBackupFrequencyDialog = false },
            onSelect = { freq ->
                viewModel.setBackupFrequency(freq, context)
                showBackupFrequencyDialog = false
            }
        )
    }

    if (showSyncGatewayConfigDialog) {
        SyncGatewayConfigDialog(
            currentUrl = viewModel.getSyncGatewayUrl(),
            currentToken = viewModel.getSyncGatewayToken(),
            onDismiss = { showSyncGatewayConfigDialog = false },
            onSave = { url, token ->
                viewModel.saveSyncGatewayConfig(url, token)
                showSyncGatewayConfigDialog = false
            }
        )
    }

    if (showFxRatesDialog) {
        val rates by viewModel.exchangeRates.collectAsStateWithLifecycle()
        val baseCurrency = viewModel.currentCurrency.value
        val baseRate = rates.find { it.code == baseCurrency }?.rateToIdr ?: 1.0
        FxRatesDialog(
            baseCurrency = baseCurrency,
            baseRate = baseRate,
            rates = rates,
            onRefresh = { viewModel.refreshExchangeRates(context) },
            onDismiss = { showFxRatesDialog = false }
        )
    }

    if (showRestoreCloudConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreCloudConfirmDialog = false },
            title = { Text("Restore from Cloud Backup") },
            text = {
                Text(
                    "This will download the latest verified backup from Cloud Sync Gateway and replace your local database. The app will restart automatically after restore.\n\nAre you sure you want to proceed?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreCloudConfirmDialog = false
                        viewModel.restoreBackupFromCloud(context)
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Restore & Restart")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreCloudConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAllCurrenciesDialog) {
        AllCurrenciesDialog(
            allCurrencies = viewModel.allAvailableCurrencies,
            enabledCurrencies = viewModel.enabledCurrencies.value,
            onDismiss = { showAllCurrenciesDialog = false },
            onToggle = { viewModel.toggleEnabledCurrency(it) }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            supportedLanguages = viewModel.supportedLanguages,
            onDismiss = { showLanguageDialog = false },
            onSelect = { lang ->
                if (lang != currentLanguage) {
                    viewModel.setLanguage(lang)
                    showLanguageDialog = false
                    showRestartDialog = true
                } else {
                    showLanguageDialog = false
                }
            }
        )
    }

    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            currentCurrency = viewModel.currentCurrency.value,
            enabledCurrencies = viewModel.enabledCurrencies.value,
            onDismiss = { showCurrencyDialog = false },
            onSelect = { curr ->
                if (curr != viewModel.currentCurrency.value) {
                    viewModel.setCurrency(curr)
                    showCurrencyDialog = false
                    showRestartDialog = true
                } else {
                    showCurrencyDialog = false
                }
            }
        )
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text("Restart Required") },
            text = { Text("The app needs to restart to apply the changes correctly.") },
            confirmButton = {
                Button(onClick = { viewModel.restartApp(context) }) {
                    Text("Restart Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text("Later")
                }
            }
        )
    }

}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    paddingValues: PaddingValues,
    listState: androidx.compose.foundation.lazy.LazyListState,
    viewModel: SettingsViewModel,
    onNavigateToCategories: () -> Unit,
    onNavigateToTags: () -> Unit,
    onNavigateToAccountTypes: () -> Unit,
    onNavigateToDataManagement: () -> Unit,
    onNavigateToAiSettings: () -> Unit,
    exportBackup: (android.content.Context) -> Unit,
    onLanguageToggle: () -> Unit,
    onCurrencyToggle: () -> Unit,
    currentLanguage: String,
    currentCurrency: String,
    isLoading: Boolean,
    isPrivacyModeEnabled: Boolean,
    onTogglePrivacyMode: () -> Unit,
    onReSync: () -> Unit,
    supportedLanguages: List<Pair<String, String>>,
    commonCurrencies: List<String>,
    enabledCurrencies: List<String>,
    onToggleEnabledCurrency: (String) -> Unit,
    onShowAllCurrencies: () -> Unit,
    backupFrequency: String,
    backupWifiOnly: Boolean,
    backupRequiresCharging: Boolean,
    lastBackupTime: Long,
    lastBackupSizeBytes: Long,
    onFrequencyClick: () -> Unit,
    onConfigureGatewayClick: () -> Unit,
    onShowFxRates: () -> Unit,
    onWifiOnlyToggle: (Boolean) -> Unit,
    onRequiresChargingToggle: (Boolean) -> Unit,
    onBackupNow: () -> Unit,
    onRestoreFromCloud: () -> Unit,
    onSyncEmailInbox: () -> Unit = {},
    isSyncingInbox: Boolean = false,
    emailSyncEnabled: Boolean = true,
    emailSyncHour: Int = 20,
    emailSyncMinute: Int = 0,
    emailSyncWifiOnly: Boolean = false,
    lastEmailSyncTime: Long = 0L,
    onEmailSyncEnabledToggle: (Boolean) -> Unit = {},
    onEmailSyncTimeClick: () -> Unit = {},
    onEmailSyncWifiOnlyToggle: (Boolean) -> Unit = {},
    onEmailSyncGuideClick: () -> Unit = {}
) {
    val context = LocalContext.current

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. General (Language, Currency, FX, Privacy, AI)
        item {
            SettingsSectionTitle("General")
            val rates by viewModel.exchangeRates.collectAsStateWithLifecycle()
            val baseRate = rates.find { it.code == currentCurrency }?.rateToIdr ?: 1.0
            val usdRate = rates.find { it.code == "USD" }
            val fxSubtitle = if (usdRate != null && currentCurrency != "USD" && baseRate > 0) {
                val mult = usdRate.rateToIdr / baseRate
                val fmt = if (mult >= 100) String.format(java.util.Locale.US, "%,.0f", mult) else String.format(java.util.Locale.US, "%,.2f", mult)
                "1 USD = $fmt $currentCurrency · Tap for all"
            } else if (rates.isEmpty()) {
                "Tap refresh to sync live rates"
            } else {
                "${rates.size} currencies loaded · Tap to view"
            }

            val languageLabel = supportedLanguages.find { it.first == currentLanguage.take(2) }?.second
                ?: supportedLanguages.find { it.first == "en" }?.second
                ?: currentLanguage

            val currencyLabel = try {
                val curr = java.util.Currency.getInstance(currentCurrency)
                "${curr.displayName} (${curr.currencyCode})"
            } catch (e: Exception) {
                currentCurrency
            }

            SettingsGroupCard {
                SettingsRowItem(
                    icon = Icons.Default.Translate,
                    title = stringResource(R.string.language),
                    subtitle = languageLabel,
                    onClick = onLanguageToggle
                )
                SettingsRowDivider()
                SettingsRowItem(
                    icon = Icons.Default.ShoppingCart,
                    title = "Base Currency",
                    subtitle = currencyLabel,
                    onClick = onCurrencyToggle
                )
                SettingsRowDivider()
                SettingsRowItem(
                    icon = Icons.Default.Edit,
                    title = "Active Currencies",
                    subtitle = enabledCurrencies.joinToString(", ").ifBlank { "None" },
                    onClick = onShowAllCurrencies
                )
                SettingsRowDivider()
                SettingsRowItem(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    title = "Live Exchange Rates",
                    subtitle = fxSubtitle,
                    onClick = onShowFxRates,
                    trailingContent = {
                        IconButton(
                            onClick = { viewModel.refreshExchangeRates(context) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh Rates",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
                SettingsRowDivider()
                SettingsToggleRowItem(
                    title = "Privacy Mode",
                    subtitle = if (isPrivacyModeEnabled) "Hiding sensitive balances" else "Showing all balances",
                    checked = isPrivacyModeEnabled,
                    onCheckedChange = { onTogglePrivacyMode() }
                )
                SettingsRowDivider()
                SettingsRowItem(
                    icon = Icons.Default.SmartToy,
                    title = "AI Copilot (BYOK)",
                    subtitle = "API key, model & monthly insights",
                    onClick = onNavigateToAiSettings
                )
            }
        }

        // 2. Organization & Shortcuts
        item {
            SettingsSectionTitle("Organization & Shortcuts")
            SettingsGroupCard {
                SettingsRowItem(
                    icon = Icons.Default.Flag,
                    title = stringResource(R.string.categories),
                    subtitle = "Expense & income categories",
                    onClick = onNavigateToCategories
                )
                SettingsRowDivider()
                SettingsRowItem(
                    icon = Icons.Default.Edit,
                    title = stringResource(R.string.tags),
                    subtitle = "Custom tags & transaction labels",
                    onClick = onNavigateToTags
                )
                SettingsRowDivider()
                SettingsRowItem(
                    icon = Icons.Default.AccountBalance,
                    title = "Account Types",
                    subtitle = "Manage account groups & liability types",
                    onClick = onNavigateToAccountTypes
                )
                SettingsRowDivider()
                SettingsRowItem(
                    icon = Icons.Default.AddCircle,
                    title = "Pin Add Expense Shortcut",
                    subtitle = "1-tap shortcut on your home screen",
                    onClick = {
                        val pinned = com.sans.finance.presentation.util.DynamicShortcutManager.requestPinAddExpenseShortcut(context)
                        val msg = if (pinned) {
                            "Shortcut pinned to home screen"
                        } else {
                            "Long-press Sans Finance icon on home screen to drag 'Add Expense'"
                        }
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                    }
                )
            }
        }

        // 3. Cloud Backup
        item {
            SettingsSectionTitle("Cloud Backup")
            CloudBackupSettingsCard(
                frequency = backupFrequency,
                wifiOnly = backupWifiOnly,
                requiresCharging = backupRequiresCharging,
                lastBackupTime = lastBackupTime,
                lastBackupSizeBytes = lastBackupSizeBytes,
                isLoading = isLoading,
                onFrequencyClick = onFrequencyClick,
                onConfigureGatewayClick = onConfigureGatewayClick,
                onWifiOnlyToggle = onWifiOnlyToggle,
                onRequiresChargingToggle = onRequiresChargingToggle,
                onBackupNow = onBackupNow,
                onRestoreFromCloud = onRestoreFromCloud
            )
        }

        // 4. Auto-Detect Expenses
        item {
            SettingsSectionTitle("Auto-Detect Expenses")
            EmailExpensesSettingsCard(
                onSyncEmailInbox = onSyncEmailInbox,
                isSyncingInbox = isSyncingInbox,
                emailSyncEnabled = emailSyncEnabled,
                emailSyncHour = emailSyncHour,
                emailSyncMinute = emailSyncMinute,
                emailSyncWifiOnly = emailSyncWifiOnly,
                lastEmailSyncTime = lastEmailSyncTime,
                onEmailSyncEnabledToggle = onEmailSyncEnabledToggle,
                onEmailSyncTimeClick = onEmailSyncTimeClick,
                onEmailSyncWifiOnlyToggle = onEmailSyncWifiOnlyToggle,
                onGuideClick = onEmailSyncGuideClick
            )
        }

        // 4. Data & Maintenance
        item {
            SettingsSectionTitle("Data & Maintenance")
            SettingsGroupCard {
                SettingsRowItem(
                    icon = Icons.Default.FileUpload,
                    title = "Import & Export (CSV / JSON)",
                    subtitle = "Transactions, assets & portfolio, settings",
                    onClick = onNavigateToDataManagement
                )
                SettingsRowDivider()
                SettingsRowItem(
                    icon = Icons.Default.Sync,
                    title = stringResource(R.string.full_backup),
                    subtitle = stringResource(R.string.backup_to_downloads),
                    isLoading = isLoading,
                    onClick = { exportBackup(context) }
                )
                SettingsRowDivider()
                SettingsRowItem(
                    icon = Icons.Default.Refresh,
                    title = "Re-sync Account Balances",
                    subtitle = "Recalculate from transaction history",
                    onClick = onReSync
                )
                SettingsRowDivider()
                SettingsRowItem(
                    icon = Icons.Default.Delete,
                    title = "Clean Orphaned Tags",
                    subtitle = "Remove tags not linked to any expense",
                    onClick = { viewModel.cleanTags() }
                )
            }
        }
    }
}
