package com.sans.finance.data.util

import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import kotlinx.serialization.encodeToString
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

    fun toSnapshotJson(date: Long, holdings: List<PortfolioHoldingEntity>): String {
        val metadata = SnapshotMetadata(
            date = DATE_FORMAT.format(Date(date)),
            exchangeRate = null // We don't store exchange rate per holding in DB easily, or we could pass it if known
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
                valueUsd = 0.0, // Assuming 0 for now as we mostly use IDR
                assetClass = holding.assetClass,
                accountKey = holding.accountKey,
                accountName = holding.accountName ?: holding.account,
                account = holding.account,
                details = holding.details
            )
        }

        val snapshot = PortfolioSnapshotJson(metadata, holdingsJson)
        return json.encodeToString(snapshot)
    }
}
