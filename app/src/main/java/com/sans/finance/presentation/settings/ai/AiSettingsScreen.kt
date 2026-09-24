package com.sans.finance.presentation.settings.ai

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sans.finance.data.ai.AiProviderType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiSettingsScreen(
    onBack: () -> Unit,
    viewModel: AiSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val models by viewModel.models.collectAsStateWithLifecycle()
    val isLoadingModels by viewModel.isLoadingModels.collectAsStateWithLifecycle()

    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var openRouterKeyVisible by remember { mutableStateOf(false) }
    var openAiKeyVisible by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }

    val openRouterPresets = remember {
        listOf(
            "openrouter/free",
            "meta-llama/llama-3.3-70b-instruct:free",
            "google/gemini-2.0-flash-001",
            "openai/gpt-4.1-mini",
            "deepseek/deepseek-chat",
            "anthropic/claude-3.5-haiku"
        )
    }

    val openAiPresets = remember {
        listOf(
            "gpt-5.4-mini",
            "gpt-4.1-mini",
            "gpt-4o-mini"
        )
    }

    val handleBack = {
        viewModel.saveImmediately()
        onBack()
    }

    BackHandler(onBack = handleBack)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("AI (Bring Your Own Key)", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Privasi & Keamanan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "AI bersifat opsional. Kunci API Anda disimpan terenkripsi di perangkat (Android KeyStore). Tidak ada data database lengkap yang diunggah ke cloud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text("Provider AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ProviderRow(
                label = "Nonaktif (Off)",
                description = "Fitur AI dinonaktifkan sepenuhnya.",
                selected = state.provider == AiProviderType.OFF,
                onSelect = { viewModel.setProvider(AiProviderType.OFF) }
            )
            ProviderRow(
                label = "OpenRouter (Direkomendasikan)",
                description = "Mendukung ratusan model (Gemini Flash, Claude, Llama, DeepSeek, model gratis).",
                selected = state.provider == AiProviderType.OPENROUTER,
                onSelect = { viewModel.setProvider(AiProviderType.OPENROUTER) }
            )
            ProviderRow(
                label = "OpenAI",
                description = "Langsung menggunakan OpenAI API (gpt-4o-mini, gpt-4.1-mini, dll).",
                selected = state.provider == AiProviderType.OPENAI,
                onSelect = { viewModel.setProvider(AiProviderType.OPENAI) }
            )

            Spacer(Modifier.height(8.dp))

            if (state.provider == AiProviderType.OPENROUTER) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Konfigurasi OpenRouter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = state.openRouterModel,
                            onValueChange = viewModel::setOpenRouterModel,
                            label = { Text("Model ID") },
                            placeholder = { Text("cth: openrouter/free, google/gemini-2.0-flash-001") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (state.openRouterModel.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setOpenRouterModel("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear model")
                                        }
                                    }
                                    IconButton(onClick = { showModelPicker = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Buka Katalog Model")
                                    }
                                }
                            }
                        )

                        OutlinedButton(
                            onClick = { showModelPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (models.isEmpty()) "Cari & Pilih dari Katalog Model..."
                                else "Cari & Pilih dari ${models.size} Model Katalog"
                            )
                        }

                        Text(
                            "Pilihan Cepat Model Populer:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            openRouterPresets.forEach { preset ->
                                val isSelected = state.openRouterModel == preset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setOpenRouterModel(preset) },
                                    label = {
                                        Text(preset.substringAfter("/"), style = MaterialTheme.typography.labelSmall)
                                    },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }

                        OutlinedTextField(
                            value = state.openRouterApiKey,
                            onValueChange = viewModel::setOpenRouterApiKey,
                            label = { Text("OpenRouter API Key") },
                            placeholder = { Text("sk-or-v1-...") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val clip = clipboardManager.getText()?.text
                                            if (!clip.isNullOrBlank()) {
                                                viewModel.setOpenRouterApiKey(clip.trim())
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("API key ditempel dari clipboard")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste API Key")
                                    }
                                    IconButton(onClick = { openRouterKeyVisible = !openRouterKeyVisible }) {
                                        Icon(
                                            if (openRouterKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (openRouterKeyVisible) "Sembunyikan" else "Tampilkan"
                                        )
                                    }
                                }
                            },
                            visualTransformation = if (openRouterKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                autoCorrectEnabled = false
                            ),
                            singleLine = true,
                            supportingText = {
                                Text("Key disimpan terenkripsi di Android KeyStore perangkat ini.")
                            }
                        )
                    }
                }
            }

            if (state.provider == AiProviderType.OPENAI) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Konfigurasi OpenAI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = state.openAiModel,
                            onValueChange = viewModel::setOpenAiModel,
                            label = { Text("Model ID") },
                            placeholder = { Text("gpt-4.1-mini") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                            trailingIcon = {
                                if (state.openAiModel.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setOpenAiModel("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear model")
                                    }
                                }
                            }
                        )

                        Text(
                            "Pilihan Model Populer:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            openAiPresets.forEach { preset ->
                                val isSelected = state.openAiModel == preset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setOpenAiModel(preset) },
                                    label = {
                                        Text(preset, style = MaterialTheme.typography.labelSmall)
                                    },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }

                        OutlinedTextField(
                            value = state.openAiApiKey,
                            onValueChange = viewModel::setOpenAiApiKey,
                            label = { Text("OpenAI API Key") },
                            placeholder = { Text("sk-...") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val clip = clipboardManager.getText()?.text
                                            if (!clip.isNullOrBlank()) {
                                                viewModel.setOpenAiApiKey(clip.trim())
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("API key ditempel dari clipboard")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste API Key")
                                    }
                                    IconButton(onClick = { openAiKeyVisible = !openAiKeyVisible }) {
                                        Icon(
                                            if (openAiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (openAiKeyVisible) "Sembunyikan" else "Tampilkan"
                                        )
                                    }
                                }
                            },
                            visualTransformation = if (openAiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                autoCorrectEnabled = false
                            ),
                            singleLine = true,
                            supportingText = {
                                Text("Key disimpan terenkripsi di Android KeyStore perangkat ini.")
                            }
                        )
                    }
                }
            }

            if (state.provider != AiProviderType.OFF) {
                Button(
                    onClick = {
                        viewModel.saveImmediately()
                        scope.launch {
                            snackbarHostState.showSnackbar("Pengaturan AI berhasil disimpan")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Simpan Pengaturan")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showModelPicker) {
        OpenRouterModelPickerBottomSheet(
            selectedModelId = state.openRouterModel,
            models = models,
            isLoading = isLoadingModels,
            onSelectModel = { selectedId ->
                viewModel.setOpenRouterModel(selectedId)
            },
            onRefresh = {
                viewModel.loadModels(forceRefresh = true)
            },
            onDismiss = {
                showModelPicker = false
            }
        )
    }
}

@Composable
private fun ProviderRow(
    label: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
