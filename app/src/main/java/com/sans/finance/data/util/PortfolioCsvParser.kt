package com.sans.finance.data.util

import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object PortfolioCsvParser {

    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    }

    fun parse(csvContent: String): Triple<Long, List<PortfolioHoldingEntity>, Double?> {
        val lines = csvContent.lines()
        if (lines.size < 2) throw Exception("CSV is empty or missing header")

        val holdingsByDate = mutableMapOf<Long, MutableList<PortfolioHoldingEntity>>()

        lines.drop(1).forEach { line ->
            if (line.isBlank()) return@forEach
            val values = parseCsvLine(line)
            if (values.size < 8) return@forEach

            try {
                val dateStr = values[0]
                val currentLineDate = try {
                    DATE_FORMAT.parse(dateStr)?.time
                } catch (e: Exception) {
                    null
                } ?: return@forEach // Skip if date is invalid

                val holding = PortfolioHoldingEntity(
                    snapshotDate = currentLineDate,
                    source = values[1],
                    category = values[2],
                    asset = values[3],
                    currency = values[4],
                    quantity = values[5].toDoubleOrNull() ?: 0.0,
                    price = values[6].toDoubleOrNull(),
                    valueIdr = values[7].toDoubleOrNull() ?: 0.0,
                    assetClass = if (values.size > 8) values[8] else "Other",
                    account = if (values.size > 9) values[9] else "",
                    details = if (values.size > 10) values[10] else null
                )

                holdingsByDate.getOrPut(currentLineDate) { mutableListOf() }.add(holding)
            } catch (e: Exception) {
                // Skip malformed lines
            }
        }

        if (holdingsByDate.isEmpty()) return Triple(System.currentTimeMillis(), emptyList(), null)

        val latestDate = holdingsByDate.keys.maxOrNull() ?: System.currentTimeMillis()
        val latestHoldings = holdingsByDate[latestDate] ?: emptyList()

        return Triple(latestDate, latestHoldings, null)
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val char = line[i]
            when {
                char == '\"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        current.append('\"')
                        i++ // Skip next quote
                    } else {
                        inQuotes = !inQuotes
                    }
                }

                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }

                else -> current.append(char)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
