package com.sans.finance.data.util

import com.sans.finance.domain.model.Expense
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvParser {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun parse(csvContent: String): List<Expense> {
        val lines = csvContent.lines()
        if (lines.isEmpty()) return emptyList()

        val header = lines.first().split(",")
        val expenses = mutableListOf<Expense>()

        lines.drop(1).forEach { line ->
            if (line.isBlank()) return@forEach

            val values = parseCsvLine(line)
            if (values.size < 7) return@forEach

            try {
                // Assuming format from CsvExporter:
                // date,source,store,item_name,qty,item_price,order_total,status,is_installment
                val dateStr = values[0]
                val source = values[1] // Tag
                val details = values[2] // Store/Details
                val title = values[3] // Item name
                val amount = (values[6].toDouble() * 100).toLong() // order_total
                val isInst = values[8] == "1"

                expenses.add(
                    Expense(
                        date = dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis(),
                        title = title,
                        amount = amount,
                        categoryId = 1, // Default, will need to be mapped or handled by repository
                        details = details,
                        tags = if (source.isNotBlank()) listOf(source) else emptyList(),
                        isInstallment = isInst,
                        type = if (amount < 0) "INCOME" else "EXPENSE" // Simple heuristic or improve based on data
                    )
                )
            } catch (e: Exception) {
                // Skip malformed lines
            }
        }

        return expenses
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
