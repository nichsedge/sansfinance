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
    val totals: SnapshotTotals? = null,
    val holdings: List<HoldingJson> = emptyList(),
    @SerialName("liquid_cash_accounts") val liquidCashAccounts: List<HoldingJson> = emptyList(),
    val advisor: SovereignAdvisorJson? = null
)

@Serializable
data class SovereignAdvisorJson(
    val date: String = "",
    @SerialName("hourly_velocity_idr") val hourlyVelocityIdr: Double = 0.0,
    @SerialName("velocity_str") val velocityStr: String = "N/A",
    @SerialName("atracker_work_hours_30d") val workHours30d: Double = 0.0,
    @SerialName("dry_powder_idr") val dryPowderIdr: Double = 0.0,
    @SerialName("dry_powder_pct") val dryPowderPct: Double = 0.0,
    @SerialName("net_worth_idr") val netWorthIdr: Double = 0.0,
    @SerialName("mom_growth_idr") val momGrowthIdr: Double = 0.0,
    @SerialName("action_summary") val actionSummary: String = "",
    @SerialName("equities_verdicts") val equitiesVerdicts: List<EquityVerdictJson> = emptyList(),
    val opportunities: List<ScreenedOpportunityJson> = emptyList()
)

@Serializable
data class EquityVerdictJson(
    val ticker: String,
    @SerialName("value_idr") val valueIdr: Double = 0.0,
    @SerialName("weight_pct") val weightPct: Double = 0.0,
    val verdict: String = "",
    @SerialName("nff_20d") val nff20d: Double = 0.0
)

@Serializable
data class ScreenedOpportunityJson(
    val ticker: String,
    val name: String = "",
    val close: Double = 0.0,
    val roe: Double = 0.0,
    val per: Double = 0.0,
    @SerialName("nff_20d") val nff20d: Double = 0.0
)

data class SnapshotImportResult(
    val snapshotDate: Long,
    val holdings: List<PortfolioHoldingEntity>,
    val exchangeRate: Double?,
    val advisor: SovereignAdvisorJson? = null
) {
    fun toTriple(): Triple<Long, List<PortfolioHoldingEntity>, Double?> = Triple(snapshotDate, holdings, exchangeRate)
}

@Serializable
data class SnapshotTotals(
    @SerialName("net_worth_idr") val netWorthIdr: Double = 0.0,
    @SerialName("net_worth_usd") val netWorthUsd: Double = 0.0,
    @SerialName("investments_idr") val investmentsIdr: Double = 0.0,
    @SerialName("bank_cash_idr") val bankCashIdr: Double = 0.0
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
    val quantity: Double = 0.0,
    val price: Double? = null,
    @SerialName("value_idr") val valueIdr: Double = 0.0,
    @SerialName("value_usd") val valueUsd: Double = 0.0,
    @SerialName("asset_class") val assetClass: String = "Other",
    val account: String = "",
    val details: String? = null,
    @SerialName("cost_basis") val costBasis: Double? = null,
    @SerialName("yield_rate") val yieldRate: Double? = null
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
    ): SnapshotImportResult {
        val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        } ?: throw Exception("Failed to open input stream")

        return parseContent(jsonString)
    }

    fun parseContent(jsonString: String): SnapshotImportResult {
        val snapshot = json.decodeFromString<PortfolioSnapshotJson>(jsonString)

        val snapshotDate = try {
            DATE_FORMAT.parse(snapshot.metadata.date)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        val entities = snapshot.holdings
            .filterNot { it.source.equals("SansFinance", ignoreCase = true) }
            .map { holding ->
            PortfolioHoldingEntity(
                snapshotDate = snapshotDate,
                source = holding.source,
                category = holding.category,
                asset = holding.asset,
                currency = holding.currency,
                quantity = holding.quantity,
                price = holding.price,
                valueIdr = holding.valueIdr,
                assetClass = holding.assetClass,
                account = holding.account,
                details = holding.details,
                costBasis = holding.costBasis,
                yieldRate = holding.yieldRate
            )
        }

        return SnapshotImportResult(snapshotDate, entities, snapshot.metadata.exchangeRate, snapshot.advisor)
    }
}
