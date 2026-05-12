package com.sans.finance.presentation.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onStatsClick) {
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = "Account Statistics"
                        )
                    }
                    IconButton(onClick = { viewModel.togglePrivacyMode() }) {
                        Icon(
                            imageVector = if (state.isPrivacyModeEnabled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (state.isPrivacyModeEnabled) "Show balances" else "Hide balances"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp) // extra padding for FAB
        ) {
            item {
                AccountHeaderStats(state)
            }



            state.accountsByType.forEach { (type, accounts) ->
                item {
                    val groupTotal =
                        accounts.sumOf { if (it.type == "Credit Card" || it.type == "Loan") -it.balance else it.balance }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val headerIcon = when (type) {
                                "Cash" -> Icons.Default.AccountBalanceWallet
                                "Bank" -> Icons.Default.AccountBalance
                                "Credit Card" -> Icons.Default.CreditCard
                                "Loan" -> Icons.Default.Payments
                                else -> Icons.Default.AccountBalance
                            }
                            Icon(
                                imageVector = headerIcon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                type,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PrivacyText(
                            amount = groupTotal,
                            currencyCode = state.currentCurrency,
                            isVisible = !state.isPrivacyModeEnabled,
                            fontWeight = FontWeight.Bold,
                            color = if (groupTotal < 0) Color(0xFFE57373) else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                items(accounts) { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { accountToEdit = account }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (account.type) {
                            "Cash" -> Icons.Default.AccountBalanceWallet
                            "Bank" -> Icons.Default.AccountBalance
                            "Credit Card" -> Icons.Default.CreditCard
                            "Loan" -> Icons.Default.Payments
                            else -> Icons.Default.AccountBalance
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )

                        Text(
                            account.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        PrivacyText(
                            amount = account.balance,
                            currencyCode = account.currency,
                            isVisible = !state.isPrivacyModeEnabled,
                            color = if (account.type == "Credit Card" || account.type == "Loan") Color(
                                0xFFE57373
                            ) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    )
                }
            }
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
                title = { Text(if (isEditing) "Edit Account" else "Add New Account") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Account Name") }
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
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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
                            onValueChange = { balance = it.filter { char -> char.isDigit() } },
                            label = { Text("Initial Balance") },
                            visualTransformation = com.sans.finance.core.util.ThousandsSeparatorVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val parsedBalance = balance.toLongOrNull()?.times(100) ?: 0L
                        if (isEditing) {
                            viewModel.updateAccount(accountToEdit!!, name, type, parsedBalance)
                        } else {
                            viewModel.addAccount(name, type, parsedBalance, state.currentCurrency)
                        }
                        showAddDialog = false
                        accountToEdit = null
                    }) {
                        Text(if (isEditing) "Save" else "Add")
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
                }
            )
        }
    }
}

@Composable
fun AccountHeaderStats(state: AccountScreenState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Assets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            PrivacyText(
                amount = state.assets,
                currencyCode = state.currentCurrency,
                isVisible = !state.isPrivacyModeEnabled,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFFE57373)
                )
                Text(
                    "Liabilities",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            PrivacyText(
                amount = state.liabilities,
                currencyCode = state.currentCurrency,
                isVisible = !state.isPrivacyModeEnabled,
                color = Color(0xFFE57373),
                fontWeight = FontWeight.Bold
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.AccountBalance,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (state.total < 0) Color(0xFFE57373) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Cash Liquidity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            PrivacyText(
                amount = state.total,
                currencyCode = state.currentCurrency,
                isVisible = !state.isPrivacyModeEnabled,
                color = if (state.total < 0) Color(0xFFE57373) else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


