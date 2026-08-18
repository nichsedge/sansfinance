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
    private val localeManager: LocaleManager
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)

    private val baseState = combine(
        accountRepository.getAllAccounts(),
        portfolioRepository.getLatestSnapshotHeader(),
        portfolioRepository.getLatestSnapshot(),
        accountTypeRepository.getAllAccountTypes(),
        localeManager.privacyMode
    ) { accounts, latestHeader, latestHoldings, types, privacyMode ->
        val liabilityTypeNames = types.filter { it.isLiability }.map { it.name }.toSet()

        val cashAssets = accounts
            .filter { it.type !in liabilityTypeNames && it.type != "Investment" }
            .sumOf { it.balance }
        val liabilities = accounts
            .filter { it.type in liabilityTypeNames }
            .sumOf { it.balance }

        val portfolioValue = latestHoldings.sumOf { it.valueIdr }.toLong() * 100
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
            currencyCode = localeManager.getCurrency(),
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
