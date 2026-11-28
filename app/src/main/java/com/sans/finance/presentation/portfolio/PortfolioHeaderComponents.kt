package com.sans.finance.presentation.portfolio

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.presentation.ai.components.MarkdownContent
import com.sans.finance.presentation.components.AppTopBar
import com.sans.finance.presentation.components.GlassCard
import com.sans.finance.presentation.components.PrivacyText
import com.sans.finance.presentation.portfolio.components.AllocationDonutChart
import com.sans.finance.presentation.portfolio.components.EnhancedHoldingItem
import com.sans.finance.presentation.portfolio.components.ExpandableCategoryGroup
import com.sans.finance.presentation.portfolio.components.HoldingDetailBottomSheet
import com.sans.finance.presentation.portfolio.components.NetWorthTrendChart
import com.sans.finance.presentation.portfolio.components.PortfolioFilterBar
import com.sans.finance.presentation.portfolio.components.PortfolioGuideBottomSheet
import com.sans.finance.presentation.portfolio.components.PortfolioHealthView
import com.sans.finance.presentation.portfolio.components.PortfolioSortOption
import com.sans.finance.presentation.portfolio.components.PortfolioViewMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun MultiCurrencyBreakdownCard(
    summaries: List<com.sans.finance.domain.model.CurrencyValuationSummary>,
    baseCurrency: String,
    isPrivacyModeEnabled: Boolean
) {
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
                    "Multi-Currency FX Valuation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Base: $baseCurrency",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            summaries.forEachIndexed { index, summary ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    summary.currency,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${summary.count} holding${if (summary.count > 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (summary.currency != baseCurrency) {
                            Text(
                                "FX: 1 ${summary.currency} = ${String.format(Locale.US, "%,.2f", summary.currentFxRate)} $baseCurrency",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        PrivacyText(
                            amount = (summary.totalInBaseCurrency * 100).toLong(),
                            currencyCode = baseCurrency,
                            isVisible = !isPrivacyModeEnabled,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (summary.currency != baseCurrency && summary.fxGainInBase != 0.0) {
                            val fxColor = if (summary.fxGainInBase >= 0) com.sans.finance.ui.theme.IncomeGreen else MaterialTheme.colorScheme.error
                            val fxSign = if (summary.fxGainInBase >= 0) "+" else ""
                            Text(
                                text = "FX: $fxSign${com.sans.finance.core.util.CurrencyFormatter.formatAmountCompact((summary.fxGainInBase * 100).toLong(), baseCurrency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = fxColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (index < summaries.size - 1) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun PortfolioHeader(state: PortfolioScreenState, onForecastingClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primary,
        alpha = 0.12f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Portfolio Value",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            val displayValue = if (state.totalValueInBase > 0) state.totalValueInBase else state.totalValueIdr
            PrivacyText(
                amount = (displayValue * 100).toLong(),
                currencyCode = state.currentCurrency,
                isVisible = !state.isPrivacyModeEnabled,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (state.includedAccountCashIdr > 0.0) {
                Text(
                    "Includes account cash",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            // Annualized Return (XIRR)
            state.xirr?.let { xirrValue ->
                Text(
                    text = "ANNUALIZED RETURN (XIRR): ${String.format(Locale.US, "%.2f%%", xirrValue * 100)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 4.dp),
                    letterSpacing = 0.5.sp
                )
            }

            // Gain Breakdown (Cost-basis Total Return if available, else Snapshot-over-Snapshot Trajectory)
            if (state.hasCostBasis && (state.totalGainInBase != 0.0 || state.totalPriceGainInBase != 0.0)) {
                val gainColor = if (state.totalGainInBase >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                val gainSign = if (state.totalGainInBase >= 0) "+" else ""

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(gainColor.copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (state.totalGainInBase >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = gainColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Total Return: $gainSign${com.sans.finance.core.util.CurrencyFormatter.formatAmountCompact((state.totalGainInBase * 100).toLong(), state.currentCurrency)} (${String.format(Locale.US, "%+.2f%%", state.totalGainPercentage)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = gainColor,
                        fontWeight = FontWeight.Black
                    )
                }

                if (state.totalFxGainInBase != 0.0 || state.totalPriceGainInBase != 0.0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        val fxColor = if (state.totalFxGainInBase >= 0) com.sans.finance.ui.theme.IncomeGreen else MaterialTheme.colorScheme.error
                        val fxSign = if (state.totalFxGainInBase >= 0) "+" else ""
                        Text(
                            text = "FX Movement: $fxSign${com.sans.finance.core.util.CurrencyFormatter.formatAmountCompact((state.totalFxGainInBase * 100).toLong(), state.currentCurrency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = fxColor,
                            fontWeight = FontWeight.Bold
                        )

                        if (state.totalPriceGainInBase != 0.0) {
                            Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            val priceColor = if (state.totalPriceGainInBase >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            val priceSign = if (state.totalPriceGainInBase >= 0) "+" else ""
                            Text(
                                text = "Price: $priceSign${com.sans.finance.core.util.CurrencyFormatter.formatAmountCompact((state.totalPriceGainInBase * 100).toLong(), state.currentCurrency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = priceColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else if (state.previousTotalIdr != null) {
                val currentVal = if (state.totalValueInBase > 0) state.totalValueInBase else state.totalValueIdr
                val prev = state.previousTotalIdr
                val diff = currentVal - prev
                val percent = if (prev != 0.0) (diff / prev) * 100 else 0.0
                val color = if (diff >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                val sign = if (diff >= 0) "+" else ""

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(color.copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (diff >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$sign${com.sans.finance.core.util.CurrencyFormatter.formatAmountCompact((diff * 100).toLong(), state.currentCurrency)} (${String.format(Locale.US, "%+.2f%%", percent)}) vs last snapshot",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Black
                    )
                }

                if (state.totalFxGainInBase != 0.0) {
                    val fxColor = if (state.totalFxGainInBase >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    val fxSign = if (state.totalFxGainInBase >= 0) "+" else ""
                    Text(
                        text = "FX Movement: $fxSign${com.sans.finance.core.util.CurrencyFormatter.formatAmountCompact((state.totalFxGainInBase * 100).toLong(), state.currentCurrency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = fxColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else if (state.totalFxGainInBase != 0.0) {
                val fxColor = if (state.totalFxGainInBase >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                val fxSign = if (state.totalFxGainInBase >= 0) "+" else ""
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(fxColor.copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "FX Movement: $fxSign${com.sans.finance.core.util.CurrencyFormatter.formatAmountCompact((state.totalFxGainInBase * 100).toLong(), state.currentCurrency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = fxColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Initial snapshot • Sync over time to track returns",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onForecastingClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.large,
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("View Wealth Trajectory", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PortfolioAiInsightCard(
    analysis: com.sans.finance.data.ai.PortfolioAnalysisResult?,
    streamingText: String,
    isAnalyzing: Boolean,
    onStop: () -> Unit,
    onClear: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        alpha = 0.15f
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isAnalyzing) "AI STRATEGIST • STREAMING" else "AI STRATEGIST",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }
                if (isAnalyzing) {
                    IconButton(onClick = onStop) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop Analysis",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    IconButton(onClick = onClear) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (isAnalyzing && streamingText.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Menghubungkan ke AI Strategist & menganalisis portofolio...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (streamingText.isNotEmpty()) {
                MarkdownContent(
                    text = streamingText,
                    isStreaming = isAnalyzing
                )
            } else if (analysis?.rawText != null) {
                MarkdownContent(
                    text = analysis.rawText,
                    isStreaming = false
                )
            } else if (analysis != null) {
                Text(
                    analysis.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )

                if (analysis.insights.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    analysis.insights.forEach { insight ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            ),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val dotColor = when (insight.importance) {
                                        "HIGH" -> MaterialTheme.colorScheme.error
                                        "MEDIUM" -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.secondary
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(dotColor, CircleShape)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        insight.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    insight.observation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        insight.suggestion,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
