package com.sans.finance.presentation.portfolio.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sans.finance.data.local.dao.CategoryTotal
import com.sans.finance.data.local.dao.SnapshotTotal
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NetWorthTrendChart(
    history: List<SnapshotTotal>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    isPrivacyModeEnabled: Boolean = false
) {
    if (history.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Not enough data for trend", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )

    Canvas(modifier = modifier.padding(bottom = 24.dp, end = 8.dp)) {
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
            val label = if (isPrivacyModeEnabled) "••••" else formatCompactIdr(value)
            val textLayoutResult = textMeasurer.measure(label, labelStyle)
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(width - textLayoutResult.size.width, y - textLayoutResult.size.height)
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
        val dateFormat = SimpleDateFormat("MMM", Locale.US)
        history.forEachIndexed { index, snapshot ->
            if (index == 0 || index == history.size - 1 || history.size < 5) {
                val dateStr = dateFormat.format(Date(snapshot.snapshot_date))
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
    }
}

@Composable
fun AllocationDonutChart(
    categories: List<CategoryTotal>,
    modifier: Modifier = Modifier
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

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            var startAngle = -90f
            categories.forEachIndexed { index, category ->
                val sweepAngle = (category.totalIdr / total).toFloat() * 360f
                val color = colors[index % colors.size]
                
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 32.dp.toPx(), cap = StrokeCap.Butt)
                )
                startAngle += sweepAngle
            }
        }

        Spacer(Modifier.width(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.take(5).forEachIndexed { index, category ->
                val color = colors[index % colors.size]
                val percent = (category.totalIdr / total * 100).toInt()
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color, shape = CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${category.category} ($percent%)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (categories.size > 5) {
                Text(
                    text = "+ ${categories.size - 5} more",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatCompactIdr(value: Double): String {
    return when {
        value >= 1_000_000_000 -> String.format("%.1fB", value / 1_000_000_000)
        value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000)
        value >= 1_000 -> String.format("%.1fK", value / 1_000)
        else -> String.format("%.0f", value)
    }
}
