package com.sans.finance.presentation.transaction_stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sans.finance.R
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.domain.model.CategorySpent
import com.sans.finance.domain.model.DaySpent
import com.sans.finance.presentation.components.CategoryIcon
import com.sans.finance.presentation.components.ExpenseItem
import com.sans.finance.presentation.components.GlassCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin


@Composable
fun TrendChart(
    trendData: List<DaySpent>,
    period: TransactionStatsPeriodType,
    timeScope: TrendTimeScope,
    onTimeScopeSelected: (TrendTimeScope) -> Unit,
    currencyCode: String = "USD"
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val haptic = LocalHapticFeedback.current

    val sortedSpending = remember(trendData) { trendData.sortedBy { it.day } }
    val amounts = remember(sortedSpending) { sortedSpending.map { it.amount } }
    val nonZeroAmounts = remember(amounts) { amounts.filter { it > 0 }.sorted() }
    val totalAmount = remember(amounts) { amounts.sum() }
    val maxAmount = remember(amounts) { amounts.maxOrNull() ?: 0L }
    val medianAmount = remember(nonZeroAmounts) {
        if (nonZeroAmounts.isEmpty()) 0L
        else nonZeroAmounts[nonZeroAmounts.size / 2]
    }
    val avgAmount = remember(sortedSpending, totalAmount) {
        if (sortedSpending.isNotEmpty()) totalAmount / sortedSpending.size else 0L
    }

    val isAnomalyDetected = remember(maxAmount, medianAmount, sortedSpending.size) {
        sortedSpending.size >= 3 && medianAmount > 0 && maxAmount >= 3 * medianAmount
    }
    val outlierThreshold = remember(medianAmount, isAnomalyDetected) {
        if (isAnomalyDetected) (medianAmount * 3).coerceAtLeast(1L) else Long.MAX_VALUE
    }

    var scaleMode by remember(isAnomalyDetected) {
        mutableStateOf(if (isAnomalyDetected) ScaleMode.ADAPTIVE else ScaleMode.ADAPTIVE)
    }

    val isDaily = remember(timeScope, period) {
        timeScope == TrendTimeScope.IN_PERIOD && period != TransactionStatsPeriodType.ANNUALLY
    }

    val zoneId = remember { ZoneId.systemDefault() }
    val tooltipDailyDtf = remember {
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.getDefault())
    }
    val tooltipMonthlyDtf = remember {
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    }
    val xAxisLabels = remember(sortedSpending, isDaily) {
        val pattern = if (isDaily) "dd" else "MMM"
        val dtf = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        sortedSpending.map { dtf.format(Instant.ofEpochMilli(it.day).atZone(zoneId)) }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Header Row: Title & Scale Mode Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.spending_trend).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    scaleMode = if (scaleMode == ScaleMode.ADAPTIVE) ScaleMode.LINEAR else ScaleMode.ADAPTIVE
                },
                shape = RoundedCornerShape(12.dp),
                color = if (scaleMode == ScaleMode.ADAPTIVE) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (scaleMode == ScaleMode.ADAPTIVE) Icons.Default.Bolt else Icons.Default.Timeline,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (scaleMode == ScaleMode.ADAPTIVE) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (scaleMode == ScaleMode.ADAPTIVE) "Adaptive" else "Linear",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (scaleMode == ScaleMode.ADAPTIVE) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Time Range Filter Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf(
                TrendTimeScope.IN_PERIOD to "Period",
                TrendTimeScope.LAST_6_MONTHS to "6M",
                TrendTimeScope.LAST_12_MONTHS to "1Y",
                TrendTimeScope.ALL_TIME to "All"
            )
            tabs.forEach { (scope, label) ->
                val isSelected = timeScope == scope
                FilterChip(
                    selected = isSelected,
                    onClick = { onTimeScopeSelected(scope) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.height(30.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Anomaly Banner (if detected)
        if (isAnomalyDetected) {
            Spacer(modifier = Modifier.height(8.dp))
            val outlier = sortedSpending.maxByOrNull { it.amount }
            val outlierDateStr = remember(outlier, isDaily) {
                if (outlier != null) {
                    val dtf = if (isDaily) DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())
                    else DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
                    dtf.format(Instant.ofEpochMilli(outlier.day).atZone(zoneId))
                } else ""
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFB74D).copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFFFB74D),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Fat-tail spike on $outlierDateStr (${CurrencyFormatter.formatAmountCompact(maxAmount, currencyCode)}): scale dynamically compressed to keep regular spending patterns visible.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chart Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            if (sortedSpending.isEmpty()) {
                Text(
                    stringResource(R.string.no_data_available),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val textMeasurer = rememberTextMeasurer()
                val labelStyle = MaterialTheme.typography.labelSmall.copy(
                    color = onSurfaceColor.copy(alpha = 0.65f),
                    fontSize = 10.sp
                )
                var selectedIndex by remember { mutableStateOf<Int?>(null) }

                val tooltipTitleStyle = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                val tooltipValueStyle = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black
                )
                val tooltipBadgeStyle = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFFFFB74D),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )

                val c = maxOf(1.0, medianAmount.toDouble() / 2.0)
                val fMax = kotlin.math.ln(1.0 + maxAmount.toDouble() / c)

                fun normY(amt: Long): Float {
                    if (maxAmount <= 0L) return 0f
                    return when (scaleMode) {
                        ScaleMode.LINEAR -> (amt.toDouble() / maxAmount.toDouble()).toFloat().coerceIn(0f, 1f)
                        ScaleMode.ADAPTIVE -> {
                            if (fMax <= 0.0) 0f
                            else (kotlin.math.ln(1.0 + amt.toDouble() / c) / fMax).toFloat().coerceIn(0f, 1f)
                        }
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(sortedSpending.size) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val yAxisLabelWidth = textMeasurer.measure(
                                        CurrencyFormatter.formatAmountCompact(maxAmount, currencyCode),
                                        style = labelStyle
                                    ).size.width.toFloat() + 16f
                                    val chartLeft = yAxisLabelWidth
                                    val chartWidth = size.width - chartLeft
                                    val stepX = chartWidth / (sortedSpending.size - 1).coerceAtLeast(1)
                                    val idx = ((offset.x - chartLeft + stepX / 2f) / stepX).toInt()
                                        .coerceIn(0, sortedSpending.size - 1)
                                    if (selectedIndex != idx) {
                                        selectedIndex = idx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                onDrag = { change, _ ->
                                    val yAxisLabelWidth = textMeasurer.measure(
                                        CurrencyFormatter.formatAmountCompact(maxAmount, currencyCode),
                                        style = labelStyle
                                    ).size.width.toFloat() + 16f
                                    val chartLeft = yAxisLabelWidth
                                    val chartWidth = size.width - chartLeft
                                    val stepX = chartWidth / (sortedSpending.size - 1).coerceAtLeast(1)
                                    val idx = ((change.position.x - chartLeft + stepX / 2f) / stepX).toInt()
                                        .coerceIn(0, sortedSpending.size - 1)
                                    if (selectedIndex != idx) {
                                        selectedIndex = idx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                onDragEnd = { selectedIndex = null },
                                onDragCancel = { selectedIndex = null }
                            )
                        }
                        .pointerInput(sortedSpending.size) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val yAxisLabelWidth = textMeasurer.measure(
                                        CurrencyFormatter.formatAmountCompact(maxAmount, currencyCode),
                                        style = labelStyle
                                    ).size.width.toFloat() + 16f
                                    val chartLeft = yAxisLabelWidth
                                    val chartWidth = size.width - chartLeft
                                    val stepX = chartWidth / (sortedSpending.size - 1).coerceAtLeast(1)
                                    val idx = ((offset.x - chartLeft + stepX / 2f) / stepX).toInt()
                                        .coerceIn(0, sortedSpending.size - 1)
                                    selectedIndex = idx
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    tryAwaitRelease()
                                    selectedIndex = null
                                }
                            )
                        }
                ) {
                    val yAxisLabelText = CurrencyFormatter.formatAmountCompact(maxAmount, currencyCode)
                    val yAxisLabelWidth = textMeasurer.measure(yAxisLabelText, style = labelStyle).size.width.toFloat() + 16f
                    val bottomPadding = 32f

                    val chartLeft = yAxisLabelWidth
                    val chartRight = size.width
                    val chartTop = 16f
                    val chartBottom = size.height - bottomPadding

                    val chartWidth = chartRight - chartLeft
                    val chartHeight = chartBottom - chartTop

                    // Grid and Y-axis labels
                    val yValues = when (scaleMode) {
                        ScaleMode.LINEAR -> {
                            listOf(0L, (maxAmount * 0.33f).toLong(), (maxAmount * 0.66f).toLong(), maxAmount)
                        }
                        ScaleMode.ADAPTIVE -> {
                            val v1 = if (fMax > 0) (c * (kotlin.math.exp(0.33 * fMax) - 1.0)).toLong() else 0L
                            val v2 = if (fMax > 0) (c * (kotlin.math.exp(0.66 * fMax) - 1.0)).toLong() else 0L
                            listOf(0L, v1, v2, maxAmount).distinct().sorted()
                        }
                    }

                    yValues.forEach { value ->
                        val fractionY = normY(value)
                        val y = chartBottom - (fractionY * chartHeight)

                        drawLine(
                            color = gridColor,
                            start = Offset(chartLeft, y),
                            end = Offset(chartRight, y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )

                        val textLayoutResult = textMeasurer.measure(
                            CurrencyFormatter.formatAmountCompact(value, currencyCode),
                            style = labelStyle
                        )
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(
                                chartLeft - textLayoutResult.size.width - 8f,
                                y - textLayoutResult.size.height / 2f
                            )
                        )
                    }

                    // Curve & Area Fill
                    if (sortedSpending.isNotEmpty()) {
                        val points = mutableListOf<Offset>()
                        val stepX = if (sortedSpending.size > 1) {
                            chartWidth / (sortedSpending.size - 1)
                        } else {
                            chartWidth / 2f
                        }

                        sortedSpending.forEachIndexed { index, data ->
                            val x = if (sortedSpending.size == 1) chartLeft + chartWidth / 2f else chartLeft + index * stepX
                            val fractionY = normY(data.amount)
                            val y = chartBottom - (fractionY * chartHeight)
                            points.add(Offset(x, y))
                        }

                        val path = Path()
                        path.moveTo(points.first().x, points.first().y)

                        if (points.size > 1) {
                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                val dx = (p2.x - p1.x) * 0.45f
                                val cp1 = Offset(p1.x + dx, p1.y)
                                val cp2 = Offset(p2.x - dx, p2.y)
                                path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
                            }
                        }

                        val fillPath = Path().apply {
                            addPath(path)
                            lineTo(points.last().x, chartBottom)
                            lineTo(points.first().x, chartBottom)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.35f),
                                    primaryColor.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                startY = chartTop,
                                endY = chartBottom
                            )
                        )

                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = Stroke(width = 5f, cap = StrokeCap.Round)
                        )

                        // Outlier Beacon Pins
                        sortedSpending.forEachIndexed { index, data ->
                            if (data.amount >= outlierThreshold && isAnomalyDetected) {
                                val pt = points[index]
                                drawCircle(
                                    color = Color(0xFFFFB74D).copy(alpha = 0.25f),
                                    radius = 18f,
                                    center = pt
                                )
                                drawCircle(
                                    color = Color(0xFFFFB74D),
                                    radius = 8f,
                                    center = pt,
                                    style = Stroke(width = 3f)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 4f,
                                    center = pt
                                )
                            }
                        }

                        // Tooltip when scrubbing
                        selectedIndex?.let { idx ->
                            val pt = points[idx]
                            val data = sortedSpending[idx]
                            val isOutlier = data.amount >= outlierThreshold && isAnomalyDetected

                            // Dashed guide line
                            drawLine(
                                color = primaryColor.copy(alpha = 0.6f),
                                start = Offset(pt.x, chartTop),
                                end = Offset(pt.x, chartBottom),
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )

                            // Glowing point dot
                            drawCircle(color = primaryColor.copy(alpha = 0.3f), radius = 14f, center = pt)
                            drawCircle(color = primaryColor, radius = 7f, center = pt)
                            drawCircle(color = Color.White, radius = 3.5f, center = pt)

                            // Formatted strings
                            val dateStr = if (isDaily) {
                                tooltipDailyDtf.format(Instant.ofEpochMilli(data.day).atZone(zoneId))
                            } else {
                                tooltipMonthlyDtf.format(Instant.ofEpochMilli(data.day).atZone(zoneId))
                            }
                            val amtStr = CurrencyFormatter.formatAmount(data.amount, currencyCode)
                            val badgeStr = if (isOutlier && medianAmount > 0) {
                                "⚡ Spike (${String.format(Locale.US, "%.0f", data.amount.toDouble() / medianAmount)}× median)"
                            } else null

                            val dateLayout = textMeasurer.measure(dateStr, tooltipTitleStyle)
                            val amtLayout = textMeasurer.measure(amtStr, tooltipValueStyle)
                            val badgeLayout = badgeStr?.let { textMeasurer.measure(it, tooltipBadgeStyle) }

                            val contentWidth = maxOf(
                                dateLayout.size.width,
                                amtLayout.size.width,
                                badgeLayout?.size?.width ?: 0
                            ).toFloat()
                            val contentHeight = dateLayout.size.height + amtLayout.size.height +
                                    (badgeLayout?.let { it.size.height + 4 } ?: 0)

                            val tWidth = contentWidth + 28f
                            val tHeight = contentHeight + 20f

                            var tX = pt.x - tWidth / 2f
                            if (tX < chartLeft) tX = chartLeft + 8f
                            if (tX + tWidth > chartRight) tX = chartRight - tWidth - 8f

                            var tY = pt.y - tHeight - 20f
                            if (tY < chartTop) tY = pt.y + 20f

                            // Draw Tooltip Container
                            drawRoundRect(
                                color = surfaceVariantColor,
                                topLeft = Offset(tX, tY),
                                size = Size(tWidth, tHeight),
                                cornerRadius = CornerRadius(12f, 12f)
                            )
                            drawRoundRect(
                                color = primaryColor.copy(alpha = 0.4f),
                                topLeft = Offset(tX, tY),
                                size = Size(tWidth, tHeight),
                                cornerRadius = CornerRadius(12f, 12f),
                                style = Stroke(width = 1.5f)
                            )

                            var currentY = tY + 10f
                            drawText(
                                textLayoutResult = dateLayout,
                                topLeft = Offset(tX + 14f, currentY)
                            )
                            currentY += dateLayout.size.height + 2f
                            drawText(
                                textLayoutResult = amtLayout,
                                topLeft = Offset(tX + 14f, currentY)
                            )
                            if (badgeLayout != null) {
                                currentY += amtLayout.size.height + 2f
                                drawText(
                                    textLayoutResult = badgeLayout,
                                    topLeft = Offset(tX + 14f, currentY)
                                )
                            }
                        }

                        // X-axis date labels
                        val labelStep = when {
                            sortedSpending.size <= 7 -> 1
                            sortedSpending.size <= 14 -> 2
                            sortedSpending.size <= 24 -> 4
                            else -> (sortedSpending.size / 5).coerceAtLeast(1)
                        }

                        for (i in sortedSpending.indices step labelStep) {
                            val pt = points[i]
                            val labelText = xAxisLabels[i]
                            val textLayoutResult = textMeasurer.measure(labelText, style = labelStyle)
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(
                                    pt.x - textLayoutResult.size.width / 2f,
                                    chartBottom + 6f
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mini Metrics Strip
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TrendMetricItem(
                label = "Total",
                value = CurrencyFormatter.formatAmountCompact(totalAmount, currencyCode)
            )
            TrendMetricItem(
                label = if (isDaily) "Daily Avg" else "Monthly Avg",
                value = CurrencyFormatter.formatAmountCompact(avgAmount, currencyCode)
            )
            TrendMetricItem(
                label = "Median",
                value = CurrencyFormatter.formatAmountCompact(medianAmount, currencyCode)
            )
            TrendMetricItem(
                label = "Peak",
                value = CurrencyFormatter.formatAmountCompact(maxAmount, currencyCode)
            )
        }
    }
}

