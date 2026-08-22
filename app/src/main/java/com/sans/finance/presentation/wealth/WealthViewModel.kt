package com.sans.finance.presentation.wealth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.data.worker.CloudSyncAndBackupWorker
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.PortfolioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WealthState(
    val cashAssets: Long = 0L,
    val liabilities: Long = 0L,
    val portfolioValue: Long = 0L,
    val lastSnapshotDate: Long? = null,
    val portfolioSources: List<Pair<String, Int>> = emptyList(),
    val currencyCode: String = "IDR",
    val isPrivacyModeEnabled: Boolean = false,
    val isSyncing: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class WealthViewModel @Inject constructor(
    accountRepository: AccountRepository,
    portfolioRepository: PortfolioRepository,
    accountTypeRepository: com.sans.finance.domain.repository.AccountTypeRepository,
    private val currencyDao: com.sans.finance.data.local.dao.CurrencyDao,
    private val localeManager: LocaleManager
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)

    private val portfolioState = combine(
        portfolioRepository.getLatestSnapshotHeader(),
        portfolioRepository.getLatestSnapshot()
    ) { latestHeader, latestHoldings -> latestHeader to latestHoldings }

    private val baseState = combine(
        accountRepository.getAllAccounts(),
        portfolioState,
        accountTypeRepository.getAllAccountTypes(),
        currencyDao.getAllRates(),
        localeManager.privacyMode
    ) { accounts, (latestHeader, latestHoldings), types, rates, privacyMode ->
        val liabilityTypeNames = types.filter { it.isLiability }.map { it.name }.toSet()
        val baseCurrency = localeManager.getCurrency()
        val ratesMap = rates.associate { it.code to it.rateToIdr }
        val baseRate = if (baseCurrency == "IDR") 1.0 else (ratesMap[baseCurrency] ?: 1.0)

        fun convertToBase(amount: Long, from: String): Long {
            if (from == baseCurrency) return amount
            val fromRate = if (from == "IDR") 1.0 else (ratesMap[from] ?: 1.0)
            val toRate = baseRate
            if (toRate == 0.0) return amount
            return ((amount * fromRate) / toRate).toLong()
        }

        val cashAssets = accounts
            .filter { it.type !in liabilityTypeNames && it.type != "Investment" }
            .sumOf { convertToBase(it.balance, it.currency) }
        val liabilities = accounts
            .filter { it.type in liabilityTypeNames }
            .sumOf { convertToBase(it.balance, it.currency) }

        val portfolioValueIdr = latestHoldings.sumOf { it.valueIdr }
        val portfolioValue = if (baseRate > 0) ((portfolioValueIdr / baseRate) * 100).toLong() else (portfolioValueIdr * 100).toLong()
        val sources = latestHoldings
            .groupBy { it.source }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }

        WealthState(
            cashAssets = cashAssets,
            liabilities = liabilities,
            portfolioValue = portfolioValue,
            lastSnapshotDate = latestHeader?.snapshotDate,
            portfolioSources = sources,
            currencyCode = baseCurrency,
            isPrivacyModeEnabled = privacyMode,
            isSyncing = false,
            isLoading = false
        )
    }

    val state = combine(baseState, _isSyncing) { base, isSyncing ->
        base.copy(isSyncing = isSyncing)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WealthState()
    )

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
        localeManager.setPrivacyModeEnabled(!localeManager.isPrivacyModeEnabled())
    }
}
