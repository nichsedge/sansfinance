package com.sans.finance.domain.model

data class AssetClassTarget(
    val assetClass: String,
    val targetPercentage: Double,
    val description: String,
    val riskLevel: RiskLevel
)

enum class RiskLevel {
    LOW, MEDIUM, HIGH, VERY_HIGH
}

data class AssetClassHealth(
    val assetClass: String,
    val currentPercentage: Double,
    val targetPercentage: Double,
    val currentAmount: Double,
    val riskLevel: RiskLevel,
    val status: HealthStatus,
    val diffPercentage: Double
)

enum class HealthStatus {
    HEALTHY, OVERWEIGHT, UNDERWEIGHT
}

object PortfolioHealthDefaults {
    val targets = listOf(
        AssetClassTarget("Cash & Stables", 15.0, "Emergency fund and liquidity", RiskLevel.LOW),
        AssetClassTarget("Equities", 50.0, "Stocks for long-term growth", RiskLevel.HIGH),
        AssetClassTarget("Fixed Income", 20.0, "Bonds and stable yield assets", RiskLevel.MEDIUM),
        AssetClassTarget("Crypto", 10.0, "High-risk speculative assets", RiskLevel.VERY_HIGH),
        AssetClassTarget("Commodities", 5.0, "Inflation hedge and diversification", RiskLevel.MEDIUM)
    )
}
