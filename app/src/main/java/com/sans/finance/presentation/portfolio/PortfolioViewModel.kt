package com.sans.finance.presentation.portfolio

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.local.dao.AssetClassTotal
import com.sans.finance.data.local.dao.CategoryTotal
import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.data.util.PortfolioJsonExporter
import com.sans.finance.data.util.PortfolioJsonImporter
import com.sans.finance.domain.model.AssetClassHealth
import com.sans.finance.domain.repository.GoalRepository
import com.sans.finance.domain.repository.PortfolioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    val selectedTab: Int = 0,
    val xirr: Double? = null,
    val goals: List<com.sans.finance.presentation.goals.GoalWithProgress> = emptyList()
)

private data class PortfolioData(
    val dates: List<Long>,
    val history: List<SnapshotTotal>,
    val dbTargets: List<com.sans.finance.data.local.entity.PortfolioTargetEntity>,
    val goals: List<com.sans.finance.data.local.entity.GoalEntity>,
    val dateIndex: Int,
    val importMsg: String?
)

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val repository: PortfolioRepository,
    private val goalRepository: GoalRepository,
    private val localeManager: LocaleManager,
    @param:ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _selectedDateIndex = MutableStateFlow(0)
    private val _importMessage = MutableStateFlow<String?>(null)
    private val _selectedTab = MutableStateFlow(0)
    private val _xirr = MutableStateFlow<Double?>(null)

    init {
        // Seed default targets if none exist
        viewModelScope.launch {
            repository.getPortfolioTargets().first().let {
                if (it.isEmpty()) {
                    com.sans.finance.domain.model.PortfolioHealthDefaults.targets.forEach { t ->
                        repository.updatePortfolioTarget(
                            com.sans.finance.data.local.entity.PortfolioTargetEntity(
                                assetClass = t.assetClass,
                                targetPercentage = t.targetPercentage,
                                description = t.description,
                                riskLevel = t.riskLevel.name
                            )
                        )
                    }
                }
            }
        }
    }

    val state: StateFlow<PortfolioScreenState> = combine(
        repository.getAllSnapshotDates(),
        repository.getTotalValueOverTime(),
        repository.getPortfolioTargets(),
        goalRepository.getAllGoals(),
        _selectedDateIndex,
        _importMessage,
        localeManager.privacyMode,
        _selectedTab,
        _xirr
    ) { args ->
        val dates = args[0] as List<Long>
        val history = args[1] as List<SnapshotTotal>
        val dbTargets = args[2] as List<com.sans.finance.data.local.entity.PortfolioTargetEntity>
        val goals = args[3] as List<com.sans.finance.data.local.entity.GoalEntity>
        val dateIndex = args[4] as Int
        val importMsg = args[5] as String?
        val privacyMode = args[6] as Boolean
        val selectedTab = args[7] as Int
        val xirrValue = args[8] as Double?

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

        // Trigger XIRR update
        updateXirr(selectedDate)

        val holdings = repository.getSnapshotByDateSync(selectedDate)
        val categoryTotals =
            repository.getCategoryTotals(selectedDate).sortedByDescending { it.totalIdr }
        val assetClassTotals =
            repository.getAssetClassTotals(selectedDate).sortedByDescending { it.totalIdr }

        val totalValueIdr = assetClassTotals.sumOf { it.totalIdr }
        val healthList = calculateHealth(assetClassTotals, totalValueIdr, dbTargets)

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

        val goalsWithProgress = if (holdings.isNotEmpty()) {
            val latestHeader = repository.getLatestSnapshotHeader().first()
            val exchangeRate = latestHeader?.exchangeRateUsd ?: 16000.0

            goals.map { goal ->
                val currentAmountIdr = when (goal.targetType) {
                    "TOTAL" -> holdings.sumOf { it.valueIdr }
                    "CATEGORY" -> holdings.filter { it.category == goal.targetName }
                        .sumOf { it.valueIdr }
                    "ASSET_CLASS" -> holdings.filter { it.assetClass == goal.targetName }
                        .sumOf { it.valueIdr }
                    else -> 0.0
                }

                val currentAmount = if (goal.currency == "USD") {
                    currentAmountIdr / exchangeRate
                } else {
                    currentAmountIdr
                }

                com.sans.finance.presentation.goals.GoalWithProgress(goal, currentAmount)
            }
        } else emptyList()

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
            healthList = healthList,
            xirr = xirrValue,
            selectedTab = selectedTab,
            goals = goalsWithProgress
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PortfolioScreenState()
    )

    private fun updateXirr(date: Long) {
        viewModelScope.launch {
            val value = repository.calculateXirr(date)
            _xirr.value = if (value.isNaN()) null else value
        }
    }

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

    fun exportFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val selectedDate = state.value.selectedDate ?: return@launch
                val holdings = repository.getSnapshotByDateSync(selectedDate)
                val jsonString = PortfolioJsonExporter.toSnapshotJson(selectedDate, holdings)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.bufferedWriter().use { it.write(jsonString) }
                }
                _importMessage.value = "Portfolio exported successfully"
            } catch (e: Exception) {
                _importMessage.value = "Export failed: ${e.message}"
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
        totalValue: Double,
        targets: List<com.sans.finance.data.local.entity.PortfolioTargetEntity>
    ): List<com.sans.finance.domain.model.AssetClassHealth> {
        if (totalValue <= 0) return emptyList()

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
                riskLevel = com.sans.finance.domain.model.RiskLevel.valueOf(target.riskLevel),
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

    fun updateTarget(assetClass: String, targetPercentage: Double) {
        viewModelScope.launch {
            repository.getPortfolioTargets().first().let { targets ->
                val existing = targets.find { it.assetClass == assetClass }
                repository.updatePortfolioTarget(
                    com.sans.finance.data.local.entity.PortfolioTargetEntity(
                        assetClass = assetClass,
                        targetPercentage = targetPercentage,
                        description = existing?.description ?: "",
                        riskLevel = existing?.riskLevel ?: "MEDIUM"
                    )
                )
            }
        }
    }
}
