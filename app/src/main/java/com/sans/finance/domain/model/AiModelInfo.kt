package com.sans.finance.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AiModelInfo(
    val id: String,
    val name: String,
    val description: String = "",
    val contextLength: Int = 0,
    val promptPricing: Double = 0.0,
    val completionPricing: Double = 0.0,
    val isFree: Boolean = false
) {
    val pricingLabel: String
        get() = when {
            isFree -> "GRATIS"
            promptPricing == 0.0 && completionPricing == 0.0 -> "GRATIS"
            else -> {
                val promptPerMillion = promptPricing * 1_000_000
                "$" + String.format(java.util.Locale.US, "%.2f", promptPerMillion) + "/1M"
            }
        }

    val contextLengthLabel: String
        get() = when {
            contextLength >= 1_000_000 -> "${contextLength / 1_000_000}M ctx"
            contextLength >= 1_000 -> "${contextLength / 1_000}k ctx"
            contextLength > 0 -> "$contextLength ctx"
            else -> ""
        }
}