@Composable
fun TrendMetricItem(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SectionTitle(title: String, icon: ImageVector? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 1.5.sp
        )
    }
}


@Composable
fun PieChartWithLabels(
    categories: List<CategorySpent>,
    totalAmount: Long,
    currencyCode: String
) {
    if (categories.isEmpty() || totalAmount == 0L) return

    val textMeasurer = rememberTextMeasurer()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val haptic = LocalHapticFeedback.current
    var selectedIndex by remember { mutableIntStateOf(-1) }

    val sortedCategories = remember(categories) {
        categories.sortedByDescending { it.totalAmount }.take(12)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(sortedCategories) {
                    detectTapGestures { offset ->
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
                        val radius = Math.min(canvasWidth, canvasHeight) / 3f

                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val distance = Math.sqrt((dx * dx + dy * dy).toDouble())

                        if (distance <= radius && distance >= radius * 0.5f) {
                            var angle =
                                Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f

                            // Adjust for -90 start angle
                            val adjustedAngle = (angle + 90f) % 360f

                            var currentStartAngle = 0f
                            sortedCategories.forEachIndexed { index, category ->
                                val sweep = (category.totalAmount.toFloat() / totalAmount) * 360f
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
            val radius = Math.min(canvasWidth, canvasHeight) / 3f
            val center = Offset(canvasWidth / 2, canvasHeight / 2)

            var startAngle = -90f

            sortedCategories.forEachIndexed { index, category ->
                val sweepAngle = (category.totalAmount.toFloat() / totalAmount) * 360f
                val color = pieChartColors[index % pieChartColors.size]
                val isSelected = selectedIndex == index

                // Draw pie slice (Donut style)
                val slicePath = Path().apply {
                    moveTo(center.x, center.y)
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            center.x - radius,
                            center.y - radius,
                            center.x + radius,
                            center.y + radius
                        ),
                        startAngleDegrees = startAngle,
                        sweepAngleDegrees = sweepAngle,
                        forceMoveTo = false
                    )
                    close()
                }

                val innerRadius = radius * 0.6f
                val innerPath = Path().apply {
                    addOval(
                        androidx.compose.ui.geometry.Rect(
                            center.x - innerRadius,
                            center.y - innerRadius,
                            center.x + innerRadius,
                            center.y + innerRadius
                        )
                    )
                }

                clipPath(innerPath, clipOp = androidx.compose.ui.graphics.ClipOp.Difference) {
                    drawPath(
                        path = slicePath,
                        color = if (isSelected) color else color.copy(alpha = 0.8f)
                    )
                    if (isSelected) {
                        drawPath(
                            path = slicePath,
                            color = Color.White.copy(alpha = 0.3f),
                            style = Stroke(width = 4f)
                        )
                    }
                }

                // Only draw labels for slices > 3% to avoid clutter
                if (sweepAngle > 10f) {
                    // Calculate label position
                    val angleInRadians = (startAngle + sweepAngle / 2) * (Math.PI / 180f)
                    val lineStart = Offset(
                        x = center.x + (radius * 0.9f) * cos(angleInRadians).toFloat(),
                        y = center.y + (radius * 0.9f) * sin(angleInRadians).toFloat()
                    )
                    val lineEnd = Offset(
                        x = center.x + (radius * 1.2f) * cos(angleInRadians).toFloat(),
                        y = center.y + (radius * 1.2f) * sin(angleInRadians).toFloat()
                    )
                    val isRightSide = cos(angleInRadians) > 0
                    val textEnd = Offset(
                        x = lineEnd.x + (if (isRightSide) 20f else -20f),
                        y = lineEnd.y
                    )

                    // Draw connecting line
                    val path = Path().apply {
                        moveTo(lineStart.x, lineStart.y)
                        lineTo(lineEnd.x, lineEnd.y)
                        lineTo(textEnd.x, textEnd.y)
                    }
                    drawPath(
                        path = path,
                        color = color.copy(alpha = 0.5f),
                        style = Stroke(width = 2f)
                    )

                    // Draw text label
                    val label = String.format(Locale.US, "%.0f%%", (sweepAngle / 360f) * 100)
                    val textLayoutResult = textMeasurer.measure(
                        text = label,
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceColor
                        )
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x = textEnd.x + (if (isRightSide) 4f else -textLayoutResult.size.width - 4f),
                            y = textEnd.y - textLayoutResult.size.height / 2f
                        )
                    )
                }

                startAngle += sweepAngle
            }
        }

        // Center Info
        if (selectedIndex != -1) {
            val selected = sortedCategories[selectedIndex]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    selected.categoryName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    CurrencyFormatter.formatAmount(selected.totalAmount, currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    String.format(
                        Locale.US,
                        "%.1f%%",
                        (selected.totalAmount.toFloat() / totalAmount) * 100
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = pieChartColors[selectedIndex % pieChartColors.size],
                    fontWeight = FontWeight.ExtraBold
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "TOTAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    CurrencyFormatter.formatAmount(totalAmount, currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
