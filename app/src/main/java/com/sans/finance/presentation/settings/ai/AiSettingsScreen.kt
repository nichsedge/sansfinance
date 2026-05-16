package com.sans.finance.presentation.settings.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sans.finance.data.ai.AiProviderType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    onBack: () -> Unit,
    viewModel: AiSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI (Bring Your Own Key)", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Privacy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "AI is optional. Your API keys are stored encrypted on-device. When enabled, monthly review summaries will send aggregates (not your full DB).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text("Provider", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ProviderRow(
                label = "Off",
                selected = state.provider == AiProviderType.OFF,
                onSelect = { viewModel.setProvider(AiProviderType.OFF) }
            )
            ProviderRow(
                label = "OpenAI",
                selected = state.provider == AiProviderType.OPENAI,
                onSelect = { viewModel.setProvider(AiProviderType.OPENAI) }
            )
            ProviderRow(
                label = "OpenRouter",
                selected = state.provider == AiProviderType.OPENROUTER,
                onSelect = { viewModel.setProvider(AiProviderType.OPENROUTER) }
            )

            Spacer(Modifier.height(8.dp))

            if (state.provider == AiProviderType.OPENAI) {
                Text("OpenAI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = state.openAiModel,
                    onValueChange = viewModel::setOpenAiModel,
                    label = { Text("Model") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Example: gpt-5.4-mini") }
                )
                OutlinedTextField(
                    value = state.openAiApiKey,
                    onValueChange = viewModel::setOpenAiApiKey,
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                Text(
                    "Keys are never committed to backups unless you export them manually.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.provider == AiProviderType.OPENROUTER) {
                Text("OpenRouter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = state.openRouterModel,
                    onValueChange = viewModel::setOpenRouterModel,
                    label = { Text("Model") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Example: openai/gpt-4.1-mini") }
                )
                OutlinedTextField(
                    value = state.openRouterApiKey,
                    onValueChange = viewModel::setOpenRouterApiKey,
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun ProviderRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

