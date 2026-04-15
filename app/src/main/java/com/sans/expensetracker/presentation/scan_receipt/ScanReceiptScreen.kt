package com.sans.expensetracker.presentation.scan_receipt

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.expensetracker.R
import com.sans.expensetracker.core.util.CurrencyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReceiptScreen(
    onBack: () -> Unit,
    viewModel: ScanReceiptViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Gallery image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onEvent(ScanReceiptEvent.ImageSelected(context, uri))
        }
    }

    // Model file picker
    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onEvent(ScanReceiptEvent.ModelSelected(uri))
        }
    }

    // Camera capture — stores the output URI before launching the camera app
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { uri ->
                viewModel.onEvent(ScanReceiptEvent.ImageSelected(context, uri))
            }
        }
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "camera_receipt.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        val file = File(context.cacheDir, "camera_receipt.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        cameraImageUri = uri
        if (context.checkSelfPermission(android.Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            cameraLauncher.launch(uri)
        } else {
            cameraPermLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_receipt)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.suggestedTransactions.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (!state.isSaving) {
                            viewModel.onEvent(ScanReceiptEvent.SaveAcceptedTransactions)
                        }
                    },
                    icon = {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    },
                    text = { Text(if (state.isSaving) stringResource(R.string.saving) else stringResource(R.string.save_selected)) }
                )
            }
        }
    ) { paddingValues ->
        // Trigger caching when a model URI is selected but not yet cached
        LaunchedEffect(state.modelUri) {
            state.modelUri?.let { uri ->
                if (state.cachedModelPath == null) {
                    viewModel.onEvent(ScanReceiptEvent.CacheModelFile(context, uri))
                }
            }
        }

        // Navigate back after successful save
        LaunchedEffect(state.isSaved) {
            if (state.isSaved) onBack()
        }

        if (state.suggestedTransactions.isNotEmpty()) {
            // ── Results list ────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                state.aiThinking?.let { thinking ->
                    item { AiThinkingCard(thinkingText = thinking) }
                }

                item {
                    Text(
                        stringResource(R.string.found_transactions, state.suggestedTransactions.size),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(
                    count = state.suggestedTransactions.size,
                    key = { index -> state.suggestedTransactions[index].id }
                ) { index ->
                    val tx = state.suggestedTransactions[index]
                    SuggestedTransactionCard(
                        transaction = tx,
                        availableCategories = state.availableCategories,
                        onToggle = { viewModel.onEvent(ScanReceiptEvent.ToggleTransactionAcceptance(tx.id)) },
                        onDelete = { viewModel.onEvent(ScanReceiptEvent.DeleteTransaction(tx.id)) },
                        onEditTitle = { viewModel.onEvent(ScanReceiptEvent.EditTransactionTitle(tx.id, it)) },
                        onEditAmount = { viewModel.onEvent(ScanReceiptEvent.EditTransactionAmount(tx.id, it)) },
                        onEditCategory = { viewModel.onEvent(ScanReceiptEvent.EditTransactionCategory(tx.id, it)) },
                        onEditDate = { viewModel.onEvent(ScanReceiptEvent.EditTransactionDate(tx.id, it)) }
                    )
                }

                item {
                    OutlinedButton(
                        onClick = { viewModel.onEvent(ScanReceiptEvent.ResetForNewScan) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.scan_another_receipt))
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) } // FAB clearance
            }
        } else {
            // ── Selection / loading / empty UI ──────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when {
                    // Step 1: no model selected yet
                    state.modelUri == null && state.cachedModelPath == null -> {
                        Text(
                            stringResource(R.string.step1_select_model),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.step1_model_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { modelPickerLauncher.launch("*/*") }) {
                            Text(stringResource(R.string.select_model))
                        }
                    }

                    // Caching model
                    state.modelUri != null && state.cachedModelPath == null -> {
                        CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 6.dp)
                        Spacer(Modifier.height(24.dp))
                        Text(
                            stringResource(R.string.caching_model),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Processing: inference in progress
                    state.isProcessing -> {
                        // Thumbnail of the selected image
                        val thumbnailBitmap by produceState<android.graphics.Bitmap?>(
                            initialValue = null,
                            key1 = state.preprocessedImagePath
                        ) {
                            value = state.preprocessedImagePath?.let { path ->
                                withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path) }
                            }
                        }
                        thumbnailBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.height(16.dp))
                        }

                        CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 6.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.analyzing_receipt),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = { viewModel.onEvent(ScanReceiptEvent.CancelInference) }) {
                            Text(stringResource(R.string.cancel))
                        }
                        Spacer(Modifier.height(8.dp))

                        if (state.streamingText.isNotEmpty()) {
                            val scrollState = rememberScrollState()
                            LaunchedEffect(state.streamingText) {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .padding(horizontal = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(
                                    text = state.streamingText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                        .verticalScroll(scrollState)
                                )
                            }
                        } else {
                            Text(
                                stringResource(R.string.first_run_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // No items found (empty result after processing)
                    state.noResultsFound -> {
                        Text("🧾", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.no_transactions_found),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.no_transactions_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))
                        ImagePickerButtons(
                            onGallery = { imagePickerLauncher.launch("image/*") },
                            onCamera = ::launchCamera
                        )
                    }

                    // Step 2: model ready, waiting for image (or error recovery)
                    else -> {
                        if (state.errorMessage == null) {
                            Text(
                                stringResource(R.string.step2_select_image),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(
                                    R.string.model_ready,
                                    state.cachedModelPath?.substringAfterLast("/") ?: ""
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(24.dp))
                            ImagePickerButtons(
                                onGallery = { imagePickerLauncher.launch("image/*") },
                                onCamera = ::launchCamera
                            )
                        }

                        state.errorMessage?.let { errorMsg ->
                            Spacer(Modifier.height(24.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        stringResource(R.string.inference_error),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        errorMsg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (state.imageUri != null) {
                                    OutlinedButton(
                                        onClick = {
                                            state.imageUri?.let { uri ->
                                                viewModel.onEvent(ScanReceiptEvent.ImageSelected(context, uri))
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.retry))
                                    }
                                }
                                Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                                    Text(stringResource(R.string.new_image))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagePickerButtons(
    onGallery: () -> Unit,
    onCamera: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onCamera,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.take_photo))
        }
        Button(
            onClick = onGallery,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Photo, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.select_image_gallery))
        }
    }
}

@Composable
fun AiThinkingCard(thinkingText: String) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (expanded) stringResource(R.string.hide_ai_thinking) else stringResource(R.string.view_ai_thinking))
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = thinkingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestedTransactionCard(
    transaction: SuggestedTransaction,
    availableCategories: List<String>,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEditTitle: (String) -> Unit,
    onEditAmount: (Long) -> Unit,
    onEditCategory: (String) -> Unit,
    onEditDate: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    // Local edit states — keyed to the card's identity via LazyColumn key={tx.id}
    var titleText by remember { mutableStateOf(transaction.title) }
    var amountText by remember { mutableStateOf((transaction.amount / 100).toString()) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = transaction.dateString?.let { ds ->
            runCatching {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(ds)?.time
            }.getOrNull()
        } ?: System.currentTimeMillis()
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (transaction.isAccepted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        // ── Collapsed header ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = transaction.isAccepted,
                onCheckedChange = { onToggle() }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onToggle)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                transaction.dateString?.let { dateStr ->
                    Text(
                        text = stringResource(R.string.date_label, dateStr),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = CurrencyFormatter.formatAmount(transaction.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        // ── Inline edit panel ─────────────────────────────────────────────
        if (expanded) {
            HorizontalDivider()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it; onEditTitle(it) },
                    label = { Text(stringResource(R.string.edit_transaction_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newText ->
                        amountText = newText
                        newText.toLongOrNull()?.let { onEditAmount(it * 100) }
                    },
                    label = { Text(stringResource(R.string.amount_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                if (availableCategories.isNotEmpty()) {
                    Text(
                        stringResource(R.string.category),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableCategories) { cat ->
                            FilterChip(
                                selected = cat.equals(transaction.category, ignoreCase = true),
                                onClick = { onEditCategory(cat) },
                                label = { Text(cat) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                val displayDate = transaction.dateString
                    ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = displayDate,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.date)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val formatted = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }.format(Date(millis))
                        onEditDate(formatted)
                    }
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
