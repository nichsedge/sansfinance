package com.sans.finance.data.util

import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.data.local.entity.AccountTypeEntity
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object PortfolioJsonExporter {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    }

    fun toSnapshotJson(
        date: Long,
        holdings: List<PortfolioHoldingEntity>,
        accounts: List<AccountEntity> = emptyList(),
        accountTypes: List<AccountTypeEntity> = emptyList(),
        ratesMap: Map<String, Double> = emptyMap()
    ): String {
        val usdRate = ratesMap["USD"]?.takeIf { it > 0.0 }
        val metadata = SnapshotMetadata(
            date = DATE_FORMAT.format(Date(date)),
            exchangeRate = usdRate
        )

        val holdingsJson = holdings.map { holding ->
            HoldingJson(
                source = holding.source,
                category = holding.category,
                asset = holding.asset,
                currency = holding.currency,
                quantity = holding.quantity,
                price = holding.price,
                valueIdr = holding.valueIdr,
                valueUsd = if (usdRate != null) holding.valueIdr / usdRate else 0.0,
                assetClass = holding.assetClass,
                account = holding.account,
                details = holding.details,
                costBasis = holding.costBasis,
                yieldRate = holding.yieldRate
            )
        }

        val liabilityTypeNames = accountTypes.filter { it.isLiability }.map { it.name }.toSet()
        val investmentTypeNames = accountTypes.filter { it.isInvestment }.map { it.name }.toSet()

        val accountSnapshots = accounts.map { account ->
            val isLiability = account.type in liabilityTypeNames
            val isInvestment = account.type in investmentTypeNames
            val rateToIdr = if (account.currency == "IDR") 1.0 else (ratesMap[account.currency] ?: 1.0)
            val rawQuantity = account.balance / 100.0
            val signedQuantity = if (isLiability) -rawQuantity else rawQuantity
            val signedValueIdr = signedQuantity * rateToIdr
            val valueUsd = if (usdRate != null) signedValueIdr / usdRate else 0.0
            val assetClass = when {
                isLiability -> "Liability"
                isInvestment -> "Investment"
                else -> "Cash & Equivalents"
            }

            HoldingJson(
                source = "SansFinance",
                category = account.type,
                asset = account.name,
                currency = account.currency,
                quantity = signedQuantity,
                price = rateToIdr,
                valueIdr = signedValueIdr,
                valueUsd = valueUsd,
                assetClass = assetClass,
                account = account.name,
                details = if (isLiability) "Liability account snapshot" else "Account balance snapshot"
            )
        }

        val investmentsIdr = holdingsJson.sumOf { it.valueIdr }
        val bankCashIdr = accountSnapshots.sumOf { it.valueIdr }
        val netWorthIdr = investmentsIdr + bankCashIdr
        val totals = SnapshotTotals(
            netWorthIdr = netWorthIdr,
            netWorthUsd = if (usdRate != null) netWorthIdr / usdRate else 0.0,
            investmentsIdr = investmentsIdr,
            bankCashIdr = bankCashIdr
        )

        val snapshot = PortfolioSnapshotJson(
            metadata = metadata,
            totals = totals,
            holdings = holdingsJson,
            liquidCashAccounts = accountSnapshots
        )
        return json.encodeToString(snapshot)
    }
}
