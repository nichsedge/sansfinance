package com.sans.finance.presentation.settings.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.ai.AiProviderType
import com.sans.finance.data.ai.AiSettings
import com.sans.finance.data.ai.AiSettingsRepository
import com.sans.finance.data.ai.OpenRouterModelRepository
import com.sans.finance.domain.model.AiModelInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val repo: AiSettingsRepository,
    private val openRouterModelRepo: OpenRouterModelRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AiSettings())
    val state: StateFlow<AiSettings> = _state.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _models = MutableStateFlow<List<AiModelInfo>>(emptyList())
    val models: StateFlow<List<AiModelInfo>> = _models.asStateFlow()

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels: StateFlow<Boolean> = _isLoadingModels.asStateFlow()

    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val initial = repo.settings.first()
            _state.value = initial
        }
        loadModels()
    }

    fun loadModels(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoadingModels.value = true
            try {
                val list = openRouterModelRepo.getModels(forceRefresh)
                _models.value = list
            } finally {
                _isLoadingModels.value = false
            }
        }
    }

    fun setProvider(provider: AiProviderType) {
        _state.update { it.copy(provider = provider) }
        viewModelScope.launch { repo.setProvider(provider) }
    }

    fun setOpenAiApiKey(value: String) {
        _state.update { it.copy(openAiApiKey = value) }
        scheduleSave()
    }

    fun setOpenAiModel(value: String) {
        _state.update { it.copy(openAiModel = value) }
        scheduleSave()
    }

    fun setOpenRouterApiKey(value: String) {
        _state.update { it.copy(openRouterApiKey = value) }
        scheduleSave()
    }

    fun setOpenRouterModel(value: String) {
        _state.update { it.copy(openRouterModel = value) }
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400) // Debounce 400ms to allow smooth 60fps/120fps typing without async roundtrips
            saveCurrentState()
        }
    }

    fun saveImmediately() {
        saveJob?.cancel()
        viewModelScope.launch {
            saveCurrentState()
        }
    }

    private suspend fun saveCurrentState() {
        _isSaving.value = true
        try {
            val current = _state.value
            repo.setOpenAiModel(current.openAiModel)
            repo.setOpenAiApiKey(current.openAiApiKey)
            repo.setOpenRouterModel(current.openRouterModel)
            repo.setOpenRouterApiKey(current.openRouterApiKey)
        } finally {
            _isSaving.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveJob?.cancel()
    }
}
