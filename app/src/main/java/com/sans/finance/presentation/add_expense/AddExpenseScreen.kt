package com.sans.finance.presentation.add_expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
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
import com.sans.finance.presentation.components.CategoryIcon
import com.sans.finance.core.util.DateFormatterUtils
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onBack: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val datePickerState =
        rememberDatePickerState(initialSelectedDateMillis = viewModel.selectedDate)
    var showDatePicker by remember { androidx.compose.runtime.mutableStateOf(false) }
    val dateFormatter = DateFormatterUtils.getStandardFormatter()
    val focusManager = LocalFocusManager.current
    var itemNameExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
    var merchantExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
    var typeExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
    var accountExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
    var recurrenceExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
    val currencySymbol = remember {
        try { java.util.Currency.getInstance("IDR").getSymbol(java.util.Locale.getDefault()) } catch (e: Exception) { "Rp" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (viewModel.isEditMode) stringResource(R.string.edit_transaction) else stringResource(
                            R.string.add_expense
                        ), fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Transaction Type Selector
            @OptIn(ExperimentalMaterial3Api::class)
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                OutlinedTextField(
                    value = viewModel.transactionType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Transaction Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    shape = MaterialTheme.shapes.medium
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    listOf("EXPENSE", "INCOME", "TRANSFER").forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                viewModel.transactionType = type
                                typeExpanded = false
                            }
                        )
                    }
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
                    label = { Text("Account") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    shape = MaterialTheme.shapes.medium
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

            // Amount Input with large text
            OutlinedTextField(
                value = viewModel.amount,
                onValueChange = { viewModel.amount = it },
                label = { Text(stringResource(R.string.amount_spent)) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                singleLine = true,
                visualTransformation = com.sans.finance.core.util.ThousandsSeparatorVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                ),
                prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold) },
                shape = MaterialTheme.shapes.medium
            )

            // Date Picker Field
            OutlinedTextField(
                value = dateFormatter.format(Date(viewModel.selectedDate)),
                onValueChange = { },
                label = { Text("Transaction Date") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                },
                shape = MaterialTheme.shapes.medium
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
                expanded = itemNameExpanded && viewModel.itemNameSuggestions.isNotEmpty(),
                onExpandedChange = { itemNameExpanded = !itemNameExpanded }
            ) {
                OutlinedTextField(
                    value = viewModel.itemName,
                    onValueChange = {
                        viewModel.itemName = it
                        itemNameExpanded = true
                    },
                    label = { Text(stringResource(R.string.what_did_you_buy)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                    ),
                    shape = MaterialTheme.shapes.medium
                )

                ExposedDropdownMenu(
                    expanded = itemNameExpanded && viewModel.itemNameSuggestions.isNotEmpty(),
                    onDismissRequest = { itemNameExpanded = false }
                ) {
                    viewModel.itemNameSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                viewModel.itemName = suggestion
                                itemNameExpanded = false
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = viewModel.categoryId == category.id,
                        onClick = { viewModel.categoryId = category.id },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryIcon(category.icon, fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(category.name)
                            }
                        },
                        shape = MaterialTheme.shapes.small
                    )
                }
            }

            Text("Tags".uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.5.sp)

            val allTags by viewModel.allTags.collectAsState()

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tagsToShow = (allTags + viewModel.selectedTags).distinct()
                tagsToShow.forEach { tagName ->
                    FilterChip(
                        selected = viewModel.selectedTags.contains(tagName),
                        onClick = { viewModel.toggleTag(tagName) },
                        label = { Text(tagName) },
                        shape = MaterialTheme.shapes.small
                    )
                }
            }

            OutlinedTextField(
                value = viewModel.newTagText,
                onValueChange = { viewModel.newTagText = it },
                label = { Text("Add New Tag") },
                modifier = Modifier.fillMaxWidth(),
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
                shape = MaterialTheme.shapes.medium
            )


            @OptIn(ExperimentalMaterial3Api::class)
            ExposedDropdownMenuBox(
                expanded = merchantExpanded && viewModel.merchantSuggestions.isNotEmpty(),
                onExpandedChange = { merchantExpanded = !merchantExpanded }
            ) {
                OutlinedTextField(
                    value = viewModel.merchant,
                    onValueChange = {
                        viewModel.merchant = it
                        merchantExpanded = true
                    },
                    label = { Text(stringResource(R.string.merchant_store)) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
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
                    shape = MaterialTheme.shapes.medium
                )

                ExposedDropdownMenu(
                    expanded = merchantExpanded && viewModel.merchantSuggestions.isNotEmpty(),
                    onDismissRequest = { merchantExpanded = false }
                ) {
                    viewModel.merchantSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                viewModel.merchant = suggestion
                                merchantExpanded = false
                            }
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                onClick = { viewModel.isRecurring = !viewModel.isRecurring }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = viewModel.isRecurring,
                        onCheckedChange = { viewModel.isRecurring = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.recurring_expenses),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
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
                        value = viewModel.recurrenceInterval,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Recurrence Interval") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recurrenceExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = MaterialTheme.shapes.medium
                    )
                    ExposedDropdownMenu(
                        expanded = recurrenceExpanded,
                        onDismissRequest = { recurrenceExpanded = false }
                    ) {
                        listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    viewModel.recurrenceInterval = type
                                    recurrenceExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                onClick = { viewModel.isInstallment = !viewModel.isInstallment }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = viewModel.isInstallment,
                        onCheckedChange = { viewModel.isInstallment = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.is_installment),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (viewModel.isInstallment) {
                OutlinedTextField(
                    value = viewModel.durationMonths,
                    onValueChange = { viewModel.durationMonths = it },
                    label = { Text(stringResource(R.string.duration_months)) },
                    modifier = Modifier.fillMaxWidth(),
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
                    shape = MaterialTheme.shapes.medium
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
                    .height(58.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = (if (viewModel.isEditMode) stringResource(R.string.update_transaction) else stringResource(
                        R.string.confirm_transaction
                    )).uppercase(), 
                    fontWeight = FontWeight.Black, 
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
