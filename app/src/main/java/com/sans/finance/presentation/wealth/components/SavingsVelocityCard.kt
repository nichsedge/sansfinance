package com.sans.finance.presentation.wealth.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.domain.model.MomentumTrend
import com.sans.finance.domain.model.SavingsRateVelocitySummary
import com.sans.finance.presentation.components.PrivacyText

@Composable
fun SavingsVelocityCard(
    summary: SavingsRateVelocitySummary,
    isPrivacyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val momentumColor = when (summary.momentumTrend) {
        MomentumTrend.ACCELERATING -> Color(0xFF10B981) // Emerald
        MomentumTrend.STEADY -> MaterialTheme.colorScheme.primary
        MomentumTrend.DECELERATING -> Color(0xFFF59E0B) // Amber
    }

    val momentumIcon = when (summary.momentumTrend) {
        MomentumTrend.ACCELERATING -> Icons.AutoMirrored.Filled.TrendingUp
        MomentumTrend.STEADY -> Icons.AutoMirrored.Filled.TrendingFlat
        MomentumTrend.DECELERATING -> Icons.AutoMirrored.Filled.TrendingDown
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(momentumColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = momentumColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Savings Velocity",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = "6-Month Savings Track",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = momentumColor.copy(alpha = 0.12f),
                    contentColor = momentumColor,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = momentumIcon, contentDescription = null, modifier = Modifier.size(13.dp))
                        Text(
                            text = summary.momentumTrend.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Bold,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            }

            // Key Metrics Row (Compact 2-Pillar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Savings Rate
                Column {
                    Text(
                        text = "Current Savings Rate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.1f%%".format(summary.currentMonthSavingsRatePct),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.Black,
                        color = momentumColor
                    )
                    Text(
                        text = "3-Mo Avg: %.1f%%".format(summary.threeMonthAvgSavingsRatePct),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                // Right: Net Worth Velocity
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Net Worth Velocity",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isPrivacyMode && summary.monthlyNetWorthVelocity > 0) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            )
                        }
                        PrivacyText(
                            amount = summary.monthlyNetWorthVelocity,
                            currencyCode = summary.currencyCode,
                            isVisible = !isPrivacyMode,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.monthlyNetWorthVelocity >= 0) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                            )
                        )
                        Text(
                            text = " / mo",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "6-Mo Avg: %.1f%%".format(summary.sixMonthAvgSavingsRatePct),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            // Interactive 6-Month Savings Mini Bar Visualizer
            if (summary.history.isNotEmpty()) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(summary.history.size) {
                                detectTapGestures { offset ->
                                    val count = summary.history.size
                                    if (count > 0) {
                                        val step = size.width / count
                                        val idx = (offset.x / step).toInt().coerceIn(0, count - 1)
                                        if (selectedIndex != idx) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedIndex = idx
                                        } else {
                                            selectedIndex = null
                                        }
                                    }
                                }
                            }
                    ) {
                        val count = summary.history.size
                        val slotWidth = size.width / count
                        val barWidth = 14.dp.toPx()
                        val chartHeight = size.height - 18.dp.toPx()

                        summary.history.forEachIndexed { index, point ->
                            val centerX = index * slotWidth + slotWidth / 2f
                            val rate = point.savingsRatePct.coerceIn(0.0, 100.0)
                            val barHeight = ((rate / 100.0) * chartHeight).toFloat().coerceAtLeast(6.dp.toPx())
                            val isSelected = selectedIndex == index

                            // Draw subtle background capsule track
                            drawRoundRect(
                                color = trackColor,
                                topLeft = Offset(centerX - barWidth / 2f, 0f),
                                size = Size(barWidth, chartHeight),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )

                            // Draw active savings bar
                            val barBrush = if (isSelected) {
                                Brush.verticalGradient(
                                    colors = listOf(momentumColor, momentumColor.copy(alpha = 0.7f)),
                                    startY = chartHeight - barHeight,
                                    endY = chartHeight
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors = listOf(primaryColor, primaryColor.copy(alpha = 0.5f)),
                                    startY = chartHeight - barHeight,
                                    endY = chartHeight
                                )
                            }

                            drawRoundRect(
                                brush = barBrush,
                                topLeft = Offset(centerX - barWidth / 2f, chartHeight - barHeight),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }
                    }

                    // Month Labels beneath
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        summary.history.forEachIndexed { idx, point ->
                            Text(
                                text = point.monthLabel.take(3),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedIndex == idx) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedIndex == idx) momentumColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }

                // Tooltip detail overlay when a bar is inspected
                AnimatedVisibility(
                    visible = selectedIndex != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    selectedIndex?.let { idx ->
                        val point = summary.history.getOrNull(idx)
                        if (point != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${point.monthLabel}: ${"%.1f%%".format(point.savingsRatePct)} rate",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = momentumColor
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Saved: ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        PrivacyText(
                                            amount = point.savings,
                                            currencyCode = summary.currencyCode,
                                            isVisible = !isPrivacyMode,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
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
}
