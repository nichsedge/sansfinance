package com.sans.finance.data.util

import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.data.local.entity.AccountTypeEntity
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object PortfolioCsvExporter {

    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    }

    fun toCsv(
        date: Long,
        holdings: List<PortfolioHoldingEntity>,
        accounts: List<AccountEntity> = emptyList(),
        accountTypes: List<AccountTypeEntity> = emptyList(),
        ratesMap: Map<String, Double> = emptyMap()
    ): String {
        val dateStr = DATE_FORMAT.format(Date(date))
        val sb = StringBuilder()
        sb.append("date,source,category,asset,currency,quantity,price,value_idr,asset_class,account,details,cost_basis,yield_rate\n")

        holdings.forEach { holding ->
            sb.append(dateStr).append(",")
            sb.append(escapeCsv(holding.source)).append(",")
            sb.append(escapeCsv(holding.category)).append(",")
            sb.append(escapeCsv(holding.asset)).append(",")
            sb.append(escapeCsv(holding.currency)).append(",")
            sb.append(holding.quantity).append(",")
            sb.append(holding.price ?: 0.0).append(",")
            sb.append(holding.valueIdr).append(",")
            sb.append(escapeCsv(holding.assetClass)).append(",")
            sb.append(escapeCsv(holding.account)).append(",")
            sb.append(escapeCsv(holding.details ?: "")).append(",")
            sb.append(holding.costBasis ?: "").append(",")
            sb.append(holding.yieldRate ?: "")
            sb.append("\n")
        }

        val liabilityTypeNames = accountTypes.filter { it.isLiability }.map { it.name }.toSet()
        val investmentTypeNames = accountTypes.filter { it.isInvestment }.map { it.name }.toSet()

        accounts.forEach { account ->
            val isLiability = account.type in liabilityTypeNames
            val isInvestment = account.type in investmentTypeNames
            val rateToIdr = if (account.currency == "IDR") 1.0 else (ratesMap[account.currency] ?: 1.0)
            val rawQuantity = account.balance / 100.0
            val signedQuantity = if (isLiability) -rawQuantity else rawQuantity
            val signedValueIdr = signedQuantity * rateToIdr
            val assetClass = when {
                isLiability -> "Liability"
                isInvestment -> "Investment"
                else -> "Cash & Equivalents"
            }
            val details = if (isLiability) "Liability account snapshot" else "Account balance snapshot"

            sb.append(dateStr).append(",")
            sb.append("SansFinance").append(",")
            sb.append(escapeCsv(account.type)).append(",")
            sb.append(escapeCsv(account.name)).append(",")
            sb.append(escapeCsv(account.currency)).append(",")
            sb.append(signedQuantity).append(",")
            sb.append(rateToIdr).append(",")
            sb.append(signedValueIdr).append(",")
            sb.append(escapeCsv(assetClass)).append(",")
            sb.append(escapeCsv(account.name)).append(",")
            sb.append(escapeCsv(details)).append(",")
            sb.append("").append(",")
            sb.append("")
            sb.append("\n")
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }
}
