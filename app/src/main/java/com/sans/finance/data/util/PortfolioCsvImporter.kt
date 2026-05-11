package com.sans.finance.data.util

import android.net.Uri
import android.content.Context
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Parses portfolio CSV files in the format:
 * source,category,asset,currency,amount,value_idr,value_usd,account,details
 *
 * The snapshot date is derived from the filename (YYYY-MM-DD_portfolio.csv)
 * or supplied explicitly by the caller.
 */
object PortfolioCsvImporter {

    private val DATE_PATTERN = Regex("""(\d{4}-\d{2}-\d{2})""")
    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    }

    /**
     * Parse a portfolio CSV from a content URI.
     *
     * @param context Android context for ContentResolver
     * @param uri     Content URI of the CSV file
     * @param snapshotDateOverride If provided, use this date (epoch millis) instead of parsing from filename
     * @return Pair of (snapshotDate, list of parsed entities)
     */
    fun parse(
        context: Context,
        uri: Uri,
        snapshotDateOverride: Long? = null
    ): Pair<Long, List<PortfolioHoldingEntity>> {
        // Try to extract date from display name
        val snapshotDate = snapshotDateOverride ?: extractDateFromUri(context, uri)
            ?: System.currentTimeMillis()

        val items = mutableListOf<PortfolioHoldingEntity>()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream))
            var isFirstLine = true

            reader.forEachLine { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty()) return@forEachLine
                if (isFirstLine) {
                    isFirstLine = false
                    // Skip header
                    if (line.startsWith("source", ignoreCase = true)) return@forEachLine
                }

                val fields = parseCsvLine(line)
                if (fields.size >= 9) {
                    val source = fields[0].trim()
                    val category = fields[1].trim()
                    val asset = fields[2].trim()
                    val currency = fields[3].trim().ifEmpty { "IDR" }
                    val amount = fields[4].trim().toDoubleOrNull() ?: 0.0
                    val valueIdr = fields[5].trim().toDoubleOrNull() ?: 0.0
                    val account = fields[7].trim()
                    val details = fields[8].trim().takeIf { it.isNotEmpty() }

                    // Try to extract price from details
                    val price = extractPrice(details)

                    items.add(
                        PortfolioHoldingEntity(
                            snapshotDate = snapshotDate,
                            source = source,
                            category = category,
                            asset = asset,
                            currency = currency,
                            amount = amount,
                            price = price,
                            valueIdr = valueIdr,
                            account = account,
                            details = details
                        )
                    )
                }
            }
        }

        return snapshotDate to items
    }

    /**
     * Parse a CSV line that may contain quoted fields with commas inside.
     */
    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        fields.add(current.toString())
        return fields
    }

    /**
     * Try to extract a numeric price from the details string.
     * Handles formats like: "Price: $2,330.38" or "Price: 6175"
     */
    private fun extractPrice(details: String?): Double? {
        if (details == null) return null
        val priceRegex = Regex("""Price:\s*\$?([\d,]+\.?\d*)""")
        val match = priceRegex.find(details)
        return match?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
    }

    /**
     * Try to extract a date from the URI display name.
     * Expects format like: 2026-05-11_portfolio.csv
     */
    private fun extractDateFromUri(context: Context, uri: Uri): Long? {
        val displayName = try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) cursor.getString(nameIndex) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }

        if (displayName != null) {
            val match = DATE_PATTERN.find(displayName)
            if (match != null) {
                return try {
                    DATE_FORMAT.parse(match.value)?.time
                } catch (e: Exception) {
                    null
                }
            }
        }
        return null
    }
}
