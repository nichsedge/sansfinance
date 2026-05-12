package com.sans.finance.presentation.portfolio

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.local.dao.AssetClassTotal
import com.sans.finance.data.local.dao.CategoryTotal
import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.data.util.PortfolioJsonImporter
import com.sans.finance.domain.model.AssetClassHealth
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
    val importMessage: String? = null,
    val assetClassTotals: List<AssetClassTotal> = emptyList(),
    val healthList: List<AssetClassHealth> = emptyList(),
    val selectedTab: Int = 0
)

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val repository: PortfolioRepository,
    private val localeManager: LocaleManager,
    @param:ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _selectedDateIndex = MutableStateFlow(0)
    private val _importMessage = MutableStateFlow<String?>(null)
    private val _selectedTab = MutableStateFlow(0)

    val state: StateFlow<PortfolioScreenState> = combine(
        combine(
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
                    importMessage = importMsg,
                    isPrivacyModeEnabled = privacyMode
                )
            }

            val validIndex = dateIndex.coerceIn(0, dates.size - 1)
            val selectedDate = dates[validIndex]

            val holdings = repository.getSnapshotByDateSync(selectedDate)
            val categoryTotals =
                repository.getCategoryTotals(selectedDate).sortedByDescending { it.totalIdr }
            val assetClassTotals =
                repository.getAssetClassTotals(selectedDate).sortedByDescending { it.totalIdr }

            val totalValueIdr = assetClassTotals.sumOf { it.totalIdr }
            val healthList = calculateHealth(assetClassTotals, totalValueIdr)

            val sortedHoldingsByCategory = holdings.groupBy { it.category }
                .mapValues { entry -> entry.value.sortedByDescending { it.valueIdr } }
                .toList()
                .sortedByDescending { it.second.sumOf { h -> h.valueIdr } }
                .toMap()

            val previousDate = dates.getOrNull(validIndex + 1)
            val previousTotalIdr = if (previousDate != null) {
                history.find { it.snapshot_date == previousDate }?.totalIdr
            } else null

            val currentTotal = history.find { it.snapshot_date == selectedDate }

            PortfolioScreenState(
                holdings = holdings,
                holdingsByCategory = sortedHoldingsByCategory,
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
                isPrivacyModeEnabled = privacyMode,
                assetClassTotals = assetClassTotals,
                healthList = healthList
            )
        },
        _selectedTab
    ) { baseState, selectedTab ->
        baseState.copy(selectedTab = selectedTab)
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
                val (date, items, exchangeRate) = PortfolioJsonImporter.parse(context, uri)

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


    fun clearImportMessage() {
        _importMessage.value = null
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    private fun calculateHealth(
        totals: List<com.sans.finance.data.local.dao.AssetClassTotal>,
        totalValue: Double
    ): List<com.sans.finance.domain.model.AssetClassHealth> {
        if (totalValue <= 0) return emptyList()

        val targets = com.sans.finance.domain.model.PortfolioHealthDefaults.targets

        // Calculate health for targeted asset classes
        val targetedHealth = targets.map { target ->
            val currentTotal =
                totals.find { it.assetClass.equals(target.assetClass, ignoreCase = true) }?.totalIdr
                    ?: 0.0
            val currentPercentage = (currentTotal / totalValue) * 100.0
            val diff = currentPercentage - target.targetPercentage

            val status = when {
                diff > 5.0 -> com.sans.finance.domain.model.HealthStatus.OVERWEIGHT
                diff < -5.0 -> com.sans.finance.domain.model.HealthStatus.UNDERWEIGHT
                else -> com.sans.finance.domain.model.HealthStatus.HEALTHY
            }

            com.sans.finance.domain.model.AssetClassHealth(
                assetClass = target.assetClass,
                currentPercentage = currentPercentage,
                targetPercentage = target.targetPercentage,
                currentAmount = currentTotal,
                riskLevel = target.riskLevel,
                status = status,
                diffPercentage = diff
            )
        }

        // Find asset classes in data that are NOT in targets
        val untargetedHealth = totals.filter { total ->
            targets.none { it.assetClass.equals(total.assetClass, ignoreCase = true) }
        }.map { total ->
            val currentPercentage = (total.totalIdr / totalValue) * 100.0
            com.sans.finance.domain.model.AssetClassHealth(
                assetClass = total.assetClass,
                currentPercentage = currentPercentage,
                targetPercentage = 0.0,
                currentAmount = total.totalIdr,
                riskLevel = com.sans.finance.domain.model.RiskLevel.MEDIUM,
                status = com.sans.finance.domain.model.HealthStatus.OVERWEIGHT,
                diffPercentage = currentPercentage
            )
        }

        return (targetedHealth + untargetedHealth).sortedByDescending { it.currentPercentage }
    }

    fun togglePrivacyMode() {
        localeManager.setPrivacyModeEnabled(!localeManager.isPrivacyModeEnabled())
    }
}
