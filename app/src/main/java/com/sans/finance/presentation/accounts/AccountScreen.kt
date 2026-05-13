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
import androidx.compose.material.icons.filled.QueryStats
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
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.presentation.components.PrivacyText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onStatsClick: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Accounts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
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
                                    Icons.Default.QueryStats,
                                    contentDescription = null
                                )
                            }
                        )

                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                AccountHeaderStats(state)
            }

            state.accountsByType.forEach { (type, accounts) ->
                item {
                    AccountGroupCard(
                        type = type,
                        accounts = accounts,
                        currentCurrency = state.currentCurrency,
                        isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                        onAccountClick = { accountToEdit = it }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        if (showAddDialog || accountToEdit != null) {
            val isEditing = accountToEdit != null
            var name by remember(accountToEdit) { mutableStateOf(accountToEdit?.name ?: "") }
            var type by remember(accountToEdit) { mutableStateOf(accountToEdit?.type ?: "Bank") }
            var balance by remember(accountToEdit) {
                mutableStateOf(accountToEdit?.balance?.let { (it / 100).toString() } ?: "")
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
                                listOf(
                                    "Cash",
                                    "Bank",
                                    "Credit Card",
                                    "Loan"
                                ).forEach { accountType ->
                                    val accountIcon = when (accountType) {
                                        "Cash" -> Icons.Default.AccountBalanceWallet
                                        "Bank" -> Icons.Default.AccountBalance
                                        "Credit Card" -> Icons.Default.CreditCard
                                        "Loan" -> Icons.Default.Payments
                                        else -> Icons.Default.AccountBalance
                                    }
                                    DropdownMenuItem(
                                        text = { Text(accountType) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = accountIcon,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            type = accountType
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
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val parsedBalance = balance.toLongOrNull()?.times(100) ?: 0L
                            if (isEditing) {
                                viewModel.updateAccount(accountToEdit!!, name, type, parsedBalance)
                            } else {
                                viewModel.addAccount(
                                    name,
                                    type,
                                    parsedBalance,
                                    state.currentCurrency
                                )
                            }
                            showAddDialog = false
                            accountToEdit = null
                        },
                        shape = MaterialTheme.shapes.large
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
    }
}

@Composable
fun AccountGroupCard(
    type: String,
    accounts: List<AccountEntity>,
    currentCurrency: String,
    isPrivacyModeEnabled: Boolean,
    onAccountClick: (AccountEntity) -> Unit
) {
    val groupTotal =
        accounts.sumOf { if (it.type == "Credit Card" || it.type == "Loan") -it.balance else it.balance }

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
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val headerIcon = when (type) {
                        "Cash" -> Icons.Default.AccountBalanceWallet
                        "Bank" -> Icons.Default.AccountBalance
                        "Credit Card" -> Icons.Default.CreditCard
                        "Loan" -> Icons.Default.Payments
                        else -> Icons.Default.AccountBalance
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = headerIcon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        type,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
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
                    onClick = { onAccountClick(account) }
                )
                if (index < accounts.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
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

        PrivacyText(
            amount = account.balance,
            currencyCode = account.currency,
            isVisible = !isPrivacyModeEnabled,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Black,
            color = if (account.type == "Credit Card" || account.type == "Loan") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AccountHeaderStats(state: AccountScreenState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                alpha = 0.3f
            )
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderStatItem(
                label = "Assets",
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
                label = "Liabilities",
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                letterSpacing = 1.sp
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
