package com.sans.finance.presentation.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Translate

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.material3.RadioButton
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.R
import com.sans.finance.domain.model.Category
import com.sans.finance.domain.model.Tag
import com.sans.finance.presentation.components.CategoryIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: (() -> Unit)? = null,
    onNavigateToGoals: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToTags: () -> Unit,
    onNavigateToRecurringExpenses: () -> Unit,
    onNavigateToDataManagement: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentLanguage = viewModel.currentLanguage.value
    val snackbarHostState = remember { SnackbarHostState() }
    val currentBudget by viewModel.monthlyBudget.collectAsState()

    val isLoading by viewModel.isLoading

    val context = LocalContext.current

    val listState = rememberLazyListState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var showSyncConfirmation by remember { mutableStateOf(false) }
    var showAllCurrenciesDialog by remember { mutableStateOf(false) }

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
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        SettingsContent(
            paddingValues = paddingValues,
            listState = listState,
            viewModel = viewModel,
            onGoals = onNavigateToGoals,
            onBudget = onNavigateToBudgets,
            onNavigateToCategories = onNavigateToCategories,
            onNavigateToTags = onNavigateToTags,
            onNavigateToRecurringExpenses = onNavigateToRecurringExpenses,
            onNavigateToDataManagement = onNavigateToDataManagement,
            exportBackup = { viewModel.exportFullBackup(it) },
            onLanguageToggle = { showLanguageDialog = true },
            onCurrencyToggle = { showCurrencyDialog = true },
            currentLanguage = currentLanguage,
            currentCurrency = viewModel.currentCurrency.value,
            currentBudget = currentBudget,
            isLoading = isLoading,
            isPrivacyModeEnabled = viewModel.isPrivacyModeEnabled.value,
            onTogglePrivacyMode = { viewModel.togglePrivacyMode() },
            onReSync = { showSyncConfirmation = true },
            supportedLanguages = viewModel.supportedLanguages,
            commonCurrencies = viewModel.commonCurrencies,
            enabledCurrencies = viewModel.enabledCurrencies.value,
            onToggleEnabledCurrency = { viewModel.toggleEnabledCurrency(it) },
            onShowAllCurrencies = { showAllCurrenciesDialog = true }
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

    if (showSyncConfirmation) {
        AlertDialog(
            onDismissRequest = { showSyncConfirmation = false },
            title = { Text("Re-sync Balances?") },
            text = { Text("This will recalculate all account balances from your transaction history. Manual adjustments may be overwritten. Continue?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reSyncBalances()
                        showSyncConfirmation = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Re-sync")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncConfirmation = false }) {
                    Text("Cancel")
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
    onGoals: () -> Unit,
    onBudget: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToTags: () -> Unit,
    onNavigateToRecurringExpenses: () -> Unit,
    onNavigateToDataManagement: () -> Unit,
    exportBackup: (android.content.Context) -> Unit,
    onLanguageToggle: () -> Unit,
    onCurrencyToggle: () -> Unit,
    currentLanguage: String,
    currentCurrency: String,
    currentBudget: Long,
    isLoading: Boolean,
    isPrivacyModeEnabled: Boolean,
    onTogglePrivacyMode: () -> Unit,
    onReSync: () -> Unit,
    supportedLanguages: List<Pair<String, String>>,
    commonCurrencies: List<String>,
    enabledCurrencies: List<String>,
    onToggleEnabledCurrency: (String) -> Unit,
    onShowAllCurrencies: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Language Section
        item {
            SettingsSectionTitle(stringResource(R.string.language))
            SettingsClickableCard(
                onClick = onLanguageToggle,
                icon = Icons.Default.Translate,
                title = supportedLanguages.find { it.first == currentLanguage.take(2) }?.second
                    ?: supportedLanguages.find { it.first == "en" }?.second
                    ?: currentLanguage
            )
        }

        // Currency Section
        item {
            SettingsSectionTitle("Currency")
            SettingsClickableCard(
                onClick = onCurrencyToggle,
                icon = Icons.Default.ShoppingCart,
                title = try {
                    val curr = java.util.Currency.getInstance(currentCurrency)
                    "${curr.displayName} (${curr.currencyCode})"
                } catch (e: Exception) {
                    currentCurrency
                }
            )

            Spacer(Modifier.height(4.dp))
            Text(
                "Quick Selection Options",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 8.dp)
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                commonCurrencies.forEach { curr ->
                    val isEnabled = enabledCurrencies.contains(curr)
                    FilterChip(
                        selected = isEnabled,
                        onClick = { onToggleEnabledCurrency(curr) },
                        label = { Text(curr, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (isEnabled) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null
                    )
                }

                FilterChip(
                    selected = false,
                    onClick = onShowAllCurrencies,
                    label = { Text("More...", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
        }

        item {
            SettingsSectionTitle("Features")
            SettingsClickableCard(
                onClick = onGoals,
                icon = Icons.Default.Flag,
                title = "Savings Goals",
                subtitle = "Track your financial targets"
            )
        }

        item {
            SettingsClickableCard(
                onClick = onBudget,
                icon = Icons.Default.ShoppingCart,
                title = "Monthly Budget",
                subtitle = if (currentBudget > 0L) {
                    com.sans.finance.core.util.CurrencyFormatter.formatAmount(currentBudget, currentCurrency)
                } else "Not Set"
            )
        }

        item {
            SettingsClickableCard(
                onClick = onNavigateToRecurringExpenses,
                icon = Icons.Default.Sync,
                title = stringResource(R.string.recurring_expenses),
                subtitle = "Manage subscriptions and fixed costs"
            )
        }

        item {
            SettingsClickableCard(
                onClick = onNavigateToCategories,
                icon = Icons.Default.ChevronRight,
                title = stringResource(R.string.categories)
            )
        }

        item {
            SettingsClickableCard(
                onClick = onNavigateToTags,
                icon = Icons.Default.ChevronRight,
                title = stringResource(R.string.tags)
            )
        }

        // Security Section
        item {
            SettingsSectionTitle("Security")
            SettingsToggleCard(
                checked = isPrivacyModeEnabled,
                onCheckedChange = { onTogglePrivacyMode() },
                title = "Privacy Mode",
                subtitle = if (isPrivacyModeEnabled) "Hiding sensitive balances" else "Showing all balances"
            )
        }

        // Data Management Section
        item {
            SettingsSectionTitle(stringResource(R.string.data_management))
            SettingsClickableCard(
                onClick = onNavigateToDataManagement,
                icon = Icons.Default.FileUpload,
                title = "Import & Export",
                subtitle = "CSV and JSON data handling"
            )
        }

        item {
            SettingsClickableCard(
                onClick = { exportBackup(context) },
                icon = Icons.Default.Sync,
                title = stringResource(R.string.full_backup),
                subtitle = stringResource(R.string.backup_to_downloads),
                isLoading = isLoading
            )
        }

        // Maintenance Section
        item {
            SettingsSectionTitle("Maintenance")
            SettingsClickableCard(
                onClick = { viewModel.cleanTags() },
                icon = Icons.Default.Delete,
                title = "Clean Orphaned Tags",
                subtitle = "Remove tags not linked to any expense"
            )
        }

        item {
            SettingsClickableCard(
                onClick = onReSync,
                icon = Icons.Default.Sync,
                title = "Re-sync Account Balances",
                subtitle = "Recalculate from history (May overwrite)"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsClickableCard(
    onClick: () -> Unit,
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    isLoading: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun SettingsToggleCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    subtitle: String? = null,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }
            androidx.compose.material3.Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.scale(0.8f)
            )
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    icon: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(
                icon = icon,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)

            if (onMoveUp != null) {
                IconButton(onClick = onMoveUp) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Move Up",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (onMoveDown != null) {
                IconButton(onClick = onMoveDown) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = "Move Down",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun CategoryEditDialog(
    category: Category? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember(category?.id) { mutableStateOf(category?.name ?: "") }
    var icon by remember(category?.id) { mutableStateOf(category?.icon ?: "📁") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Add Category" else "Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Icon", style = MaterialTheme.typography.labelMedium)

                    val presets =
                        listOf("🍔", "💊", "🛍️", "🚗", "🌐", "📁", "🏠", "🎮", "🎁", "💡", "💰", "🔧")

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { emoji ->
                            val isSelected = icon == emoji
                            Surface(
                                onClick = { icon = emoji },
                                shape = MaterialTheme.shapes.small,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = icon,
                        onValueChange = { if (it.length <= 2) icon = it },
                        label = { Text("Custom Emoji") },
                        singleLine = true,
                        modifier = Modifier.width(120.dp),
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, icon) }) {
                Text("Confirm")
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
fun TagEditDialog(
    tag: Tag,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(tag.id) { mutableStateOf(tag.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Tag") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tag Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) {
                Text("Confirm")
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
fun LanguageSelectionDialog(
    currentLanguage: String,
    supportedLanguages: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language)) },
        text = {
            Column {
                supportedLanguages.forEach { (code, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(code) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage.startsWith(code),
                            onClick = { onSelect(code) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(name, style = MaterialTheme.typography.bodyLarge)
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

@Composable
fun CurrencySelectionDialog(
    currentCurrency: String,
    enabledCurrencies: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Currency") },
        text = {
            LazyColumn(modifier = Modifier.height(300.dp)) {
                items(enabledCurrencies.size) { index ->
                    val currency = enabledCurrencies[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(currency) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currency == currentCurrency,
                            onClick = { onSelect(currency) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = try {
                                    java.util.Currency.getInstance(currency).displayName
                                } catch (e: Exception) {
                                    currency
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = currency,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCurrenciesDialog(
    allCurrencies: List<String>,
    enabledCurrencies: List<String>,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCurrencies = remember(searchQuery) {
        allCurrencies.filter {
            it.contains(searchQuery, ignoreCase = true) ||
                    try {
                        java.util.Currency.getInstance(it).displayName.contains(
                            searchQuery,
                            ignoreCase = true
                        )
                    } catch (e: Exception) {
                        false
                    }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable Currencies") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Currency") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(filteredCurrencies.size) { index ->
                        val currency = filteredCurrencies[index]
                        val isEnabled = enabledCurrencies.contains(currency)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(currency) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Checkbox(
                                checked = isEnabled,
                                onCheckedChange = { onToggle(currency) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = try {
                                        java.util.Currency.getInstance(currency).displayName
                                    } catch (e: Exception) {
                                        currency
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = currency,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
