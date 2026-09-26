package com.sans.finance.presentation.portfolio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sans.finance.domain.model.AssetClassHealth
import com.sans.finance.domain.model.HealthStatus
import com.sans.finance.domain.model.RiskLevel

@Composable
fun PortfolioHealthView(
    healthList: List<AssetClassHealth>,
    rebalanceSuggestions: List<com.sans.finance.domain.model.RebalanceAction>,
    currencyBreakdowns: List<com.sans.finance.domain.model.CurrencyValuationSummary>,
    isPrivacyModeEnabled: Boolean,
    currentCurrency: String,
    modifier: Modifier = Modifier,
    comparison: com.sans.finance.domain.model.PortfolioBenchmarkComparison? = null,
    selectedBenchmark: com.sans.finance.domain.model.BenchmarkType = com.sans.finance.domain.model.BenchmarkType.SP500,
    onSelectBenchmark: (com.sans.finance.domain.model.BenchmarkType) -> Unit = {},
    onTargetClick: (AssetClassHealth) -> Unit = {}
) {
    if (healthList.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No health data available", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PortfolioBenchmarkCard(
            comparison = comparison,
            selectedBenchmark = selectedBenchmark,
            onSelectBenchmark = onSelectBenchmark
        )

        DiversificationSummary(healthList)

        CurrencyExposureCard(currencyBreakdowns, currentCurrency)

        AllocationDriftChart(healthList)

        DcaDepositRebalanceCard(
            healthList = healthList,
            isPrivacyModeEnabled = isPrivacyModeEnabled,
            currentCurrency = currentCurrency
        )

        if (rebalanceSuggestions.isNotEmpty()) {
            RebalanceSuggestionsSection(
                suggestions = rebalanceSuggestions,
                isPrivacyModeEnabled = isPrivacyModeEnabled,
                currentCurrency = currentCurrency
            )
        }

        healthList.forEach { health ->
            AssetHealthCard(health, isPrivacyModeEnabled, onClick = { onTargetClick(health) })
        }
    }
}

