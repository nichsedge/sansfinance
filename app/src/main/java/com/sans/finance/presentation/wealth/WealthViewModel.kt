package com.sans.finance.presentation.wealth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.PortfolioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class WealthState(
    val cashAssets: Long = 0L,
    val liabilities: Long = 0L,
    val portfolioValue: Long = 0L,
    val lastSnapshotDate: Long? = null,
    val portfolioSources: List<Pair<String, Int>> = emptyList(),
    val currencyCode: String = "IDR",
    val isPrivacyModeEnabled: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class WealthViewModel @Inject constructor(
    accountRepository: AccountRepository,
    portfolioRepository: PortfolioRepository,
    accountTypeRepository: com.sans.finance.domain.repository.AccountTypeRepository,
    private val localeManager: LocaleManager
) : ViewModel() {

    val state = combine(
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
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WealthState()
    )
}
