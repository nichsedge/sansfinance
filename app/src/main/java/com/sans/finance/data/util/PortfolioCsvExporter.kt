package com.sans.finance.data.util

import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object PortfolioCsvExporter {

    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    }

    fun toCsv(date: Long, holdings: List<PortfolioHoldingEntity>): String {
        val dateStr = DATE_FORMAT.format(Date(date))
        val sb = StringBuilder()
        sb.append("date,source,category,asset,currency,quantity,price,value_idr,asset_class,account,details\n")

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
            sb.append(escapeCsv(holding.details ?: ""))
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
