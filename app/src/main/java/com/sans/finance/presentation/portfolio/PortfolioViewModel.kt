package com.sans.finance.presentation.portfolio

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.local.dao.AccountAliasDao
import com.sans.finance.data.local.dao.AssetClassTotal
import com.sans.finance.data.local.dao.CategoryTotal
import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.data.util.PortfolioJsonExporter
import com.sans.finance.data.util.PortfolioJsonImporter
import com.sans.finance.domain.model.AssetClassHealth
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.AccountTypeRepository
import com.sans.finance.domain.repository.GoalRepository
import com.sans.finance.domain.repository.PortfolioRepository
import com.sans.finance.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PortfolioScreenState(
    val holdings: List<PortfolioHoldingEntity> = emptyList(),
    val holdingsByCategory: Map<String, List<PortfolioHoldingEntity>> = emptyMap(),
    val valuedHoldings: List<com.sans.finance.domain.model.ValuedHolding> = emptyList(),
    val valuation: com.sans.finance.domain.model.MultiCurrencyPortfolioValuation? = null,
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val totalValueIdr: Double = 0.0,
    val totalValueUsd: Double = 0.0,
    val totalValueInBase: Double = 0.0,
    val totalHistoricalCostInBase: Double = 0.0,
    val totalFxGainInBase: Double = 0.0,
    val totalPriceGainInBase: Double = 0.0,
    val totalGainInBase: Double = 0.0,
    val totalGainPercentage: Double = 0.0,
    val hasCostBasis: Boolean = false,
    val currencyBreakdowns: List<com.sans.finance.domain.model.CurrencyValuationSummary> = emptyList(),
    val snapshotDates: List<Long> = emptyList(),
    val selectedDateIndex: Int = 0,
    val selectedDate: Long? = null,
    val valueHistory: List<SnapshotTotal> = emptyList(),
    val netWorthHistory: List<SnapshotTotal> = emptyList(),
    val chartMode: Int = 0,
    val currentCurrency: String = "IDR",
    val isLoading: Boolean = true,
    val previousTotalIdr: Double? = null,
    val isPrivacyModeEnabled: Boolean = false,
    val importMessage: String? = null,
    val assetClassTotals: List<AssetClassTotal> = emptyList(),
    val healthList: List<AssetClassHealth> = emptyList(),
    val selectedTab: Int = 0,
    val xirr: Double? = null,
    val goals: List<com.sans.finance.presentation.goals.GoalWithProgress> = emptyList(),
    val accountAliases: Map<String, String> = emptyMap(),
    val includedAccountCashIdr: Double = 0.0,
    val rebalanceSuggestions: List<com.sans.finance.domain.model.RebalanceAction> = emptyList(),
    val dividendSummary: com.sans.finance.domain.model.DividendYieldSummary? = null,
    val cashInjectionDepositAmount: Double = 0.0,
    val cashInjectionResult: com.sans.finance.domain.usecase.CashInjectionRebalanceResult? = null,
    val aiAnalysis: com.sans.finance.data.ai.PortfolioAnalysisResult? = null,
    val isAiAnalyzing: Boolean = false,
    val aiStreamingText: String = "",
    val benchmarkComparison: com.sans.finance.domain.model.PortfolioBenchmarkComparison? = null,
    val selectedBenchmark: com.sans.finance.domain.model.BenchmarkType = com.sans.finance.domain.model.BenchmarkType.SP500
)

