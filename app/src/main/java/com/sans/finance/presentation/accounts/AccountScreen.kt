package com.sans.finance.presentation.accounts

import com.sans.finance.core.util.groupByParent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.presentation.components.AppTopBar
import com.sans.finance.presentation.components.GlassCard
import com.sans.finance.presentation.components.PrivacyText

data class AccountUpdateParams(
    val account: AccountEntity,
    val name: String,
    val type: String,
    val balance: Long,
    val currency: String,
    val interestRate: Double,
    val minPayment: Long,
    val parentAccountId: Long? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onStatsClick: () -> Unit,
    onDebtStrategistClick: () -> Unit = {},
    onBack: () -> Unit = {},
    onViewLedger: (Long) -> Unit = {},
    viewModel: AccountViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var preselectedParentForAdd by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var selectedAccountForAction by remember { mutableStateOf<AccountEntity?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showAdjustmentDialog by remember { mutableStateOf(false) }
    var pendingUpdateData by remember { mutableStateOf<AccountUpdateParams?>(null) }
    var isReorderMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Accounts",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Account Statistics") },
                            onClick = {
                                showMenu = false
                                onStatsClick()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Insights,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Debt Strategist") },
                            onClick = {
                                showMenu = false
                                onDebtStrategistClick()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Payments,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isReorderMode) "Finish Reordering" else "Reorder Accounts") },
                            onClick = {
                                isReorderMode = !isReorderMode
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    if (isReorderMode) Icons.Default.Check else Icons.Default.SwapVert,
                                    contentDescription = null
                                )
                            }
                        )
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
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
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
                AccountHeaderStats(state)
            }

            val displayedTypeNames = state.accountTypes.map { it.name }.toSet()

            // 1. Show accounts for defined types in their display order
            state.accountTypes.forEach { accountType ->
                val accounts = state.accountsByType[accountType.name] ?: emptyList()
                if (accounts.isNotEmpty()) {
                    item {
                        AccountGroupCard(
                            type = accountType.name,
                            accounts = accounts,
                            currentCurrency = state.currentCurrency,
                            ratesMap = state.ratesMap,
                            isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                            onAccountClick = { selectedAccountForAction = it },
                            onMoveAccountUp = viewModel::moveAccountUp,
                            onMoveAccountDown = viewModel::moveAccountDown,
                            isReorderMode = isReorderMode,
                            accountTypes = state.accountTypes
                        )
                    }
                }
            }

            // 2. Show accounts for types that are NOT defined in settings (e.g. legacy types)
            state.accountsByType.forEach { (type, accounts) ->
                if (type !in displayedTypeNames) {
                    item {
                        AccountGroupCard(
                            type = type,
                            accounts = accounts,
                            currentCurrency = state.currentCurrency,
                            ratesMap = state.ratesMap,
                            isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                            onAccountClick = { selectedAccountForAction = it },
                            onMoveAccountUp = viewModel::moveAccountUp,
                            onMoveAccountDown = viewModel::moveAccountDown,
                            isReorderMode = isReorderMode,
                            accountTypes = state.accountTypes
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        if (showAddDialog || accountToEdit != null) {
            val isEditing = accountToEdit != null
            val allAccounts = state.accountsByType.values.flatten()
            val hasChildren = isEditing && allAccounts.any { it.parentAccountId == accountToEdit?.id }
            var name by remember(accountToEdit, preselectedParentForAdd) { mutableStateOf(accountToEdit?.name ?: "") }
            var type by remember(accountToEdit, preselectedParentForAdd, state.accountTypes) {
                mutableStateOf(
                    accountToEdit?.type
                        ?: preselectedParentForAdd?.type
                        ?: state.accountTypes.firstOrNull()?.name
                        ?: ""
                )
            }
            var parentAccountId by remember(accountToEdit, preselectedParentForAdd) {
                mutableStateOf(accountToEdit?.parentAccountId ?: preselectedParentForAdd?.id)
            }
            var currency by remember(accountToEdit, state.currentCurrency) {
                mutableStateOf(accountToEdit?.currency ?: state.currentCurrency)
            }
            var balance by remember(accountToEdit) {
                mutableStateOf(
                    accountToEdit?.balance?.let { cents ->
                        if (cents % 100 == 0L) (cents / 100).toString()
                        else String.format(java.util.Locale.US, "%.2f", cents / 100.0).trimEnd('0').trimEnd('.')
                    } ?: ""
                )
            }
            var interestRate by remember(accountToEdit) {
                mutableStateOf(accountToEdit?.interestRate?.toString() ?: "0.0")
            }
            var minPayment by remember(accountToEdit) {
                mutableStateOf(
                    accountToEdit?.minPayment?.let { cents ->
                        if (cents % 100 == 0L) (cents / 100).toString()
                        else String.format(java.util.Locale.US, "%.2f", cents / 100.0).trimEnd('0').trimEnd('.')
                    } ?: "0"
                )
            }
            var expanded by remember { mutableStateOf(false) }
            var parentExpanded by remember { mutableStateOf(false) }
            var currencyExpanded by remember { mutableStateOf(false) }
            val eligibleParents = remember(allAccounts, accountToEdit) {
                allAccounts.filter { it.parentAccountId == null && it.id != accountToEdit?.id }
            }

            AlertDialog(
                onDismissRequest = {
                    showAddDialog = false
                    preselectedParentForAdd = null
                    accountToEdit = null
                },
                title = {
                    Text(
                        if (isEditing) "Edit Account" else "Add New Account",
                        fontWeight = FontWeight.Black
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Account Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        )

                        if (!hasChildren && eligibleParents.isNotEmpty()) {
                            val selectedParentName = eligibleParents.find { it.id == parentAccountId }?.name ?: "None (Top-level)"
                            ExposedDropdownMenuBox(
                                expanded = parentExpanded,
                                onExpandedChange = { parentExpanded = !parentExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedParentName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Parent Account (Optional)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentExpanded) },
                                    modifier = Modifier
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth(),
                                    shape = MaterialTheme.shapes.large
                                )
                                ExposedDropdownMenu(
                                    expanded = parentExpanded,
                                    onDismissRequest = { parentExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("None (Top-level)") },
                                        onClick = {
                                            parentAccountId = null
                                            parentExpanded = false
                                        }
                                    )
                                    eligibleParents.forEach { candidate ->
                                        DropdownMenuItem(
                                            text = { Text("${candidate.name} (${candidate.type})") },
                                            onClick = {
                                                parentAccountId = candidate.id
                                                type = candidate.type
                                                parentExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1.2f)) {
                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = !expanded }
                                ) {
                                    OutlinedTextField(
                                        value = type,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Type") },
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
                                        state.accountTypes.forEach { accountType ->
                                            val accountIcon = when (accountType.icon) {
                                                "AccountBalanceWallet" -> Icons.Default.AccountBalanceWallet
                                                "AccountBalance" -> Icons.Default.AccountBalance
                                                "CreditCard" -> Icons.Default.CreditCard
                                                "Payments" -> Icons.Default.Payments
                                                else -> Icons.Default.AccountBalance
                                            }
                                            DropdownMenuItem(
                                                text = { Text(accountType.name) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = accountIcon,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                onClick = {
                                                    type = accountType.name
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(0.9f)) {
                                ExposedDropdownMenuBox(
                                    expanded = currencyExpanded,
                                    onExpandedChange = { currencyExpanded = !currencyExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = currency,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Currency") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                                        modifier = Modifier
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth(),
                                        shape = MaterialTheme.shapes.large
                                    )
                                    ExposedDropdownMenu(
                                        expanded = currencyExpanded,
                                        onDismissRequest = { currencyExpanded = false }
                                    ) {
                                        val currencies = if (state.enabledCurrencies.isNotEmpty()) {
                                            state.enabledCurrencies
                                        } else {
                                            com.sans.finance.data.util.LocaleManager.COMMON_CURRENCIES
                                        }
                                        currencies.forEach { curr ->
                                            DropdownMenuItem(
                                                text = { Text(curr) },
                                                onClick = {
                                                    currency = curr
                                                    currencyExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = balance,
                            onValueChange = { input ->
                                val filtered = buildString {
                                    var hasDot = false
                                    for ((idx, ch) in input.withIndex()) {
                                        when {
                                            ch.isDigit() -> append(ch)
                                            ch == '-' && idx == 0 -> append(ch)
                                            (ch == '.' || ch == ',') && !hasDot -> {
                                                hasDot = true
                                                append('.')
                                            }
                                        }
                                    }
                                }
                                balance = filtered
                            },
                            label = { Text("Current Balance ($currency)") },
                            visualTransformation = com.sans.finance.core.util.ThousandsSeparatorVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        )

                        val selectedTypeObj = state.accountTypes.find { it.name == type }
                        if (selectedTypeObj?.isLiability == true) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedTextField(
                                    value = interestRate,
                                    onValueChange = { interestRate = it },
                                    label = { Text("Interest Rate (%)") },
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.large,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                                    )
                                )
                                OutlinedTextField(
                                    value = minPayment,
                                    onValueChange = { input ->
                                        val filtered = buildString {
                                            var hasDot = false
                                            for (ch in input) {
                                                when {
                                                    ch.isDigit() -> append(ch)
                                                    (ch == '.' || ch == ',') && !hasDot -> {
                                                        hasDot = true
                                                        append('.')
                                                    }
                                                }
                                            }
                                        }
                                        minPayment = filtered
                                    },
                                    label = { Text("Min. Payment") },
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.large,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                                    )
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (name.isBlank() || type.isBlank()) return@Button
                            val parsedBalance = balance.toDoubleOrNull()?.let { kotlin.math.round(it * 100.0).toLong() } ?: 0L
                            val parsedInterest = interestRate.toDoubleOrNull() ?: 0.0
                            val parsedMinPayment = minPayment.toDoubleOrNull()?.let { kotlin.math.round(it * 100.0).toLong() } ?: 0L

                            if (isEditing) {
                                if (parsedBalance != accountToEdit!!.balance) {
                                    pendingUpdateData = AccountUpdateParams(
                                        account = accountToEdit!!,
                                        name = name,
                                        type = type,
                                        balance = parsedBalance,
                                        currency = currency,
                                        interestRate = parsedInterest,
                                        minPayment = parsedMinPayment,
                                        parentAccountId = parentAccountId
                                    )
                                    showAdjustmentDialog = true
                                } else {
                                    viewModel.updateAccount(
                                        account = accountToEdit!!,
                                        newName = name,
                                        newType = type,
                                        newBalance = parsedBalance,
                                        newCurrency = currency,
                                        interestRate = parsedInterest,
                                        minPayment = parsedMinPayment,
                                        newParentAccountId = parentAccountId
                                    )
                                }
                            } else {
                                viewModel.addAccount(
                                    name = name,
                                    type = type,
                                    initialBalance = parsedBalance,
                                    currency = currency,
                                    interestRate = parsedInterest,
                                    minPayment = parsedMinPayment,
                                    parentAccountId = parentAccountId
                                )
                            }
                            showAddDialog = false
                            preselectedParentForAdd = null
                            accountToEdit = null
                        },
                        shape = MaterialTheme.shapes.large,
                        enabled = name.isNotBlank() && type.isNotBlank()
                    ) {
                        Text(if (isEditing) "Save" else "Add", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isEditing && accountToEdit?.id != 1L) {
                            IconButton(onClick = {
                                viewModel.deleteAccount(accountToEdit!!.id)
                                showAddDialog = false
                                preselectedParentForAdd = null
                                accountToEdit = null
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        TextButton(onClick = {
                            showAddDialog = false
                            preselectedParentForAdd = null
                            accountToEdit = null
                        }) {
                            Text("Cancel")
                        }
                    }
                },
                shape = MaterialTheme.shapes.extraLarge
            )
        }

        if (showAdjustmentDialog && pendingUpdateData != null) {
            val data = pendingUpdateData!!
            val diff = data.balance - data.account.balance
            val isIncrease = diff > 0
            val absDiff = if (diff > 0) diff else -diff

            AlertDialog(
                onDismissRequest = {
                    showAdjustmentDialog = false
                    pendingUpdateData = null
                },
                title = { Text("Balance Adjustment", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("You changed the balance of ${data.account.name}.")
                        Text(
                            "Difference: ${if (isIncrease) "+" else "-"}${
                                com.sans.finance.core.util.CurrencyFormatter.formatAmount(
                                    absDiff,
                                    data.currency
                                )
                            }",
                            fontWeight = FontWeight.Bold,
                            color = if (isIncrease) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Would you like to record this difference as a transaction?")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateAccount(
                                account = data.account,
                                newName = data.name,
                                newType = data.type,
                                newBalance = data.balance,
                                newCurrency = data.currency,
                                recordAdjustment = true,
                                interestRate = data.interestRate,
                                minPayment = data.minPayment,
                                newParentAccountId = data.parentAccountId
                            )
                            showAdjustmentDialog = false
                            pendingUpdateData = null
                        }
                    ) {
                        Text("Record as ${if (isIncrease) "Income" else "Expense"}")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.updateAccount(
                                account = data.account,
                                newName = data.name,
                                newType = data.type,
                                newBalance = data.balance,
                                newCurrency = data.currency,
                                recordAdjustment = false,
                                interestRate = data.interestRate,
                                minPayment = data.minPayment,
                                newParentAccountId = data.parentAccountId
                            )
                            showAdjustmentDialog = false
                            pendingUpdateData = null
                        }
                    ) {
                        Text("Just Update Balance")
                    }
                }
            )
        }

        if (selectedAccountForAction != null) {
            val account = selectedAccountForAction!!
            val converted = if (account.currency != state.currentCurrency) {
                val fromRate = if (account.currency == "IDR") 1.0 else (state.ratesMap[account.currency] ?: 1.0)
                val toRate = if (state.currentCurrency == "IDR") 1.0 else (state.ratesMap[state.currentCurrency] ?: 1.0)
                if (toRate > 0) ((account.balance * fromRate) / toRate).toLong() else account.balance
            } else null

            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { selectedAccountForAction = null }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val parentName = account.parentAccountId?.let { pid ->
                                state.accountsByType.values.flatten().find { it.id == pid }?.name
                            }
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = buildString {
                                    append("${account.type} • ${account.currency}")
                                    if (parentName != null) append(" • Subaccount of $parentName")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            PrivacyText(
                                amount = account.balance,
                                currencyCode = account.currency,
                                isVisible = !state.isPrivacyModeEnabled,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (converted != null) {
                                Text(
                                    text = "≈ ${com.sans.finance.core.util.CurrencyFormatter.formatAmount(converted, state.currentCurrency)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    androidx.compose.material3.ListItem(
                        headlineContent = { Text("View Ledger") },
                        supportingContent = { Text("Open transactions filtered to this account") },
                        leadingContent = {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier.clickable {
                            val id = account.id
                            selectedAccountForAction = null
                            onViewLedger(id)
                        }
                    )

                    if (account.parentAccountId == null) {
                        androidx.compose.material3.ListItem(
                            headlineContent = { Text("Add Subaccount") },
                            supportingContent = { Text("Create a pocket or currency under ${account.name}") },
                            leadingContent = {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            },
                            modifier = Modifier.clickable {
                                preselectedParentForAdd = account
                                selectedAccountForAction = null
                                showAddDialog = true
                            }
                        )
                    }

                    androidx.compose.material3.ListItem(
                        headlineContent = { Text("Edit Account") },
                        supportingContent = { Text("Change name, type, or currency") },
                        leadingContent = {
                            Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        modifier = Modifier.clickable {
                            accountToEdit = account
                            selectedAccountForAction = null
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun AccountGroupCard(
    type: String,
    accounts: List<AccountEntity>,
    currentCurrency: String,
    ratesMap: Map<String, Double> = emptyMap(),
    isPrivacyModeEnabled: Boolean,
    onAccountClick: (AccountEntity) -> Unit,
    onMoveAccountUp: (AccountEntity) -> Unit = {},
    onMoveAccountDown: (AccountEntity) -> Unit = {},
    isReorderMode: Boolean = false,
    accountTypes: List<com.sans.finance.data.local.entity.AccountTypeEntity> = emptyList()
) {
    val typeObj = accountTypes.find { it.name == type }
    val isLiability = typeObj?.isLiability == true

    fun convertToBase(amount: Long, from: String): Long {
        if (from == currentCurrency) return amount
        val fromRate = if (from == "IDR") 1.0 else (ratesMap[from] ?: 1.0)
        val toRate = if (currentCurrency == "IDR") 1.0 else (ratesMap[currentCurrency] ?: 1.0)
        if (toRate == 0.0) return amount
        return ((amount * fromRate) / toRate).toLong()
    }

    val groupTotal = accounts.sumOf {
        val converted = convertToBase(it.balance, it.currency)
        if (isLiability) -converted else converted
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val headerIcon = when (typeObj?.icon) {
                        "AccountBalanceWallet" -> Icons.Default.AccountBalanceWallet
                        "AccountBalance" -> Icons.Default.AccountBalance
                        "CreditCard" -> Icons.Default.CreditCard
                        "Payments" -> Icons.Default.Payments
                        else -> Icons.Default.AccountBalance
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = headerIcon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        type.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.sp
                    )
                }
                PrivacyText(
                    amount = groupTotal,
                    currencyCode = currentCurrency,
                    isVisible = !isPrivacyModeEnabled,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = if (groupTotal < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            val hierarchy = accounts.groupByParent()
            val childrenByParent = hierarchy.childrenByParent
            val topLevelAccounts = hierarchy.topLevel
            val expandedParents = remember { androidx.compose.runtime.mutableStateMapOf<Long, Boolean>() }

            topLevelAccounts.forEachIndexed { index, account ->
                val children = childrenByParent[account.id].orEmpty()
                val isExpanded = expandedParents[account.id] ?: true
                val convertedBalance = if (account.currency != currentCurrency) {
                    convertToBase(account.balance, account.currency)
                } else null
                val rolledUpTotal = if (children.isNotEmpty()) {
                    convertToBase(account.balance, account.currency) +
                        children.sumOf { convertToBase(it.balance, it.currency) }
                } else null

                AccountRowItem(
                    account = account,
                    currentCurrency = currentCurrency,
                    convertedBalance = convertedBalance,
                    rolledUpTotal = rolledUpTotal,
                    subaccountCount = children.size,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        expandedParents[account.id] = !isExpanded
                    },
                    isPrivacyModeEnabled = isPrivacyModeEnabled,
                    onClick = { onAccountClick(account) },
                    onMoveUp = { onMoveAccountUp(account) },
                    onMoveDown = { onMoveAccountDown(account) },
                    isReorderMode = isReorderMode,
                    isLiability = isLiability
                )

                if (isExpanded && children.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ) {
                        Column {
                            children.forEachIndexed { childIndex, child ->
                                val childConverted = if (child.currency != currentCurrency) {
                                    convertToBase(child.balance, child.currency)
                                } else null
                                AccountRowItem(
                                    account = child,
                                    currentCurrency = currentCurrency,
                                    convertedBalance = childConverted,
                                    isSubaccount = true,
                                    isPrivacyModeEnabled = isPrivacyModeEnabled,
                                    onClick = { onAccountClick(child) },
                                    onMoveUp = { onMoveAccountUp(child) },
                                    onMoveDown = { onMoveAccountDown(child) },
                                    isReorderMode = isReorderMode,
                                    isLiability = isLiability
                                )
                                if (childIndex < children.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (index < topLevelAccounts.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
fun AccountRowItem(
    account: AccountEntity,
    currentCurrency: String,
    convertedBalance: Long? = null,
    rolledUpTotal: Long? = null,
    subaccountCount: Int = 0,
    isExpanded: Boolean = true,
    onToggleExpand: (() -> Unit)? = null,
    isSubaccount: Boolean = false,
    isPrivacyModeEnabled: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    isReorderMode: Boolean = false,
    isLiability: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = androidx.compose.ui.semantics.Role.Button,
                onClickLabel = "View account details"
            ) { onClick() }
            .padding(
                horizontal = if (isSubaccount) 14.dp else 16.dp,
                vertical = if (isSubaccount) 10.dp else 14.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSubaccount) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(26.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    account.name,
                    style = if (isSubaccount) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSubaccount) FontWeight.SemiBold else FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isSubaccount) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            account.currency,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            if (!isSubaccount) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        account.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (subaccountCount > 0 && onToggleExpand != null) {
                        Surface(
                            onClick = onToggleExpand,
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "$subaccountCount pockets",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                    contentDescription = if (isExpanded) "Collapse subaccounts" else "Expand subaccounts",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isReorderMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMoveUp) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onMoveDown) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", modifier = Modifier.size(20.dp))
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            if (rolledUpTotal != null) {
                PrivacyText(
                    amount = rolledUpTotal,
                    currencyCode = currentCurrency,
                    isVisible = !isPrivacyModeEnabled,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black,
                    color = if (isLiability) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                if (account.balance != 0L) {
                    Text(
                        text = if (isPrivacyModeEnabled) "••••••" else "Own: ${com.sans.finance.core.util.CurrencyFormatter.formatAmount(account.balance, account.currency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                PrivacyText(
                    amount = account.balance,
                    currencyCode = account.currency,
                    isVisible = !isPrivacyModeEnabled,
                    style = if (isSubaccount) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black,
                    color = if (isLiability) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                if (account.currency != currentCurrency && convertedBalance != null) {
                    Text(
                        text = if (isPrivacyModeEnabled) "••••••" else "≈ ${com.sans.finance.core.util.CurrencyFormatter.formatAmount(convertedBalance, currentCurrency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AccountHeaderStats(state: AccountScreenState) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primary,
        alpha = 0.12f
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderStatItem(
                label = "Total Assets",
                amount = state.assets,
                currencyCode = state.currentCurrency,
                isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                color = MaterialTheme.colorScheme.tertiary,
                icon = Icons.AutoMirrored.Filled.TrendingUp
            )

            VerticalDivider(
                modifier = Modifier.height(40.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            HeaderStatItem(
                label = "Total Liabilities",
                amount = state.liabilities,
                currencyCode = state.currentCurrency,
                isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                color = MaterialTheme.colorScheme.error,
                icon = Icons.AutoMirrored.Filled.TrendingDown
            )
        }
    }
}

@Composable
fun HeaderStatItem(
    label: String,
    amount: Long,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = color
            )
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        PrivacyText(
            amount = amount,
            currencyCode = currencyCode,
            isVisible = !isPrivacyModeEnabled,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Black
        )
    }
}
