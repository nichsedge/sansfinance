package com.sans.finance.presentation.portfolio

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.local.dao.CategoryTotal
import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.data.util.PortfolioCsvImporter
import com.sans.finance.data.util.PortfolioJsonImporter
import com.sans.finance.domain.repository.PortfolioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PortfolioScreenState(
    val holdings: List<PortfolioHoldingEntity> = emptyList(),
    val holdingsByCategory: Map<String, List<PortfolioHoldingEntity>> = emptyMap(),
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val totalValueIdr: Double = 0.0,
    val totalValueUsd: Double = 0.0,
    val snapshotDates: List<Long> = emptyList(),
    val selectedDateIndex: Int = 0,
    val selectedDate: Long? = null,
    val valueHistory: List<SnapshotTotal> = emptyList(),
    val currentCurrency: String = "IDR",
    val isLoading: Boolean = true,
    val previousTotalIdr: Double? = null,
    val isPrivacyModeEnabled: Boolean = false,
    val importMessage: String? = null
)

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val repository: PortfolioRepository,
    private val localeManager: LocaleManager,
    @ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _selectedDateIndex = MutableStateFlow(0)
    private val _importMessage = MutableStateFlow<String?>(null)

    val state: StateFlow<PortfolioScreenState> = combine(
        repository.getAllSnapshotDates(),
        repository.getTotalValueOverTime(),
        _selectedDateIndex,
        _importMessage,
        localeManager.privacyMode
    ) { dates, history, dateIndex, importMsg, privacyMode ->
        val currency = localeManager.getCurrency()
        
        if (dates.isEmpty()) {
            return@combine PortfolioScreenState(
                currentCurrency = currency,
                isLoading = false,
                importMessage = importMsg
            )
        }

        val validIndex = dateIndex.coerceIn(0, dates.size - 1)
        val selectedDate = dates[validIndex]

        val holdings = repository.getSnapshotByDateSync(selectedDate)
        val categoryTotals = repository.getCategoryTotals(selectedDate)

        val previousDate = dates.getOrNull(validIndex + 1)
        val previousTotalIdr = if (previousDate != null) {
            history.find { it.snapshot_date == previousDate }?.totalIdr
        } else null

        val currentTotal = history.find { it.snapshot_date == selectedDate }

        PortfolioScreenState(
            holdings = holdings,
            holdingsByCategory = holdings.groupBy { it.category },
            categoryTotals = categoryTotals,
            totalValueIdr = currentTotal?.totalIdr ?: 0.0,
            totalValueUsd = currentTotal?.totalUsd ?: 0.0,
            snapshotDates = dates,
            selectedDateIndex = validIndex,
            selectedDate = selectedDate,
            valueHistory = history,
            currentCurrency = currency,
            isLoading = false,
            importMessage = importMsg,
            previousTotalIdr = previousTotalIdr,
            isPrivacyModeEnabled = privacyMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PortfolioScreenState()
    )

    fun selectDate(index: Int) {
        _selectedDateIndex.value = index
    }

    fun onPreviousSnapshot() {
        val current = _selectedDateIndex.value
        if (current < state.value.snapshotDates.size - 1) {
            _selectedDateIndex.value = current + 1
        }
    }

    fun onNextSnapshot() {
        val current = _selectedDateIndex.value
        if (current > 0) {
            _selectedDateIndex.value = current - 1
        }
    }

    fun importFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                val type = contentResolver.getType(uri)
                val fileName = getFileName(uri)

                val (date, items, exchangeRate) = when {
                    type == "application/json" || fileName?.endsWith(".json", ignoreCase = true) == true -> {
                        PortfolioJsonImporter.parse(context, uri)
                    }
                    else -> {
                        val (d, i) = PortfolioCsvImporter.parse(context, uri)
                        Triple(d, i, null)
                    }
                }

                if (items.isEmpty()) {
                    _importMessage.value = "No valid entries found in file"
                    return@launch
                }
                repository.importSnapshot(date, items, exchangeRate)
                _selectedDateIndex.value = 0 
                _importMessage.value = "Imported ${items.size} holdings"
            } catch (e: Exception) {
                _importMessage.value = "Import failed: ${e.message}"
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

    fun clearImportMessage() {
        _importMessage.value = null
    }

    fun togglePrivacyMode() {
        localeManager.setPrivacyModeEnabled(!localeManager.isPrivacyModeEnabled())
    }
}
