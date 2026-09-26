package com.sans.finance.presentation.portfolio.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sans.finance.domain.model.BenchmarkPoint
import com.sans.finance.domain.model.BenchmarkType
import com.sans.finance.domain.model.PortfolioBenchmarkComparison
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PortfolioBenchmarkCard(
    comparison: PortfolioBenchmarkComparison?,
    selectedBenchmark: BenchmarkType,
    onSelectBenchmark: (BenchmarkType) -> Unit,
    modifier: Modifier = Modifier
) {
    if (comparison == null) return

    if (comparison.trajectory.size < 2) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), CircleShape)
                                .padding(6.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "MACRO BENCHMARK & ALPHA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "Baseline Snapshot",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    "Benchmark Alpha comparison (vs ${selectedBenchmark.displayName}) requires at least 2 snapshot dates over time. Sync future snapshots to measure your portfolio outperformance trajectory.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val isPositiveAlpha = comparison.alphaPct >= 0
    val alphaColor = if (isPositiveAlpha) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), CircleShape)
                            .padding(6.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "MACRO BENCHMARK & ALPHA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = alphaColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isPositiveAlpha) "+%.2f%% Alpha".format(comparison.alphaPct) else "%.2f%% Alpha".format(comparison.alphaPct),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = alphaColor
                    )
                }
            }

            // Benchmark Selector Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BenchmarkType.entries.forEach { bench ->
                    val isSelected = bench == selectedBenchmark
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.clickable { onSelectBenchmark(bench) }
                    ) {
                        Text(
                            text = bench.displayName,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = TextStyle(fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Stats Matrix
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Portfolio Return", style = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Text(
                            "%.2f%%".format(comparison.portfolioTotalReturnPct),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = if (comparison.portfolioTotalReturnPct >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("${selectedBenchmark.name} Return", style = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Text(
                            "%.2f%%".format(comparison.benchmarkTotalReturnPct),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Timespan", style = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Text(
                            "${comparison.durationDays}d",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Chart Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp, 3.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text("Portfolio (Indexed)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp, 2.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text(selectedBenchmark.displayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Dual Line Canvas
            BenchmarkComparisonChart(
                trajectory = comparison.trajectory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }
    }
}

@Composable
fun BenchmarkComparisonChart(
    trajectory: List<BenchmarkPoint>,
    modifier: Modifier = Modifier
) {
    if (trajectory.size < 2) return

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    val primaryColor = MaterialTheme.colorScheme.primary
    val benchmarkColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current
    val dateFormatter = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(trajectory.size) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val chartLeft = 70f
                            val chartRight = size.width - 20f
                            val chartWidth = chartRight - chartLeft
                            val index = ((offset.x - chartLeft) / chartWidth * (trajectory.size - 1)).toInt()
                                .coerceIn(0, trajectory.size - 1)
                            if (selectedIndex != index) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedIndex = index
                            }
                        },
                        onDrag = { change, _ ->
                            val chartLeft = 70f
                            val chartRight = size.width - 20f
                            val chartWidth = chartRight - chartLeft
                            val index = ((change.position.x - chartLeft) / chartWidth * (trajectory.size - 1)).toInt()
                                .coerceIn(0, trajectory.size - 1)
                            if (selectedIndex != index) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedIndex = index
                            }
                        },
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null }
                    )
                }
                .pointerInput(trajectory.size) {
                    detectTapGestures(
                        onPress = { offset ->
                            val chartLeft = 70f
                            val chartRight = size.width - 20f
                            val chartWidth = chartRight - chartLeft
                            val index = ((offset.x - chartLeft) / chartWidth * (trajectory.size - 1)).toInt()
                                .coerceIn(0, trajectory.size - 1)
                            if (selectedIndex != index) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedIndex = index
                            }
                            tryAwaitRelease()
                            selectedIndex = null
                        }
                    )
                }
        ) {
            val chartTop = 20f
            val chartBottom = size.height - 35f
            val chartLeft = 70f
            val chartRight = size.width - 20f
            val chartHeight = chartBottom - chartTop
            val chartWidth = chartRight - chartLeft

            val maxIndex = maxOf(
                trajectory.maxOf { it.portfolioIndex },
                trajectory.maxOf { it.benchmarkIndex }
            )
            val minIndex = minOf(
                trajectory.minOf { it.portfolioIndex },
                trajectory.minOf { it.benchmarkIndex },
                95.0
            )
            val range = (maxIndex - minIndex).coerceAtLeast(10.0).toFloat() * 1.1f

            // Y Grid lines
            val yLines = 4
            for (i in 0 until yLines) {
                val fraction = i.toFloat() / (yLines - 1)
                val y = chartBottom - (fraction * chartHeight)
                val value = minIndex + (fraction * range)
                drawLine(
                    color = gridColor,
                    start = Offset(chartLeft, y),
                    end = Offset(chartRight, y),
                    strokeWidth = 1f
                )
                val layout = textMeasurer.measure("%.0f".format(value), style = labelStyle)
                drawText(layout, topLeft = Offset(10f, y - (layout.size.height / 2f)))
            }

            val stepX = chartWidth / (trajectory.size - 1).toFloat()

            // Draw Benchmark Line (Dashed)
            val benchmarkPath = Path()
            trajectory.forEachIndexed { i, pt ->
                val x = chartLeft + (i * stepX)
                val y = chartBottom - (((pt.benchmarkIndex - minIndex) / range).toFloat() * chartHeight)
                if (i == 0) benchmarkPath.moveTo(x, y) else benchmarkPath.lineTo(x, y)
            }
            drawPath(
                path = benchmarkPath,
                color = benchmarkColor.copy(alpha = 0.8f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                    cap = StrokeCap.Round
                )
            )

            // Draw Portfolio Line (Solid with glow)
            val portfolioPath = Path()
            val fillPath = Path()
            trajectory.forEachIndexed { i, pt ->
                val x = chartLeft + (i * stepX)
                val y = chartBottom - (((pt.portfolioIndex - minIndex) / range).toFloat() * chartHeight)
                if (i == 0) {
                    portfolioPath.moveTo(x, y)
                    fillPath.moveTo(x, chartBottom)
                    fillPath.lineTo(x, y)
                } else {
                    portfolioPath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            val lastX = chartLeft + ((trajectory.size - 1) * stepX)
            fillPath.lineTo(lastX, chartBottom)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.18f), primaryColor.copy(alpha = 0.01f)),
                    startY = chartTop,
                    endY = chartBottom
                )
            )

            drawPath(
                path = portfolioPath,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Scrubber Overlay
            selectedIndex?.let { idx ->
                val pt = trajectory[idx]
                val x = chartLeft + (idx * stepX)
                val yPort = chartBottom - (((pt.portfolioIndex - minIndex) / range).toFloat() * chartHeight)

                drawLine(
                    color = primaryColor.copy(alpha = 0.6f),
                    start = Offset(x, chartTop),
                    end = Offset(x, chartBottom),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = Offset(x, yPort))
                drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = Offset(x, yPort))

                val dateStr = dateFormatter.format(Date(pt.dateEpochMs))
                val portRet = pt.portfolioIndex - 100.0
                val benchRet = pt.benchmarkIndex - 100.0
                val text = "$dateStr: Port ${if (portRet >= 0) "+" else ""}%.2f%% vs Bench ${if (benchRet >= 0) "+" else ""}%.2f%%".format(portRet, benchRet)
                val tooltipLayout = textMeasurer.measure(
                    text,
                    style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                )

                val tooltipWidth = tooltipLayout.size.width + 16f
                val tooltipHeight = tooltipLayout.size.height + 10f
                val tooltipX = (x - tooltipWidth / 2f).coerceIn(chartLeft, chartRight - tooltipWidth)
                val tooltipY = (chartTop - 12f).coerceAtLeast(0f)

                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.85f),
                    topLeft = Offset(tooltipX, tooltipY),
                    size = Size(tooltipWidth, tooltipHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                drawText(
                    tooltipLayout,
                    topLeft = Offset(tooltipX + 8f, tooltipY + 5f)
                )
            }
        }
    }
}