data class PortfolioAiState(
    val isAnalyzing: Boolean = false,
    val streamingText: String = "",
    val analysis: com.sans.finance.data.ai.PortfolioAnalysisResult? = null
)

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val repository: PortfolioRepository,
    private val accountRepository: AccountRepository,
    private val accountTypeRepository: AccountTypeRepository,
    private val expenseRepository: com.sans.finance.domain.repository.ExpenseRepository,
    private val currencyDao: CurrencyDao,
    private val accountAliasDao: AccountAliasDao,
    private val goalRepository: GoalRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val localeManager: LocaleManager,
    private val getRebalanceSuggestionsUseCase: com.sans.finance.domain.usecase.GetRebalanceSuggestionsUseCase,
    private val getCashInjectionRebalanceUseCase: com.sans.finance.domain.usecase.GetCashInjectionRebalanceUseCase,
    private val getDividendYieldSummaryUseCase: com.sans.finance.domain.usecase.GetDividendYieldSummaryUseCase,
    private val getPortfolioBenchmarkComparisonUseCase: com.sans.finance.domain.usecase.GetPortfolioBenchmarkComparisonUseCase,
    private val valuatePortfolioUseCase: com.sans.finance.domain.usecase.ValuatePortfolioUseCase,
    private val aiProviderFactory: com.sans.finance.data.ai.AiProviderFactory,
    @param:ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _selectedDateIndex = MutableStateFlow(0)
    private val _importMessage = MutableStateFlow<String?>(null)
    private val _selectedTab = MutableStateFlow(0)
    private val _cashInjectionAmount = MutableStateFlow(0.0)
    private val _selectedBenchmark = MutableStateFlow(com.sans.finance.domain.model.BenchmarkType.SP500)
    private val _chartMode = MutableStateFlow(0)
    private val _xirr = MutableStateFlow<Double?>(null)
    private val _aiState = MutableStateFlow(PortfolioAiState())
    private var aiStreamingJob: kotlinx.coroutines.Job? = null
    private val _sovereignAdvisor = MutableStateFlow<com.sans.finance.data.util.SovereignAdvisorJson?>(null)
    val sovereignAdvisor: StateFlow<com.sans.finance.data.util.SovereignAdvisorJson?> = _sovereignAdvisor.asStateFlow()

    init {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val file = java.io.File(context.filesDir, "latest_advisor.json")
                if (file.exists()) {
                    val parser = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    _sovereignAdvisor.value = parser.decodeFromString(com.sans.finance.data.util.SovereignAdvisorJson.serializer(), file.readText())
                }
            } catch (_: Exception) {}
        }
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
        userPreferencesRepository.userPreferences.map { it.isPrivacyModeEnabled },
        _selectedTab,
        _cashInjectionAmount,
        _chartMode,
        _xirr,
        _aiState,
        accountRepository.getAllAccounts(),
        accountTypeRepository.getAllAccountTypes(),
        currencyDao.getAllRates(),
        accountAliasDao.getAllAliases(),
        expenseRepository.getExpensesBetween(0, Long.MAX_VALUE),
        getDividendYieldSummaryUseCase(),
        _selectedBenchmark
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val dates = args[0] as List<Long>
        @Suppress("UNCHECKED_CAST")
        val history = args[1] as List<SnapshotTotal>
        @Suppress("UNCHECKED_CAST")
        val dbTargets = args[2] as List<com.sans.finance.data.local.entity.PortfolioTargetEntity>
        @Suppress("UNCHECKED_CAST")
        val goals = args[3] as List<com.sans.finance.domain.model.Goal>
        val dateIndex = args[4] as Int
        val importMsg = args[5] as String?
        val privacyMode = args[6] as Boolean
        val selectedTab = args[7] as Int
        val cashInjectionDeposit = args[8] as Double
        val chartMode = args[9] as Int
        val xirrValue = args[10] as Double?
        val aiState = args[11] as PortfolioAiState
        @Suppress("UNCHECKED_CAST")
        val accounts = args[12] as List<com.sans.finance.data.local.entity.AccountEntity>
        @Suppress("UNCHECKED_CAST")
        val accountTypes = args[13] as List<com.sans.finance.data.local.entity.AccountTypeEntity>
        @Suppress("UNCHECKED_CAST")
        val rates = args[14] as List<com.sans.finance.data.local.entity.ExchangeRateEntity>
        @Suppress("UNCHECKED_CAST")
        val aliases = args[15] as List<com.sans.finance.data.local.entity.AccountAliasEntity>
        @Suppress("UNCHECKED_CAST")
        val allExpenses = args[16] as List<com.sans.finance.domain.model.Expense>
        val dividendSummary = args[17] as com.sans.finance.domain.model.DividendYieldSummary
        val selectedBenchmark = args[18] as com.sans.finance.domain.model.BenchmarkType

        val currency = localeManager.getCurrency()

        if (dates.isEmpty()) {
            return@combine PortfolioScreenState(
                currentCurrency = currency,
                isLoading = false,
                importMessage = importMsg,
                isPrivacyModeEnabled = privacyMode,
                chartMode = chartMode
            )
        }

        val validIndex = dateIndex.coerceIn(0, dates.size - 1)
        val selectedDate = dates[validIndex]

        updateXirr(selectedDate)

        val holdings = repository.getSnapshotByDateSync(selectedDate)
        val liabilityTypeNames = accountTypes.filter { it.isLiability }.map { it.name }.toSet()
        val investmentTypeNames = accountTypes.filter { it.isInvestment }.map { it.name }.toSet()
        val ratesMap = rates.associate { it.code to it.rateToIdr }
        val accountCashHoldings = accounts
            .filter { it.type !in liabilityTypeNames }
            .mapIndexed { index, account ->
                val amount = account.balance / 100.0
                val rateToIdr = if (account.currency == "IDR") 1.0 else (ratesMap[account.currency] ?: 1.0)
                val valueIdr = amount * rateToIdr
                val isInv = account.type in investmentTypeNames
                val assetClass = if (isInv) {
                    if (account.type.contains("P2P", ignoreCase = true)) "Fixed Income" else "Investment"
                } else {
                    "Cash & Equivalents"
                }
                PortfolioHoldingEntity(
                    id = -((index + 1L) * 100000L + (account.id.takeIf { it > 0 } ?: 0L)),
                    snapshotDate = selectedDate,
                    source = if (isInv) account.type else "Accounts",
                    category = account.type,
                    asset = account.name,
                    currency = account.currency,
                    quantity = amount,
                    price = if (account.currency == "IDR") 1.0 else null,
                    valueIdr = valueIdr,
                    assetClass = assetClass,
                    accountId = account.id,
                    accountKey = "account:${account.id}",
                    accountName = account.name,
                    account = account.name,
                    details = if (isInv) "From ${account.type} account" else "From account balance"
                )
            }
            .filter { it.valueIdr != 0.0 }

        val consolidatedHoldings = holdings + accountCashHoldings

        val valuation = valuatePortfolioUseCase(
            holdings = consolidatedHoldings,
            baseCurrency = currency,
            snapshotDate = selectedDate
        )

        val valuedMap = valuation.valuedHoldings.associateBy { it.holding.id }

        val categoryTotals = valuation.valuedHoldings
            .groupBy { it.holding.category }
            .map { (category, items) ->
                CategoryTotal(category = category, totalIdr = items.sumOf { it.currentValueInBase }, totalUsd = 0.0)
            }
            .sortedByDescending { it.totalIdr }
        val assetClassTotals = valuation.valuedHoldings
            .groupBy { it.holding.assetClass }
            .map { (assetClass, items) ->
                AssetClassTotal(assetClass = assetClass, totalIdr = items.sumOf { it.currentValueInBase })
            }
            .sortedByDescending { it.totalIdr }

        val totalValueInBase = valuation.totalValueInBase
        val healthList = calculateHealth(assetClassTotals, totalValueInBase, dbTargets)

        val sortedHoldingsByCategory = consolidatedHoldings.groupBy { it.category }
            .mapValues { entry ->
                entry.value.sortedByDescending { h ->
                    valuedMap[h.id]?.currentValueInBase ?: h.valueIdr
                }
            }
            .toList()
            .sortedByDescending { it.second.sumOf { h -> valuedMap[h.id]?.currentValueInBase ?: h.valueIdr } }
            .toMap()
        val rebalanceSuggestions = getRebalanceSuggestionsUseCase(healthList)
        val cashInjectionResult = if (cashInjectionDeposit > 0) {
            getCashInjectionRebalanceUseCase(healthList, cashInjectionDeposit)
        } else null

        val currentTotal = history.find { it.snapshot_date == selectedDate }
        val latestHeader = repository.getLatestSnapshotHeader().first()
        val exchangeRate = latestHeader?.exchangeRateUsd ?: 16000.0

        val goalsWithProgress = if (consolidatedHoldings.isNotEmpty()) {
            goals.map { goal ->
                val currentAmount = when (goal.targetType) {
                    "TOTAL" -> totalValueInBase
                    "CATEGORY" -> valuation.valuedHoldings.filter { it.holding.category == goal.targetName }
                        .sumOf { it.currentValueInBase }
                    "ASSET_CLASS" -> valuation.valuedHoldings.filter { it.holding.assetClass == goal.targetName }
                        .sumOf { it.currentValueInBase }
                    else -> 0.0
                }

                com.sans.finance.presentation.goals.GoalWithProgress(goal, currentAmount)
            }
        } else emptyList()

        val baseRate = if (currency == "IDR") 1.0 else (ratesMap[currency] ?: 1.0)

        val nonLiabilityAccounts = accounts.filter { it.type !in liabilityTypeNames }
        val nonLiabilityAccountIds = nonLiabilityAccounts.map { it.id }.toSet()
        val currentAccountBalances = nonLiabilityAccounts.associate { it.id to it.balance }

        val relevantTransactions = allExpenses
            .filter { it.accountId in nonLiabilityAccountIds || (it.toAccountId != null && it.toAccountId in nonLiabilityAccountIds) }
            .sortedByDescending { it.date }

        val netWorthHistory = if (history.isEmpty()) {
            emptyList()
        } else {
            val sortedSnapshots = history.sortedByDescending { it.snapshot_date }
            val balancesRunning = currentAccountBalances.toMutableMap()
            var txIndex = 0

            val resultList = mutableListOf<SnapshotTotal>()
            for (snapshot in sortedSnapshots) {
                val snapshotDate = snapshot.snapshot_date
                while (txIndex < relevantTransactions.size && relevantTransactions[txIndex].date > snapshotDate) {
                    val tx = relevantTransactions[txIndex]
                    when (tx.type.uppercase()) {
                        "EXPENSE" -> {
                            val cur = balancesRunning[tx.accountId]
                            if (cur != null) balancesRunning[tx.accountId] = cur + tx.amount
                        }
                        "INCOME" -> {
                            val cur = balancesRunning[tx.accountId]
                            if (cur != null) balancesRunning[tx.accountId] = cur - tx.amount
                        }
                        "TRANSFER" -> {
                            val fromBal = balancesRunning[tx.accountId]
                            if (fromBal != null) balancesRunning[tx.accountId] = fromBal + tx.amount
                            val toAccId = tx.toAccountId
                            if (toAccId != null) {
                                val toBal = balancesRunning[toAccId]
                                if (toBal != null) balancesRunning[toAccId] = toBal - tx.amount
                            }
                        }
                    }
                    txIndex++
                }

                val totalCashAtSnapshotIdr = nonLiabilityAccounts.sumOf { account ->
                    val balanceCents = balancesRunning[account.id] ?: 0L
                    val amount = balanceCents / 100.0
                    val rateToIdr = if (account.currency == "IDR") 1.0 else (ratesMap[account.currency] ?: 1.0)
                    amount * rateToIdr
                }

                val totalNetWorthIdr = snapshot.totalIdr + totalCashAtSnapshotIdr
                val totalNetWorthBase = if (baseRate > 0) totalNetWorthIdr / baseRate else totalNetWorthIdr
                val rate = if (snapshot.totalUsd > 0) snapshot.totalIdr / snapshot.totalUsd else exchangeRate
                val totalNetWorthUsd = totalNetWorthIdr / rate

                resultList.add(
                    SnapshotTotal(
                        snapshot_date = snapshotDate,
                        totalIdr = totalNetWorthBase,
                        totalUsd = totalNetWorthUsd
                    )
                )
            }
            val resultMap = resultList.associateBy { it.snapshot_date }
            history.map { resultMap[it.snapshot_date] ?: it }
        }

        val totalValueIdr = assetClassTotals.sumOf { it.totalIdr }
        val convertedHistory = history.map { it.copy(totalIdr = if (baseRate > 0) it.totalIdr / baseRate else it.totalIdr) }
        val benchmarkComparison = getPortfolioBenchmarkComparisonUseCase(convertedHistory, selectedBenchmark)

        val previousDate = dates.getOrNull(validIndex + 1)
        val previousTotalInBase = if (previousDate != null) {
            if (accountCashHoldings.isNotEmpty()) {
                netWorthHistory.find { it.snapshot_date == previousDate }?.totalIdr
            } else {
                history.find { it.snapshot_date == previousDate }?.let { if (baseRate > 0) it.totalIdr / baseRate else it.totalIdr }
            }
        } else null

        PortfolioScreenState(
            holdings = consolidatedHoldings,
            holdingsByCategory = sortedHoldingsByCategory,
            valuedHoldings = valuation.valuedHoldings,
            valuation = valuation,
            categoryTotals = categoryTotals,
            totalValueIdr = totalValueIdr,
            totalValueUsd = if (exchangeRate > 0) totalValueIdr / exchangeRate else (currentTotal?.totalUsd ?: 0.0),
            totalValueInBase = totalValueInBase,
            totalHistoricalCostInBase = valuation.totalHistoricalCostInBase,
            totalFxGainInBase = valuation.totalFxGainInBase,
            totalPriceGainInBase = valuation.totalPriceGainInBase,
            totalGainInBase = valuation.totalGainInBase,
            totalGainPercentage = valuation.totalGainPercentage,
            hasCostBasis = valuation.hasCostBasis,
            currencyBreakdowns = valuation.currencyBreakdowns.values.toList(),
            snapshotDates = dates,
            selectedDateIndex = validIndex,
            selectedDate = selectedDate,
            valueHistory = history.map { it.copy(totalIdr = if (baseRate > 0) it.totalIdr / baseRate else it.totalIdr) },
            netWorthHistory = netWorthHistory,
            chartMode = chartMode,
            currentCurrency = currency,
            isLoading = false,
            importMessage = importMsg,
            previousTotalIdr = previousTotalInBase,
            isPrivacyModeEnabled = privacyMode,
            assetClassTotals = assetClassTotals,
            healthList = healthList,
            xirr = xirrValue,
            selectedTab = selectedTab,
            goals = goalsWithProgress,
            accountAliases = aliases.associate { it.accountKey to it.aliasName },
            includedAccountCashIdr = accountCashHoldings.sumOf { it.valueIdr },
            rebalanceSuggestions = rebalanceSuggestions,
            dividendSummary = dividendSummary,
            cashInjectionDepositAmount = cashInjectionDeposit,
            cashInjectionResult = cashInjectionResult,
            aiAnalysis = aiState.analysis,
            isAiAnalyzing = aiState.isAnalyzing,
            aiStreamingText = aiState.streamingText,
            benchmarkComparison = benchmarkComparison,
            selectedBenchmark = selectedBenchmark
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PortfolioScreenState()
    )

    fun selectBenchmark(benchmark: com.sans.finance.domain.model.BenchmarkType) {
        _selectedBenchmark.value = benchmark
    }

    fun updateCashInjectionDeposit(amount: Double) {
        _cashInjectionAmount.value = amount
    }

    fun setChartMode(mode: Int) {
        _chartMode.value = mode
    }

    private var lastCalculatedXirrDate: Long? = null

    private fun updateXirr(date: Long) {
        if (lastCalculatedXirrDate == date) return
        lastCalculatedXirrDate = date
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
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
                val content = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                    ?: throw Exception("Could not read file")

                val (date, items, exchangeRate) = if (content.trim().startsWith("{") || content.trim().startsWith("[")) {
                    com.sans.finance.data.util.PortfolioJsonImporter.parseContent(content).toTriple()
                } else {
                    com.sans.finance.data.util.PortfolioCsvParser.parse(content)
                }

                if (items.isEmpty()) {
                    _importMessage.value = "No valid holdings found in file"
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

    fun syncFromGcs() {
        syncFromCloud()
    }

    fun syncFromCloud() {
        viewModelScope.launch {
            try {
                val prefs = userPreferencesRepository.userPreferences.first()
                val provider = com.sans.finance.data.util.CloudStorageSyncer.getActiveProvider(prefs)
                val providerLabel = if (provider == com.sans.finance.data.util.CloudStorageProvider.CLOUDFLARE_R2) "Cloudflare R2" else "GCS"
                _importMessage.value = "Connecting to $providerLabel..."
                val (date, items, exchangeRate, advisor) = com.sans.finance.data.util.CloudStorageSyncer.downloadLatestSnapshot(context, prefs)

                if (items.isEmpty()) {
                    _importMessage.value = "No valid entries found in $providerLabel"
                    return@launch
                }
                repository.importSnapshot(date, items, exchangeRate)
                if (advisor != null) {
                    _sovereignAdvisor.value = advisor
                    try {
                        val file = java.io.File(context.filesDir, "latest_advisor.json")
                        file.writeText(kotlinx.serialization.json.Json.encodeToString(com.sans.finance.data.util.SovereignAdvisorJson.serializer(), advisor))
                    } catch (_: Exception) {}
                }
                _selectedDateIndex.value = 0
                _importMessage.value = "Synced ${items.size} holdings from $providerLabel"
            } catch (e: Exception) {
                _importMessage.value = "Sync failed: ${e.message}"
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
        totals: List<AssetClassTotal>,
        totalValue: Double,
        targets: List<com.sans.finance.data.local.entity.PortfolioTargetEntity>
    ): List<AssetClassHealth> {
        if (totalValue <= 0) return emptyList()

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

            AssetClassHealth(
                assetClass = target.assetClass,
                currentPercentage = currentPercentage,
                targetPercentage = target.targetPercentage,
                currentAmount = currentTotal,
                riskLevel = com.sans.finance.domain.model.RiskLevel.valueOf(target.riskLevel),
                status = status,
                diffPercentage = diff
            )
        }

        val untargetedHealth = totals.filter { total ->
            targets.none { it.assetClass.equals(total.assetClass, ignoreCase = true) }
        }.map { total ->
            val currentPercentage = (total.totalIdr / totalValue) * 100.0
            AssetClassHealth(
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
        viewModelScope.launch {
            userPreferencesRepository.setPrivacyModeEnabled(!state.value.isPrivacyModeEnabled)
        }
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

    fun analyzePortfolioWithAi() {
        val currentState = state.value
        if (currentState.holdings.isEmpty() || currentState.isAiAnalyzing) return

        // Auto-navigate to Health content tab immediately
        _selectedTab.value = 1

        aiStreamingJob?.cancel()
        _aiState.value = PortfolioAiState(isAnalyzing = true, streamingText = "", analysis = null)

        aiStreamingJob = viewModelScope.launch {
            try {
                val provider = aiProviderFactory.create() ?: throw Exception("AI Provider belum dikonfigurasi. Atur API key di Pengaturan > AI Settings.")

                val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                val dateLabel = currentState.selectedDate?.let { dateFormat.format(java.util.Date(it)) } ?: "Current"

                val input = com.sans.finance.data.ai.PortfolioAnalysisInput(
                    dateLabel = dateLabel,
                    currency = currentState.currentCurrency,
                    totalValue = currentState.totalValueInBase,
                    assetAllocation = currentState.assetClassTotals.map { it.assetClass to (if (currentState.totalValueInBase > 0) it.totalIdr / currentState.totalValueInBase * 100.0 else 0.0) },
                    healthStatus = currentState.healthList.map { "${it.assetClass}: ${it.status} (target: ${it.targetPercentage}%, aktual: ${String.format(java.util.Locale.US, "%.1f", it.currentPercentage)}%)" },
                    xirr = currentState.xirr,
                    goals = currentState.goals.map { "${it.goal.name}: ${String.format(java.util.Locale.US, "%.1f%%", (it.currentAmount / it.goal.targetAmount * 100))}" },
                    notes = "Portfolio rebalancing target check."
                )

                provider.streamPortfolioAnalysis(input).collect { event ->
                    when (event) {
                        is com.sans.finance.domain.model.StreamEvent.TextDelta -> {
                            _aiState.update { current ->
                                current.copy(streamingText = current.streamingText + event.text)
                            }
                        }
                        is com.sans.finance.domain.model.StreamEvent.Done -> {
                            val parsed = parsePortfolioJsonResult(event.fullText) ?: com.sans.finance.data.ai.PortfolioAnalysisResult(
                                summary = "Rekomendasi Strategis Portofolio",
                                insights = emptyList(),
                                rawText = event.fullText
                            )
                            _aiState.value = PortfolioAiState(isAnalyzing = false, streamingText = "", analysis = parsed)
                        }
                        is com.sans.finance.domain.model.StreamEvent.Error -> {
                            _aiState.value = PortfolioAiState(isAnalyzing = false, streamingText = "", analysis = null)
                            _importMessage.value = "AI Analysis gagal: ${event.message}"
                        }
                    }
                }
            } catch (e: Exception) {
                _aiState.value = PortfolioAiState(isAnalyzing = false, streamingText = "", analysis = null)
                _importMessage.value = "AI Analysis gagal: ${e.message}"
            }
        }
    }

    private fun parsePortfolioJsonResult(text: String): com.sans.finance.data.ai.PortfolioAnalysisResult? {
        val clean = com.sans.finance.data.ai.AiJsonParser.extractJsonString(text)
        return runCatching {
            val obj = kotlinx.serialization.json.Json.parseToJsonElement(clean) as? kotlinx.serialization.json.JsonObject ?: return null
            val summary = obj["summary"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } ?: "Portfolio Analysis"
            val insightsJson = obj["insights"]
            val insights = if (insightsJson == null) {
                emptyList()
            } else {
                kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<List<com.sans.finance.data.ai.PortfolioAnalysisInsight>>(insightsJson.toString())
            }
            com.sans.finance.data.ai.PortfolioAnalysisResult(summary = summary, insights = insights, rawText = null)
        }.getOrNull()
    }

    fun stopAiAnalysis() {
        aiStreamingJob?.cancel()
        val currentText = _aiState.value.streamingText
        if (currentText.isNotBlank()) {
            _aiState.value = PortfolioAiState(
                isAnalyzing = false,
                streamingText = "",
                analysis = com.sans.finance.data.ai.PortfolioAnalysisResult(
                    summary = "Rekomendasi Strategis Portofolio",
                    insights = emptyList(),
                    rawText = currentText
                )
            )
        } else {
            _aiState.value = PortfolioAiState(isAnalyzing = false, streamingText = "", analysis = null)
        }
    }

    fun clearAiAnalysis() {
        aiStreamingJob?.cancel()
        _aiState.value = PortfolioAiState()
    }

    fun pruneMonthlySnapshots() {
        viewModelScope.launch {
            try {
                val pruned = repository.pruneSnapshotsMonthly()
                if (pruned > 0) {
                    _importMessage.value = "Pruned $pruned snapshot(s). Kept latest per month."
                    _selectedDateIndex.value = 0
                } else {
                    _importMessage.value = "Snapshots already clean (1 per month)."
                }
            } catch (e: Exception) {
                _importMessage.value = "Pruning failed: ${e.message}"
            }
        }
    }
}
