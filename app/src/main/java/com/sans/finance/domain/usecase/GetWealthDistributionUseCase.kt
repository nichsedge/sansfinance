package com.sans.finance.domain.usecase

import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.data.local.entity.AccountTypeEntity
import com.sans.finance.data.local.entity.ExchangeRateEntity
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.domain.model.WealthDistributionTab
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.AccountTypeRepository
import com.sans.finance.domain.repository.PortfolioRepository
import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.data.util.LocaleManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetWealthDistributionUseCase @Inject constructor(
    private val portfolioRepository: PortfolioRepository,
    private val accountRepository: AccountRepository,
    private val accountTypeRepository: AccountTypeRepository,
    private val currencyDao: CurrencyDao,
    private val localeManager: LocaleManager
) {
    operator fun invoke(tab: WealthDistributionTab): Flow<Map<String, Long>> {
        val baseCurrency = localeManager.getCurrency()

        return combine(
            portfolioRepository.getLatestSnapshot(),
            accountRepository.getAllAccounts(),
            accountTypeRepository.getAllAccountTypes(),
            currencyDao.getAllRates()
        ) { holdings, accounts, accountTypes, rates ->
            val ratesMap = rates.associate { it.code to it.rateToIdr }
            val baseRate = if (baseCurrency == "IDR") 1.0 else ratesMap[baseCurrency] ?: 1.0

            calculateDistribution(holdings, accounts, accountTypes, tab, baseRate, ratesMap)
        }
    }

    private fun calculateDistribution(
        holdings: List<PortfolioHoldingEntity>,
        accounts: List<AccountEntity>,
        accountTypes: List<AccountTypeEntity>,
        tab: WealthDistributionTab,
        baseRate: Double,
        ratesMap: Map<String, Double>
    ): Map<String, Long> {
        val liabilityTypeNames = accountTypes.filter { it.isLiability }.map { it.name }.toSet()
        val investmentTypeNames = accountTypes.filter { it.isInvestment }.map { it.name }.toSet()
        val nonLiabilityAccounts = accounts.filter { it.type !in liabilityTypeNames }

        val distribution = when (tab) {
            WealthDistributionTab.CURRENCY -> {
                val hGroup = holdings.groupBy { it.currency }
                    .mapValues { it.value.sumOf { h -> h.valueIdr } }

                val aGroup = nonLiabilityAccounts.groupBy { it.currency }
                    .mapValues { entry ->
                        entry.value.sumOf { a ->
                            val rateToIdr = if (a.currency == "IDR") 1.0 else (ratesMap[a.currency] ?: 1.0)
                            (a.balance / 100.0) * rateToIdr
                        }
                    }

                (hGroup.keys + aGroup.keys).associateWith { key ->
                    val idrValue = (hGroup[key] ?: 0.0) + (aGroup[key] ?: 0.0)
                    if (baseRate > 0) ((idrValue / baseRate) * 100).toLong() else (idrValue * 100).toLong()
                }
            }

            WealthDistributionTab.ASSET_CLASS -> {
                val hGroup = holdings.groupBy { it.assetClass }
                    .mapValues { it.value.sumOf { h -> h.valueIdr } }

                val combined = hGroup.toMutableMap()
                nonLiabilityAccounts.forEach { a ->
                    val rateToIdr = if (a.currency == "IDR") 1.0 else (ratesMap[a.currency] ?: 1.0)
                    val valueIdr = (a.balance / 100.0) * rateToIdr
                    val isInv = a.type in investmentTypeNames
                    val aclass = if (isInv) {
                        if (a.type.contains("P2P", ignoreCase = true)) "Fixed Income" else "Investment"
                    } else {
                        "Cash & Equivalents"
                    }
                    combined[aclass] = (combined[aclass] ?: 0.0) + valueIdr
                }

                combined.mapValues { entry ->
                    val idrValue = entry.value
                    if (baseRate > 0) ((idrValue / baseRate) * 100).toLong() else (idrValue * 100).toLong()
                }
            }

            WealthDistributionTab.CATEGORY -> {
                val hGroup = holdings.groupBy { it.category }
                    .mapValues { it.value.sumOf { h -> h.valueIdr } }

                val aGroup = nonLiabilityAccounts.groupBy { it.type }
                    .mapValues { entry ->
                        entry.value.sumOf { a ->
                            val rateToIdr = if (a.currency == "IDR") 1.0 else (ratesMap[a.currency] ?: 1.0)
                            (a.balance / 100.0) * rateToIdr
                        }
                    }

                (hGroup.keys + aGroup.keys).associateWith { key ->
                    val idrValue = (hGroup[key] ?: 0.0) + (aGroup[key] ?: 0.0)
                    if (baseRate > 0) ((idrValue / baseRate) * 100).toLong() else (idrValue * 100).toLong()
                }
            }
        }

        return distribution.toList()
            .sortedByDescending { kotlin.math.abs(it.second) }
            .toMap()
    }
}
