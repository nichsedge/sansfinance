package com.sans.finance.presentation.add_transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.R
import com.sans.finance.core.util.DateFormatterUtils
import com.sans.finance.presentation.components.CategoryIcon
import java.util.Date
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val datePickerState =
        rememberDatePickerState(initialSelectedDateMillis = viewModel.selectedDate)
    var showDatePicker by remember { androidx.compose.runtime.mutableStateOf(false) }
    val dateFormatter = DateFormatterUtils.getStandardFormatter()
    val focusManager = LocalFocusManager.current
    var titleExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
    var detailsExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
    var accountExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
    var toAccountExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
    var recurrenceExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showDeleteDialog by remember { androidx.compose.runtime.mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (viewModel.isEditMode) stringResource(R.string.edit_transaction) else stringResource(
                            R.string.add_transaction
                        ), fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (viewModel.isEditMode) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.delete_confirmation_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.delete_confirmation_msg))
                        if (viewModel.isInstallmentPayment) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "This is an individual installment payment. Deleting it will mark it as unpaid in the installment plan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (viewModel.isInstallment) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "This is an installment plan anchor. Deleting it will remove the entire plan and all associated payments.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.onDeleteClick(deleteEntirePlan = true, onSuccess = onBack)
                            showDeleteDialog = false
                        }
                    ) {
                        Text(
                            stringResource(R.string.delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (viewModel.showDuplicateDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.showDuplicateDialog = false },
                title = { Text("Potential Duplicate") },
                text = {
                    Text("A similar transaction already exists: \"${viewModel.duplicateFound?.title}\" for ${viewModel.duplicateFound?.amount?.let { it / 100.0 }} ${viewModel.duplicateFound?.currency}.\n\nDo you want to add this as a new transaction anyway?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.onSaveClick(onBack)
                        }
                    ) {
                        Text("Add Anyway")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.showDuplicateDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (viewModel.isInstallmentPayment) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Installment Payment",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Payment ${viewModel.installmentMonth} of ${viewModel.installmentTotalMonths} for this plan",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (viewModel.status == "Pending") {
                            Button(
                                onClick = {
                                    viewModel.onStatusChange("Paid")
                                    viewModel.onSaveClick { onBack() }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Mark as Paid", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            // Transaction Type Selector (Segmented Buttons / Tabs)
            // Transaction Type Selector (Rounded Chips)
            val types = listOf("EXPENSE", "INCOME", "TRANSFER")
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(types) { type ->
                    FilterChip(
                        selected = viewModel.transactionType == type,
                        onClick = { viewModel.transactionType = type },
                        label = {
                            Text(
                                type.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (viewModel.transactionType == type) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.height(32.dp)
                    )
                }
            }


            // Account Selector
            @OptIn(ExperimentalMaterial3Api::class)
            ExposedDropdownMenuBox(
                expanded = accountExpanded,
                onExpandedChange = { accountExpanded = !accountExpanded }
            ) {
                val selectedAccount = accounts.find { it.id == viewModel.accountId }
                OutlinedTextField(
                    value = selectedAccount?.name ?: "Unknown Account",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (viewModel.transactionType == "TRANSFER") "From Account" else "Account", fontSize = 12.sp) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    shape = MaterialTheme.shapes.small,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                ExposedDropdownMenu(
                    expanded = accountExpanded,
                    onDismissRequest = { accountExpanded = false }
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                viewModel.accountId = account.id
                                accountExpanded = false
                            }
                        )
                    }
                }
            }

            if (viewModel.transactionType == "TRANSFER") {
                ExposedDropdownMenuBox(
                    expanded = toAccountExpanded,
                    onExpandedChange = { toAccountExpanded = !toAccountExpanded }
                ) {
                    val selectedToAccount = accounts.find { it.id == viewModel.toAccountId }
                    OutlinedTextField(
                        value = selectedToAccount?.name ?: "Unknown Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("To Account", fontSize = 12.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toAccountExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = MaterialTheme.shapes.small,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    ExposedDropdownMenu(
                        expanded = toAccountExpanded,
                        onDismissRequest = { toAccountExpanded = false }
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    viewModel.toAccountId = account.id
                                    toAccountExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Amount Input with large text
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var currencyExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { currencyExpanded = !currencyExpanded },
                    modifier = Modifier.width(100.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.currency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Curr", fontSize = 10.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = MaterialTheme.shapes.small,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                    ExposedDropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        viewModel.enabledCurrencies.forEach { curr ->
                            DropdownMenuItem(
                                text = { Text(curr) },
                                onClick = {
                                    viewModel.currency = curr
                                    currencyExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = viewModel.amount,
                    onValueChange = { viewModel.amount = it },
                    label = { Text(stringResource(R.string.amount_spent), fontSize = 12.sp) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    singleLine = true,
                    visualTransformation = com.sans.finance.core.util.ThousandsSeparatorVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                    ),
                    shape = MaterialTheme.shapes.small
                )
            }

            // Date Picker Field
            OutlinedTextField(
                value = dateFormatter.format(Date(viewModel.selectedDate)),
                onValueChange = { },
                label = { Text("Transaction Date", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                },
                shape = MaterialTheme.shapes.small,
                textStyle = MaterialTheme.typography.bodyMedium
            )

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                viewModel.selectedDate = it
                            }
                            showDatePicker = false
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            @OptIn(ExperimentalMaterial3Api::class)
            ExposedDropdownMenuBox(
                expanded = titleExpanded && viewModel.titleSuggestions.isNotEmpty(),
                onExpandedChange = { titleExpanded = !titleExpanded }
            ) {
                OutlinedTextField(
                    value = viewModel.title,
                    onValueChange = {
                        viewModel.title = it
                        titleExpanded = true
                    },
                    label = { Text(stringResource(R.string.what_did_you_buy), fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                    ),
                    shape = MaterialTheme.shapes.small,
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                ExposedDropdownMenu(
                    expanded = titleExpanded && viewModel.titleSuggestions.isNotEmpty(),
                    onDismissRequest = { titleExpanded = false }
                ) {
                    viewModel.titleSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                viewModel.title = suggestion
                                titleExpanded = false
                                viewModel.applyPrediction(suggestion)
                            }
                        )
                    }
                }
            }

            Text(
                stringResource(R.string.category).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.5.sp
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = viewModel.categoryId == category.id,
                        onClick = { viewModel.categoryId = category.id },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryIcon(category.icon, fontSize = 14.sp)
                                Spacer(Modifier.width(4.dp))
                                Text(category.name, style = MaterialTheme.typography.labelMedium)
                            }
                        },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.height(32.dp)
                    )
                }
            }

            Text(
                "Tags".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.5.sp
            )

            val allTags by viewModel.allTags.collectAsStateWithLifecycle()

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tagsToShow = (allTags + viewModel.selectedTags).distinct()
                tagsToShow.forEach { tagName ->
                    FilterChip(
                        selected = viewModel.selectedTags.contains(tagName),
                        onClick = { viewModel.toggleTag(tagName) },
                        label = { Text(tagName, style = MaterialTheme.typography.labelMedium) },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.height(32.dp)
                    )
                }
            }

            OutlinedTextField(
                value = viewModel.newTagText,
                onValueChange = { viewModel.newTagText = it },
                label = { Text("Add New Tag", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { viewModel.addNewTag() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Tag")
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        viewModel.addNewTag()
                        focusManager.clearFocus()
                    }
                ),
                shape = MaterialTheme.shapes.small,
                textStyle = MaterialTheme.typography.bodyMedium
            )


            @OptIn(ExperimentalMaterial3Api::class)
            ExposedDropdownMenuBox(
                expanded = detailsExpanded && viewModel.detailsSuggestions.isNotEmpty(),
                onExpandedChange = { detailsExpanded = !detailsExpanded }
            ) {
                OutlinedTextField(
                    value = viewModel.details,
                    onValueChange = {
                        viewModel.details = it
                        detailsExpanded = true
                    },
                    label = { Text(stringResource(R.string.merchant_store), fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (viewModel.isInstallment) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { if (viewModel.isInstallment) focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) else focusManager.clearFocus() },
                        onDone = {
                            focusManager.clearFocus()
                            if (!viewModel.isInstallment) viewModel.onSaveClick(onBack)
                        }
                    ),
                    shape = MaterialTheme.shapes.small,
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                ExposedDropdownMenu(
                    expanded = detailsExpanded && viewModel.detailsSuggestions.isNotEmpty(),
                    onDismissRequest = { detailsExpanded = false }
                ) {
                    viewModel.detailsSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                viewModel.details = suggestion
                                detailsExpanded = false
                            }
                        )
                    }
                }
            }

            val paymentTypes = listOf("ONE_TIME", "RECURRING", "INSTALLMENT")
            val paymentLabels = listOf(
                stringResource(R.string.one_time),
                stringResource(R.string.recurring_expenses),
                stringResource(R.string.installment)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(paymentTypes.size) { index ->
                    val type = paymentTypes[index]
                    FilterChip(
                        selected = viewModel.paymentType == type,
                        onClick = { viewModel.paymentType = type },
                        label = {
                            Text(
                                paymentLabels[index],
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (viewModel.paymentType == type) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.height(32.dp)
                    )
                }
            }

            if (viewModel.isRecurring) {
                @OptIn(ExperimentalMaterial3Api::class)
                ExposedDropdownMenuBox(
                    expanded = recurrenceExpanded,
                    onExpandedChange = { recurrenceExpanded = !recurrenceExpanded }
                ) {
                    OutlinedTextField(
                        value = viewModel.recurrenceInterval.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Recurrence Interval", fontSize = 12.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recurrenceExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = MaterialTheme.shapes.small,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    ExposedDropdownMenu(
                        expanded = recurrenceExpanded,
                        onDismissRequest = { recurrenceExpanded = false }
                    ) {
                        listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY").forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        type.lowercase().replaceFirstChar { it.uppercase() })
                                },
                                onClick = {
                                    viewModel.recurrenceInterval = type
                                    recurrenceExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (viewModel.isInstallment) {
                OutlinedTextField(
                    value = viewModel.durationMonths,
                    onValueChange = { viewModel.durationMonths = it },
                    label = { Text(stringResource(R.string.duration_months), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.onSaveClick(onBack)
                        }
                    ),
                    shape = MaterialTheme.shapes.small,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            Button(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    viewModel.onSaveClick(onBack)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = MaterialTheme.shapes.small
) {
                Text(
                    text = (if (viewModel.isEditMode) stringResource(R.string.update_transaction) else stringResource(
                        R.string.confirm_transaction
                    )).uppercase(),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
