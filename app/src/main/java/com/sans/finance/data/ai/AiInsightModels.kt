package com.sans.finance.data.ai

import kotlinx.serialization.Serializable

@Serializable
data class MonthlyReviewInsight(
    val title: String,
    val why: String,
    val action: String,
    val severity: String = "INFO" // INFO | WARN | CRITICAL
)

data class MonthlyReviewResult(
    val headline: String,
    val insights: List<MonthlyReviewInsight>,
    val rawText: String? = null
)

