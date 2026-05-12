package com.sans.finance.data.util

import android.content.Context
import android.net.Uri
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Serializable
data class PortfolioSnapshotJson(
    val metadata: SnapshotMetadata,
    val holdings: List<HoldingJson>
)

@Serializable
data class SnapshotMetadata(
    val date: String,
    @SerialName("exchange_rate") val exchangeRate: Double? = null
)

@Serializable
data class HoldingJson(
    val source: String,
    val category: String,
    val asset: String,
    val currency: String = "IDR",
    val amount: Double = 0.0,
    @SerialName("value_idr") val valueIdr: Double = 0.0,
    @SerialName("value_usd") val valueUsd: Double = 0.0,
    @SerialName("asset_class") val assetClass: String = "Other",
    val account: String? = null,
    val details: String? = null
)

object PortfolioJsonImporter {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    }

    fun parse(
        context: Context,
        uri: Uri
    ): Triple<Long, List<PortfolioHoldingEntity>, Double?> {
        val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        } ?: throw Exception("Failed to open input stream")

        val snapshot = json.decodeFromString<PortfolioSnapshotJson>(jsonString)

        val snapshotDate = try {
            DATE_FORMAT.parse(snapshot.metadata.date)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        val entities = snapshot.holdings.map { holding ->
            PortfolioHoldingEntity(
                snapshotDate = snapshotDate,
                source = holding.source,
                category = holding.category,
                asset = holding.asset,
                currency = holding.currency,
                amount = holding.amount,
                price = extractPrice(holding.details),
                valueIdr = holding.valueIdr,
                assetClass = holding.assetClass,
                account = holding.account ?: "",
                details = holding.details
            )
        }

        return Triple(snapshotDate, entities, snapshot.metadata.exchangeRate)
    }

    private fun extractPrice(details: String?): Double? {
        if (details == null) return null
        val priceRegex = Regex("""Price:\s*\$?([\d,]+\.?\d*)""")
        val match = priceRegex.find(details)
        return match?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
    }
}