@Composable
fun DiversificationSummary(healthList: List<AssetClassHealth>) {
    val healthyCount = healthList.count { it.status == HealthStatus.HEALTHY }
    val totalCount = healthList.size
    val healthScore = if (totalCount > 0) (healthyCount.toFloat() / totalCount.toFloat()) else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            healthScore >= 0.8f -> Color(0xFF4CAF50)
                            healthScore >= 0.5f -> Color(0xFFFFC107)
                            else -> Color(0xFFF44336)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${(healthScore * 100).toInt()}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text(
                    "Diversification Score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        healthScore >= 0.8f -> "Your portfolio is well-balanced."
                        healthScore >= 0.5f -> "Some assets need rebalancing."
                        else -> "Your portfolio is highly imbalanced."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CurrencyExposureCard(
    summaries: List<com.sans.finance.domain.model.CurrencyValuationSummary>,
    baseCurrency: String
) {
    if (summaries.isEmpty()) return

    val totalInBase = summaries.sumOf { it.totalInBaseCurrency }
    if (totalInBase <= 0) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "CURRENCY EXPOSURE & FX RISK",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                // Risk Warning for concentration
                val highConcentration = summaries.find { (it.totalInBaseCurrency / totalInBase) > 0.85 }
                if (highConcentration != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "High ${highConcentration.currency} Risk",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val sortedSummaries = remember(summaries) { summaries.sortedByDescending { it.totalInBaseCurrency } }

            // Stacked Bar using Row & Weight
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                sortedSummaries.forEachIndexed { index, summary ->
                    val weight = (summary.totalInBaseCurrency / totalInBase).toFloat()
                    if (weight > 0.001f) {
                        val color = when (index % 4) {
                            0 -> MaterialTheme.colorScheme.primary
                            1 -> MaterialTheme.colorScheme.tertiary
                            2 -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.outline
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(weight)
                                .background(color)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Legend
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                sortedSummaries.forEachIndexed { index, summary ->
                    val pct = (summary.totalInBaseCurrency / totalInBase) * 100.0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val color = when (index % 4) {
                            0 -> MaterialTheme.colorScheme.primary
                            1 -> MaterialTheme.colorScheme.tertiary
                            2 -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.outline
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${summary.currency}: ${String.format(java.util.Locale.US, "%.2f%%", pct)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AllocationDriftChart(healthList: List<AssetClassHealth>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "ALLOCATION DRIFT (TARGET VS ACTUAL)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(16.dp))

            healthList.forEach { health ->
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            health.assetClass,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${String.format(java.util.Locale.US, "%.2f%%", health.currentPercentage)} / ${String.format(java.util.Locale.US, "%.2f%%", health.targetPercentage)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        val maxWidth = maxWidth
                        // Actual Bar (Bottom)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (health.currentPercentage / 100f).toFloat().coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(
                                    if (health.status == HealthStatus.HEALTHY) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    else if (health.status == HealthStatus.OVERWEIGHT) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                                )
                        )

                        // Target Indicator (Line)
                        val targetOffset = maxWidth * (health.targetPercentage.toFloat() / 100f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(2.5.dp)
                                .padding(start = targetOffset)
                                .background(MaterialTheme.colorScheme.onSurface)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssetHealthCard(health: AssetClassHealth, isPrivacyModeEnabled: Boolean, onClick: () -> Unit) {
    val statusColor = when (health.status) {
        HealthStatus.HEALTHY -> Color(0xFF4CAF50)
        HealthStatus.OVERWEIGHT -> Color(0xFFF44336)
        HealthStatus.UNDERWEIGHT -> Color(0xFFFFC107)
    }

    val statusIcon = when (health.status) {
        HealthStatus.HEALTHY -> Icons.Default.CheckCircle
        HealthStatus.OVERWEIGHT -> Icons.Default.Warning
        HealthStatus.UNDERWEIGHT -> Icons.Default.ArrowDownward
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = health.assetClass,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                RiskBadge(health.riskLevel)
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Current",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${String.format(java.util.Locale.US, "%.2f", health.currentPercentage)}%",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Target",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${String.format(java.util.Locale.US, "%.2f", health.targetPercentage)}%",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Status",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = health.status.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { (health.currentPercentage / 100f).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (health.status != HealthStatus.HEALTHY) {
                Spacer(Modifier.height(12.dp))
                val action = if (health.status == HealthStatus.OVERWEIGHT) "Reduce" else "Increase"
                val diff = Math.abs(health.diffPercentage)
                Text(
                    text = "$action this asset class by ≈${
                        String.format(
                            java.util.Locale.US,
                            "%.2f",
                            diff
                        )
                    }% to reach target.",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun RebalanceSuggestionsSection(
    suggestions: List<com.sans.finance.domain.model.RebalanceAction>,
    isPrivacyModeEnabled: Boolean,
    currentCurrency: String
) {
    Column {
        Text(
            "SUGGESTED REBALANCING ACTIONS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
            letterSpacing = 1.sp
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                suggestions.forEachIndexed { index, suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                suggestion.assetClass,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val actionColor = if (suggestion.action == com.sans.finance.domain.model.RebalanceType.BUY)
                                    Color(0xFF4CAF50) else Color(0xFFF44336)
                                Icon(
                                    imageVector = if (suggestion.action == com.sans.finance.domain.model.RebalanceType.BUY)
                                        Icons.Default.Add else Icons.Default.Remove,
                                    contentDescription = null,
                                    tint = actionColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    suggestion.action.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = actionColor,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Adjust by ${String.format(java.util.Locale.US, "%.2f%%", suggestion.percentageToAdjust)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        com.sans.finance.presentation.components.PrivacyText(
                            amount = (suggestion.amount * 100).toLong(),
                            currencyCode = currentCurrency,
                            isVisible = !isPrivacyModeEnabled,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (index < suggestions.size - 1) {
                        androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RiskBadge(riskLevel: RiskLevel) {
    val (color, label) = when (riskLevel) {
        RiskLevel.LOW -> Color(0xFF4CAF50) to "Low Risk"
        RiskLevel.MEDIUM -> Color(0xFFFF9800) to "Medium Risk"
        RiskLevel.HIGH -> Color(0xFFF44336) to "High Risk"
        RiskLevel.VERY_HIGH -> Color(0xFF9C27B0) to "Very High Risk"
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = CircleShape,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DcaDepositRebalanceCard(
    healthList: List<AssetClassHealth>,
    isPrivacyModeEnabled: Boolean,
    currentCurrency: String
) {
    val underweightList = healthList.filter { it.status == HealthStatus.UNDERWEIGHT }
    if (underweightList.isEmpty()) return

    val totalDeficit = underweightList.sumOf { Math.abs(it.diffPercentage) }
    if (totalDeficit <= 0) return

    var depositAmountIdr by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(5_000_000L) }
    val presets = listOf(2_500_000L, 5_000_000L, 10_000_000L, 15_000_000L)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "MONTHLY DCA DEPOSIT STRATEGY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "Allocates new deposit without selling existing holdings",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "DCA Calculator",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Interactive Deposit Selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), MaterialTheme.shapes.medium)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Deposit Target:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Rp ${String.format(java.util.Locale.US, "%,d", depositAmountIdr)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                androidx.compose.material3.Slider(
                    value = depositAmountIdr.toFloat(),
                    onValueChange = { depositAmountIdr = ((it / 500_000f).toInt() * 500_000L).coerceIn(1_000_000L, 20_000_000L) },
                    valueRange = 1_000_000f..20_000_000f,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = depositAmountIdr == preset
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { depositAmountIdr = preset }
                        ) {
                            Text(
                                text = "Rp ${preset / 1_000_000}M",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            androidx.compose.material3.HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            underweightList.forEachIndexed { index, item ->
                val weightFraction = Math.abs(item.diffPercentage) / totalDeficit
                val suggestedAllocation = (depositAmountIdr * 100 * weightFraction).toLong()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.assetClass,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Target: ${String.format(java.util.Locale.US, "%.2f%%", item.targetPercentage)} (Current: ${String.format(java.util.Locale.US, "%.2f%%", item.currentPercentage)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        com.sans.finance.presentation.components.PrivacyText(
                            amount = suggestedAllocation,
                            currencyCode = currentCurrency,
                            isVisible = !isPrivacyModeEnabled,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.2f", weightFraction * 100f)}% of deposit",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (index < underweightList.size - 1) {
                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}
