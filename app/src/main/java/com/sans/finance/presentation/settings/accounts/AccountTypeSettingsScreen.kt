package com.sans.finance.presentation.settings.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.data.local.entity.AccountTypeEntity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountTypeSettingsScreen(
    onBack: () -> Unit,
    viewModel: AccountTypeSettingsViewModel = hiltViewModel()
) {
    val accountTypes by viewModel.accountTypes.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var accountTypeToEdit by remember { mutableStateOf<AccountTypeEntity?>(null) }
    var accountTypeToDelete by remember { mutableStateOf<AccountTypeEntity?>(null) }
    var isReorderMode by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(message) {
        message?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Account Types", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isReorderMode = !isReorderMode }) {
                        Icon(
                            if (isReorderMode) Icons.Default.Check else Icons.Default.SwapVert,
                            contentDescription = "Toggle Reorder Mode"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Account Type")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(accountTypes) { type ->
                AccountTypeItem(
                    accountType = type,
                    onEdit = { accountTypeToEdit = type },
                    onDelete = { accountTypeToDelete = type },
                    onMoveUp = { viewModel.moveUp(type) },
                    onMoveDown = { viewModel.moveDown(type) },
                    isReorderMode = isReorderMode
                )
            }
        }

        if (showAddDialog || accountTypeToEdit != null) {
            AccountTypeEditDialog(
                accountType = accountTypeToEdit,
                onDismiss = {
                    showAddDialog = false
                    accountTypeToEdit = null
                },
                onConfirm = { name, icon, isLiability ->
                    if (accountTypeToEdit != null) {
                        viewModel.updateAccountType(accountTypeToEdit!!, name, icon, isLiability)
                    } else {
                        viewModel.addAccountType(name, icon, isLiability)
                    }
                    showAddDialog = false
                    accountTypeToEdit = null
                }
            )
        }

        if (accountTypeToDelete != null) {
            AlertDialog(
                onDismissRequest = { accountTypeToDelete = null },
                title = { Text("Delete Account Type", fontWeight = FontWeight.Black) },
                text = { Text("Are you sure you want to delete '${accountTypeToDelete?.name}'? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            accountTypeToDelete?.let { viewModel.deleteAccountType(it.id) }
                            accountTypeToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { accountTypeToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun AccountTypeItem(
    accountType: AccountTypeEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    isReorderMode: Boolean = false
) {
    val icon = when (accountType.icon) {
        "AccountBalanceWallet" -> Icons.Default.AccountBalanceWallet
        "AccountBalance" -> Icons.Default.AccountBalance
        "CreditCard" -> Icons.Default.CreditCard
        "Payments" -> Icons.Default.Payments
        else -> Icons.Default.AccountBalance
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = accountType.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (accountType.isLiability) "Liability" else "Asset",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (accountType.isLiability) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                )
            }
            if (isReorderMode) {
                IconButton(onClick = onMoveUp) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onMoveDown) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", modifier = Modifier.size(20.dp))
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun AccountTypeEditDialog(
    accountType: AccountTypeEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean) -> Unit
) {
    var name by remember(accountType) { mutableStateOf(accountType?.name ?: "") }
    var icon by remember(accountType) { mutableStateOf(accountType?.icon ?: "AccountBalance") }
    var isLiability by remember(accountType) { mutableStateOf(accountType?.isLiability ?: false) }

    val icons = listOf(
        "AccountBalanceWallet" to Icons.Default.AccountBalanceWallet,
        "AccountBalance" to Icons.Default.AccountBalance,
        "CreditCard" to Icons.Default.CreditCard,
        "Payments" to Icons.Default.Payments
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (accountType == null) "Add Account Type" else "Edit Account Type", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )

                Text("Icon", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    icons.forEach { (iconName, iconVector) ->
                        val isSelected = icon == iconName
                        Surface(
                            onClick = { icon = iconName },
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isLiability,
                        onCheckedChange = { isLiability = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Treat this type as liability")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, icon, isLiability) },
                shape = MaterialTheme.shapes.large
            ) {
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
