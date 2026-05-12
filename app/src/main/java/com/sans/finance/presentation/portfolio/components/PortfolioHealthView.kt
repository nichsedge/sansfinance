package com.sans.finance.presentation.portfolio.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sans.finance.domain.model.AssetClassHealth
import com.sans.finance.domain.model.HealthStatus
import com.sans.finance.domain.model.RiskLevel
import com.sans.finance.presentation.components.PrivacyText

@Composable
fun PortfolioHealthView(
    healthList: List<AssetClassHealth>,
    isPrivacyModeEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (healthList.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No health data available", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DiversificationSummary(healthList)
        }

        items(healthList) { health ->
            AssetHealthCard(health, isPrivacyModeEnabled)
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

@Composable
fun AssetHealthCard(health: AssetClassHealth, isPrivacyModeEnabled: Boolean) {
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
                    Text("Current", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${String.format("%.1f", health.currentPercentage)}%", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Target", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${String.format("%.1f", health.targetPercentage)}%", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (health.status != HealthStatus.HEALTHY) {
                Spacer(Modifier.height(12.dp))
                val action = if (health.status == HealthStatus.OVERWEIGHT) "Reduce" else "Increase"
                val diff = Math.abs(health.diffPercentage)
                Text(
                    text = "$action this asset class by ≈${String.format("%.1f", diff)}% to reach target.",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
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
