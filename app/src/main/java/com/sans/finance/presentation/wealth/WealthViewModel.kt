package com.sans.finance.presentation.wealth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sans.finance.core.util.DateFormatterUtils
import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.data.local.entity.AccountTypeEntity
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.data.local.entity.PortfolioSnapshotHeaderEntity
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.data.worker.CloudSyncAndBackupWorker
import com.sans.finance.domain.model.EmergencyFundStressTest
import com.sans.finance.domain.model.FinancialFreedomStats
import com.sans.finance.domain.model.Goal
import com.sans.finance.domain.model.SavingsRateVelocitySummary
import com.sans.finance.domain.model.UserPreferences
import com.sans.finance.domain.model.WealthDistributionTab
import com.sans.finance.domain.model.WealthMetrics
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.AccountTypeRepository
import com.sans.finance.domain.repository.GoalRepository
import com.sans.finance.domain.repository.PortfolioRepository
import com.sans.finance.domain.repository.UserPreferencesRepository
import com.sans.finance.domain.usecase.GetEmergencyFundStressTestUseCase
import com.sans.finance.domain.usecase.GetFinancialFreedomStatsUseCase
import com.sans.finance.domain.usecase.GetSavingsRateVelocityUseCase
import com.sans.finance.domain.usecase.GetWealthDistributionUseCase
import com.sans.finance.domain.usecase.GetWealthMetricsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class WealthState(
    val netWorth: Long = 0L,
    val totalAssets: Long = 0L,
    val cashAssets: Long = 0L,
    val liabilities: Long = 0L,
    val portfolioValue: Long = 0L,
    val monthlyBurn: Long = 0L,
    val runwayMonths: Double = 0.0,
    val monthlyPassiveIncome: Long = 0L,
    val annualPassiveIncome: Long = 0L,
    val nextPayoutDateStr: String = "10th of every month",
    val fiCoveragePct: Double = 0.0,
    val fiStage: String = "Foundation Stage (<25%)",
    val fiNextStageGap: Long = 0L,
    val financialFreedomYears: Double = 0.0,
    val financialFreedomScore: Float = 0f,
    val annualExpense: Long = 0L,
    val isFireManualEnabled: Boolean = false,
    val manualFireAnnualExpense: Long = 0L,
    val lastSnapshotDate: Long? = null,
    val portfolioSources: List<Pair<String, Int>> = emptyList(),
    val wealthDistribution: Map<String, Long> = emptyMap(),
    val wealthDistributionTab: WealthDistributionTab = WealthDistributionTab.ASSET_CLASS,
    val accountsCount: Int = 0,
    val activeDebtCount: Int = 0,
    val goalsCount: Int = 0,
    val avgGoalProgress: Float = 0f,
    val currencyCode: String = "IDR",
    val isPrivacyModeEnabled: Boolean = false,
    val emergencyStressTest: EmergencyFundStressTest? = null,
    val savingsVelocity: SavingsRateVelocitySummary? = null,
    val isSyncing: Boolean = false,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WealthViewModel @Inject constructor(
    private val portfolioRepository: PortfolioRepository,
    private val accountRepository: AccountRepository,
    private val accountTypeRepository: AccountTypeRepository,
    private val goalRepository: GoalRepository,
    private val getWealthMetricsUseCase: GetWealthMetricsUseCase,
    private val getWealthDistributionUseCase: GetWealthDistributionUseCase,
    private val getFinancialFreedomStatsUseCase: GetFinancialFreedomStatsUseCase,
    private val getEmergencyFundStressTestUseCase: GetEmergencyFundStressTestUseCase,
    private val getSavingsRateVelocityUseCase: GetSavingsRateVelocityUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val localeManager: LocaleManager
) : ViewModel() {

    private val _wealthDistributionTab = MutableStateFlow(WealthDistributionTab.ASSET_CLASS)
    private val _isSyncing = MutableStateFlow(false)

    private val distributionFlow = _wealthDistributionTab.flatMapLatest { tab ->
        getWealthDistributionUseCase(tab)
    }

    private val baseState = combine(
        getWealthMetricsUseCase(),
        getFinancialFreedomStatsUseCase(),
        portfolioRepository.getLatestSnapshotHeader(),
        portfolioRepository.getLatestSnapshot(),
        portfolioRepository.getTotalValueOverTime(),
        accountRepository.getAllAccounts(),
        accountTypeRepository.getAllAccountTypes(),
        goalRepository.getAllGoals(),
        userPreferencesRepository.userPreferences,
        _wealthDistributionTab,
        distributionFlow
    ) { args ->
        val metrics = args[0] as WealthMetrics
        val freedom = args[1] as FinancialFreedomStats
        val latestHeader = args[2] as PortfolioSnapshotHeaderEntity?
        @Suppress("UNCHECKED_CAST")
        val latestHoldings = args[3] as List<PortfolioHoldingEntity>
        @Suppress("UNCHECKED_CAST")
        val portfolioHistory = args[4] as List<SnapshotTotal>
        @Suppress("UNCHECKED_CAST")
        val accounts = args[5] as List<AccountEntity>
        @Suppress("UNCHECKED_CAST")
        val accountTypes = args[6] as List<AccountTypeEntity>
        @Suppress("UNCHECKED_CAST")
        val goals = args[7] as List<Goal>
        val prefs = args[8] as UserPreferences
        val tab = args[9] as WealthDistributionTab
        @Suppress("UNCHECKED_CAST")
        val distribution = args[10] as Map<String, Long>

        val sources = latestHoldings
            .groupBy { it.source }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }

        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val nextMonth = if (currentDay >= 10) calendar.get(Calendar.MONTH) + 1 else calendar.get(Calendar.MONTH)
        val nextYear = if (nextMonth > 11) calendar.get(Calendar.YEAR) + 1 else calendar.get(Calendar.YEAR)
        val adjMonth = if (nextMonth > 11) 0 else nextMonth
        val nextPayoutCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, nextYear)
            set(Calendar.MONTH, adjMonth)
            set(Calendar.DAY_OF_MONTH, 10)
        }
        val nextPayoutDateStr = "10 " + DateFormatterUtils.getMonthYearFormatter().format(nextPayoutCal.time)

        val liabilityTypeNames = accountTypes.filter { it.isLiability }.map { it.name }.toSet()
        val investmentTypeNames = accountTypes.filter { it.isInvestment }.map { it.name }.toSet()
        val nonLiabilityAccounts = accounts.filter { it.type !in liabilityTypeNames && it.type !in investmentTypeNames }
        val liabilityAccounts = accounts.filter { it.type in liabilityTypeNames }

        val latestPortfolioTotalIdr = portfolioHistory.lastOrNull()?.totalIdr ?: 0.0
        val avgGoalProg = if (goals.isNotEmpty()) {
            goals.map { goal ->
                val currentAmount = if (goal.targetType == "TOTAL") latestPortfolioTotalIdr else 0.0
                (currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
            }.average().toFloat()
        } else {
            0f
        }

        val totalAssets = metrics.cashAssets + metrics.portfolioValue
        val netWorth = totalAssets - metrics.liabilities

        WealthState(
            netWorth = netWorth,
            totalAssets = totalAssets,
            cashAssets = metrics.cashAssets,
            liabilities = metrics.liabilities,
            portfolioValue = metrics.portfolioValue,
            monthlyBurn = metrics.monthlyBurn,
            runwayMonths = metrics.runwayMonths,
            monthlyPassiveIncome = metrics.monthlyPassiveIncome,
            annualPassiveIncome = metrics.annualPassiveIncome,
            nextPayoutDateStr = nextPayoutDateStr,
            fiCoveragePct = metrics.fiCoveragePct,
            fiStage = metrics.fiStage,
            fiNextStageGap = metrics.fiNextStageGap,
            financialFreedomYears = freedom.yearsOfCover,
            financialFreedomScore = freedom.freedomScore,
            annualExpense = freedom.annualExpense,
            isFireManualEnabled = prefs.fireManualEnabled,
            manualFireAnnualExpense = prefs.manualFireAnnualExpense,
            lastSnapshotDate = latestHeader?.snapshotDate,
            portfolioSources = sources,
            wealthDistribution = distribution,
            wealthDistributionTab = tab,
            accountsCount = nonLiabilityAccounts.size,
            activeDebtCount = liabilityAccounts.size,
            goalsCount = goals.size,
            avgGoalProgress = avgGoalProg,
            currencyCode = metrics.currencyCode,
            isPrivacyModeEnabled = prefs.isPrivacyModeEnabled,
            isSyncing = false,
            isLoading = false
        )
    }

    val state: StateFlow<WealthState> = combine(
        baseState,
        _isSyncing,
        getEmergencyFundStressTestUseCase(),
        getSavingsRateVelocityUseCase()
    ) { base, isSyncing, stressTest, velocity ->
        base.copy(
            isSyncing = isSyncing,
            emergencyStressTest = stressTest,
            savingsVelocity = velocity
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WealthState()
    )

    fun setWealthDistributionTab(tab: WealthDistributionTab) {
        _wealthDistributionTab.value = tab
    }

    fun setFireManualEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setFireManualEnabled(enabled)
        }
    }

    fun setManualFireAnnualExpense(amount: Long) {
        viewModelScope.launch {
            userPreferencesRepository.setManualFireAnnualExpense(amount)
        }
    }

    fun triggerCloudSync(context: android.content.Context) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val workRequest = OneTimeWorkRequestBuilder<CloudSyncAndBackupWorker>().build()
                WorkManager.getInstance(context).enqueue(workRequest)
                kotlinx.coroutines.delay(2000)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun togglePrivacyMode() {
        viewModelScope.launch {
            userPreferencesRepository.setPrivacyModeEnabled(!state.value.isPrivacyModeEnabled)
        }
    }
}
