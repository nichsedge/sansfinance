package com.sans.finance.presentation.portfolio.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.data.local.dao.CategoryTotal
import com.sans.finance.data.local.dao.SnapshotTotal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun NetWorthTrendChart(
    history: List<SnapshotTotal>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    isPrivacyModeEnabled: Boolean = false,
    currencyCode: String = "IDR"
) {
    if (history.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Not enough data for trend", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )
    val tooltipStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onPrimary,
        fontWeight = FontWeight.Bold
    )
    val tooltipDateStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
    )

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp, end = 8.dp)
                .pointerInput(history.size) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val stepX = size.width / (history.size - 1)
                            val index =
                                (offset.x / stepX).roundToInt().coerceIn(0, history.size - 1)
                            selectedIndex = index
                        },
                        onDrag = { change, _ ->
                            val stepX = size.width / (history.size - 1)
                            val index = (change.position.x / stepX).roundToInt()
                                .coerceIn(0, history.size - 1)
                            selectedIndex = index
                        },
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null }
                    )
                }
                .pointerInput(history.size) {
                    detectTapGestures(
                        onPress = { offset ->
                            val stepX = size.width / (history.size - 1)
                            val index =
                                (offset.x / stepX).roundToInt().coerceIn(0, history.size - 1)
                            selectedIndex = index
                            tryAwaitRelease()
                            selectedIndex = null
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height

            val minVal = history.minOf { it.totalIdr }
            val maxVal = history.maxOf { it.totalIdr }
            val range = (maxVal - minVal).coerceAtLeast(1.0)

            // Add some padding to top and bottom of range
            val paddedMin = minVal - (range * 0.1)
            val paddedMax = maxVal + (range * 0.1)
            val paddedRange = paddedMax - paddedMin

            val stepX = width / (history.size - 1)

            val points = history.mapIndexed { index, snapshot ->
                val x = index * stepX
                val fractionY = (snapshot.totalIdr - paddedMin) / paddedRange
                val y = height - (fractionY.toFloat() * height)
                Offset(x, y)
            }

            // Draw background grid lines (horizontal) and labels
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = height * i / gridLines
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.2f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )

                // Y-axis value label
                val value = paddedMax - (i.toDouble() / gridLines * paddedRange)
                val label =
                    if (isPrivacyModeEnabled) "••••" else CurrencyFormatter.formatAmountCompact(
                        value.toLong() * 100,
                        currencyCode
                    )
                val textLayoutResult = textMeasurer.measure(label, labelStyle)
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        width - textLayoutResult.size.width,
                        y - textLayoutResult.size.height
                    )
                )
            }

            // Draw Path
            val path = Path()
            points.forEachIndexed { index, point ->
                if (index == 0) {
                    path.moveTo(point.x, point.y)
                } else {
                    val prev = points[index - 1]
                    val cp1 = Offset(prev.x + (point.x - prev.x) / 2f, prev.y)
                    val cp2 = Offset(prev.x + (point.x - prev.x) / 2f, point.y)
                    path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, point.x, point.y)
                }
            }

            // Draw Area Fill
            val fillPath = Path().apply {
                addPath(path)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent)
                )
            )

            // Draw Line
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draw Dots
            points.forEachIndexed { index, point ->
                drawCircle(
                    color = lineColor,
                    radius = 4.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = point
                )
            }

            // Draw X-axis labels (dates)
            val dateFormatMonth = SimpleDateFormat("MMM", Locale.US)
            history.forEachIndexed { index, snapshot ->
                if (index == 0 || index == history.size - 1 || history.size < 5) {
                    val dateStr = dateFormatMonth.format(Date(snapshot.snapshot_date))
                    val textLayoutResult = textMeasurer.measure(dateStr, labelStyle)
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x = points[index].x - textLayoutResult.size.width / 2f,
                            y = height + 4.dp.toPx()
                        )
                    )
                }
            }

            // Draw Tooltip if selected
            selectedIndex?.let { index ->
                val point = points[index]
                val snapshot = history[index]

                // Draw Vertical Line
                drawLine(
                    color = lineColor.copy(alpha = 0.5f),
                    start = Offset(point.x, 0f),
                    end = Offset(point.x, height),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // Draw Highlight Dot
                drawCircle(
                    color = lineColor,
                    radius = 6.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = point
                )

                // Tooltip Content
                val dateStr =
                    SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(snapshot.snapshot_date))
                val valueStr =
                    if (isPrivacyModeEnabled) "••••" else CurrencyFormatter.formatAmountCompact(
                        snapshot.totalIdr.toLong() * 100,
                        currencyCode
                    )

                val valueLayout = textMeasurer.measure(valueStr, tooltipStyle)
                val dateLayout = textMeasurer.measure(dateStr, tooltipDateStyle)

                val tooltipWidth =
                    maxOf(valueLayout.size.width, dateLayout.size.width) + 24.dp.toPx()
                val tooltipHeight = valueLayout.size.height + dateLayout.size.height + 16.dp.toPx()

                var tooltipX = point.x - tooltipWidth / 2f
                if (tooltipX < 0f) tooltipX = 8.dp.toPx()
                if (tooltipX + tooltipWidth > width) tooltipX = width - tooltipWidth - 8.dp.toPx()

                val tooltipY = (point.y - tooltipHeight - 16.dp.toPx()).coerceAtLeast(8.dp.toPx())

                drawRoundRect(
                    color = lineColor,
                    topLeft = Offset(tooltipX, tooltipY),
                    size = Size(tooltipWidth, tooltipHeight),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )

                drawText(
                    textLayoutResult = dateLayout,
                    topLeft = Offset(tooltipX + 12.dp.toPx(), tooltipY + 8.dp.toPx())
                )
                drawText(
                    textLayoutResult = valueLayout,
                    topLeft = Offset(
                        tooltipX + 12.dp.toPx(),
                        tooltipY + 8.dp.toPx() + dateLayout.size.height
                    )
                )
            }
        }
    }
}

