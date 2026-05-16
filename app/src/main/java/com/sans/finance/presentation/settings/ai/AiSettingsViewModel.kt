package com.sans.finance.presentation.settings.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.ai.AiProviderType
import com.sans.finance.data.ai.AiSettings
import com.sans.finance.data.ai.AiSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val repo: AiSettingsRepository
) : ViewModel() {

    val state: StateFlow<AiSettings> = repo.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AiSettings()
    )

    fun setProvider(provider: AiProviderType) {
        viewModelScope.launch { repo.setProvider(provider) }
    }

    fun setOpenAiApiKey(value: String) {
        viewModelScope.launch { repo.setOpenAiApiKey(value) }
    }

    fun setOpenAiModel(value: String) {
        viewModelScope.launch { repo.setOpenAiModel(value) }
    }

    fun setOpenRouterApiKey(value: String) {
        viewModelScope.launch { repo.setOpenRouterApiKey(value) }
    }

    fun setOpenRouterModel(value: String) {
        viewModelScope.launch { repo.setOpenRouterModel(value) }
    }
}

