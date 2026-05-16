package com.sans.finance.presentation.accounts

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.presentation.components.PrivacyText
import com.sans.finance.presentation.components.GlassCard
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class AccountUpdateParams(
    val account: AccountEntity,
    val name: String,
    val type: String,
    val balance: Long,
    val interestRate: Double,
    val minPayment: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onStatsClick: () -> Unit,
    onDebtStrategistClick: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: AccountViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showAdjustmentDialog by remember { mutableStateOf(false) }
    var pendingUpdateData by remember { mutableStateOf<AccountUpdateParams?>(null) }
    var isReorderMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Accounts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
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
                            isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                            onAccountClick = { accountToEdit = it },
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
                            isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                            onAccountClick = { accountToEdit = it },
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
            var name by remember(accountToEdit) { mutableStateOf(accountToEdit?.name ?: "") }
            var type by remember(accountToEdit, state.accountTypes) { 
                mutableStateOf(accountToEdit?.type ?: state.accountTypes.firstOrNull()?.name ?: "") 
            }
            var balance by remember(accountToEdit) {
                mutableStateOf(accountToEdit?.balance?.let { (it / 100).toString() } ?: "")
            }
            var interestRate by remember(accountToEdit) {
                mutableStateOf(accountToEdit?.interestRate?.toString() ?: "0.0")
            }
            var minPayment by remember(accountToEdit) {
                mutableStateOf(accountToEdit?.minPayment?.let { (it / 100).toString() } ?: "0")
            }
            var expanded by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = {
                    showAddDialog = false
                    accountToEdit = null
                },
                title = {
                    Text(
                        if (isEditing) "Edit Account" else "Add New Account",
                        fontWeight = FontWeight.Black
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Account Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        )

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

                        OutlinedTextField(
                            value = balance,
                            onValueChange = {
                                balance = it.filter { char -> char.isDigit() || char == '-' }
                            },
                            label = { Text("Current Balance") },
                            visualTransformation = com.sans.finance.core.util.ThousandsSeparatorVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
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
                                    onValueChange = { minPayment = it },
                                    label = { Text("Min. Payment") },
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.large,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
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
                            val parsedBalance = balance.toLongOrNull()?.times(100) ?: 0L
                            val parsedInterest = interestRate.toDoubleOrNull() ?: 0.0
                            val parsedMinPayment = minPayment.toLongOrNull()?.times(100) ?: 0L

                            if (isEditing) {
                                if (parsedBalance != accountToEdit!!.balance) {
                                    pendingUpdateData = AccountUpdateParams(
                                        account = accountToEdit!!,
                                        name = name,
                                        type = type,
                                        balance = parsedBalance,
                                        interestRate = parsedInterest,
                                        minPayment = parsedMinPayment
                                    )
                                    showAdjustmentDialog = true
                                } else {
                                    viewModel.updateAccount(
                                        accountToEdit!!,
                                        name,
                                        type,
                                        parsedBalance,
                                        interestRate = parsedInterest,
                                        minPayment = parsedMinPayment
                                    )
                                }
                            } else {
                                viewModel.addAccount(
                                    name,
                                    type,
                                    parsedBalance,
                                    state.currentCurrency,
                                    interestRate = parsedInterest,
                                    minPayment = parsedMinPayment
                                )
                            }
                            showAddDialog = false
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
                                    data.account.currency
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
                                data.account,
                                data.name,
                                data.type,
                                data.balance,
                                recordAdjustment = true,
                                interestRate = data.interestRate,
                                minPayment = data.minPayment
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
                                data.account,
                                data.name,
                                data.type,
                                data.balance,
                                recordAdjustment = false,
                                interestRate = data.interestRate,
                                minPayment = data.minPayment
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
    }
}

@Composable
fun AccountGroupCard(
    type: String,
    accounts: List<AccountEntity>,
    currentCurrency: String,
    isPrivacyModeEnabled: Boolean,
    onAccountClick: (AccountEntity) -> Unit,
    onMoveAccountUp: (AccountEntity) -> Unit = {},
    onMoveAccountDown: (AccountEntity) -> Unit = {},
    isReorderMode: Boolean = false,
    accountTypes: List<com.sans.finance.data.local.entity.AccountTypeEntity> = emptyList()
) {
    val typeObj = accountTypes.find { it.name == type }
    val isLiability = typeObj?.isLiability == true
    val groupTotal =
        accounts.sumOf { if (isLiability) -it.balance else it.balance }

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

            accounts.forEachIndexed { index, account ->
                AccountRowItem(
                    account = account,
                    isPrivacyModeEnabled = isPrivacyModeEnabled,
                    onClick = { onAccountClick(account) },
                    onMoveUp = { onMoveAccountUp(account) },
                    onMoveDown = { onMoveAccountDown(account) },
                    isReorderMode = isReorderMode,
                    isLiability = isLiability
                )
                if (index < accounts.size - 1) {
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
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                account.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                account.type,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isReorderMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                }
            }
        }

        PrivacyText(
            amount = account.balance,
            currencyCode = account.currency,
            isVisible = !isPrivacyModeEnabled,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Black,
            color = if (isLiability) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
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