@Composable
fun AllocationDonutChart(
    categories: List<CategoryTotal>,
    modifier: Modifier = Modifier,
    currencyCode: String = "IDR",
    isPrivacyModeEnabled: Boolean = false
) {
    if (categories.isEmpty()) return

    val total = categories.sumOf { it.totalIdr }.coerceAtLeast(1.0)
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        Color(0xFF4CAF50), // Emerald
        Color(0xFFFF9800), // Orange
        Color(0xFF2196F3), // Blue
        Color(0xFF9C27B0), // Purple
        Color(0xFFF44336)  // Red
    )

    val haptic = LocalHapticFeedback.current
    var selectedIndex by remember { mutableStateOf(-1) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(categories) {
                        detectTapGestures { offset ->
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
                            val radius = Math.min(canvasWidth, canvasHeight) / 2f

                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val distance = Math.sqrt((dx * dx + dy * dy).toDouble())

                            // Check if tap is within the donut ring
                            if (distance <= radius && distance >= radius * 0.5f) {
                                var angle =
                                    Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble()))
                                        .toFloat()
                                if (angle < 0) angle += 360f

                                // Adjust for -90 start angle
                                val adjustedAngle = (angle + 90f) % 360f

                                var currentStartAngle = 0f
                                categories.forEachIndexed { index, category ->
                                    val sweep = (category.totalIdr / total).toFloat() * 360f
                                    if (adjustedAngle >= currentStartAngle && adjustedAngle <= currentStartAngle + sweep) {
                                        if (selectedIndex != index) {
                                            selectedIndex = index
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        } else {
                                            selectedIndex = -1
                                        }
                                        return@detectTapGestures
                                    }
                                    currentStartAngle += sweep
                                }
                            } else {
                                selectedIndex = -1
                            }
                        }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
                val radius = Math.min(canvasWidth, canvasHeight) / 2f
                val strokeWidth = 32.dp.toPx()

                var startAngle = -90f
                categories.forEachIndexed { index, category ->
                    val sweepAngle = (category.totalIdr / total).toFloat() * 360f
                    val color = colors[index % colors.size]
                    val isSelected = selectedIndex == index

                    drawArc(
                        color = if (isSelected) color else color.copy(alpha = 0.8f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(
                            width = if (isSelected) strokeWidth * 1.2f else strokeWidth,
                            cap = StrokeCap.Butt
                        )
                    )

                    if (isSelected) {
                        // Draw a subtle border around selected slice
                        drawArc(
                            color = Color.White.copy(alpha = 0.5f),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    startAngle += sweepAngle
                }
            }

            // Center Info
            if (selectedIndex != -1) {
                val selected = categories[selectedIndex]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        selected.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    val amountText = if (isPrivacyModeEnabled) "••••" else
                        CurrencyFormatter.formatAmountCompact(
                            (selected.totalIdr * 100).toLong(),
                            currencyCode
                        )
                    Text(
                        amountText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        String.format(Locale.US, "%.1f%%", (selected.totalIdr / total) * 100),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors[selectedIndex % colors.size],
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(Modifier.width(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.take(5).forEachIndexed { index, category ->
                val color = colors[index % colors.size]
                val percent = (category.totalIdr / total * 100).toInt()
                val isSelected = selectedIndex == index

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent
                        )
                        .clickable {
                            if (selectedIndex == index) selectedIndex = -1
                            else {
                                selectedIndex = index
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 12.dp else 10.dp)
                            .background(color, shape = CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${category.category} ($percent%)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                        color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (categories.size > 5) {
                Text(
                    text = "+ ${categories.size - 5} more",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 18.dp)
                )
            }
        }
    }
}

