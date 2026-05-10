package com.sans.finance.presentation.stats

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.R
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.core.util.DateFormatterUtils
import com.sans.finance.core.util.CalendarUtils
import com.sans.finance.presentation.components.CategoryIcon
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.cos
import kotlin.math.sin

val pieChartColors = listOf(
    Color(0xFFFF6B6B), // Red/Salmon
    Color(0xFFFF9248), // Orange
    Color(0xFFFFB347), // Yellow/Orange
    Color(0xFFFFD166), // Yellow
    Color(0xFFB5E48C), // Light Green
    Color(0xFF86D97F), // Green
    Color(0xFF52B69A), // Teal/Cyan
    Color(0xFF48CAE4), // Light Blue
    Color(0xFF9D4EDD), // Purple
    Color(0xFFC77DFF), // Light Purple
    Color(0xFFFF9F1C),
    Color(0xFF2EC4B6)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.statistics), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Monthly Overview Header
                HeaderPart(
                    title = stringResource(R.string.this_month),
                    amount = state.thisMonthSpent,
                    lastMonthAmount = state.lastMonthSpent
                )

                // Spending Trend Chart
                TrendPeriodSelector(
                    selectedPeriod = state.selectedTrendPeriod,
                    onPeriodSelected = viewModel::onTrendPeriodSelected
                )

                SpendingTrendChart(state.trendSpending, state.selectedTrendPeriod)

                // Categories Breakdown
                CategoryBreakdown(state.spendingByCategory)

                // Comparison Cards
                // Comparison Cards
                SectionTitle(
                    stringResource(R.string.yearly_summary),
                    icon = Icons.Default.CalendarMonth
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatsSimpleCard(
                        modifier = Modifier.weight(1.0f),
                        title = stringResource(R.string.this_year),
                        amount = state.thisYearSpent,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                    StatsSimpleCard(
                        modifier = Modifier.weight(1.0f),
                        title = stringResource(R.string.last_year),
                        amount = state.lastYearSpent,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun HeaderPart(
    title: String,
    amount: Long,
    lastMonthAmount: Long
) {
    val diff = amount - lastMonthAmount
    val percent = if (lastMonthAmount > 0) (diff.toDouble() / lastMonthAmount * 100).toInt() else 0

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 1.5.sp
        )
        Text(
            CurrencyFormatter.formatAmount(amount),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        if (lastMonthAmount > 0) {
            Surface(
                color = if (diff > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape
            ) {
                Text(
                    text = "${if (diff > 0) "+" else ""}$percent% from last month",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (diff > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendPeriodSelector(
    selectedPeriod: TrendPeriod,
    onPeriodSelected: (TrendPeriod) -> Unit
) {
    val periods = TrendPeriod.values()
    val options = listOf(
        stringResource(R.string.daily),
        stringResource(R.string.weekly),
        stringResource(R.string.monthly),
        stringResource(R.string.quarterly),
        stringResource(R.string.yearly)
    )

    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        periods.forEachIndexed { index, period ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = periods.size),
                onClick = { onPeriodSelected(period) },
                selected = selectedPeriod == period,
                label = {
                    Text(
                        options[index],
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            )
        }
    }
}

@Composable
fun SpendingTrendChart(
    spending: List<com.sans.finance.data.local.entity.DaySpent>,
    period: TrendPeriod
) {
    SectionTitle(stringResource(R.string.spending_trend), icon = Icons.Default.Insights)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.3f
            )
        )
    ) {
        Box(modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(220.dp)) {
            if (spending.isEmpty()) {
                Text("No data for this period", modifier = Modifier.align(Alignment.Center))
            } else {
                val sortedSpending = remember(spending) { spending.sortedBy { it.day } }

                val dateFormat = remember(period) {
                    when (period) {
                        TrendPeriod.DAILY -> DateFormatterUtils.getDayMonthFormatter()
                        TrendPeriod.WEEKLY -> DateFormatterUtils.getDayMonthFormatter()
                        TrendPeriod.MONTHLY -> DateFormatterUtils.getMonthYearFormatter()
                        TrendPeriod.QUARTERLY -> {
                            object : java.text.Format() {
                                override fun format(obj: Any?, toAppendTo: StringBuffer, pos: java.text.FieldPosition): StringBuffer {
                                    val date = obj as Date
                                    val cal = CalendarUtils.getInstance().apply { time = date }
                                    val year = cal.get(Calendar.YEAR) % 100
                                    val quarter = (cal.get(Calendar.MONTH) / 3) + 1
                                    toAppendTo.append("Q$quarter '$year")
                                    return toAppendTo
                                }
                                override fun parseObject(source: String?, pos: java.text.ParsePosition?): Any? = null
                            }
                        }
                        TrendPeriod.YEARLY -> DateFormatterUtils.getYearFormatter()
                    }
                }

                val primaryColor = MaterialTheme.colorScheme.primary
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                val textMeasurer = rememberTextMeasurer()
                val labelStyle = MaterialTheme.typography.labelSmall.copy(color = onSurfaceColor)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxAmount = sortedSpending.maxOfOrNull { it.amount } ?: 1L
                    val minAmount = 0L // Start y-axis at 0
                    val amountRange = (maxAmount - minAmount).coerceAtLeast(1L)

                    val textLayoutResults = sortedSpending.map {
                        textMeasurer.measure(dateFormat.format(Date(it.day)), style = labelStyle)
                    }
                    val bottomPadding = textLayoutResults.maxOfOrNull { it.size.height }?.toFloat() ?: 40f
                    val yAxisLabels = 5
                    val yAxisLabelWidth = textMeasurer.measure(
                        CurrencyFormatter.formatAmountCompact(maxAmount), style = labelStyle
                    ).size.width.toFloat() + 16f

                    val chartLeft = yAxisLabelWidth
                    val chartRight = size.width
                    val chartTop = 16f
                    val chartBottom = size.height - bottomPadding - 16f

                    val chartWidth = chartRight - chartLeft
                    val chartHeight = chartBottom - chartTop

                    // Draw horizontal grid lines and y-axis labels
                    for (i in 0 until yAxisLabels) {
                        val fraction = i.toFloat() / (yAxisLabels - 1)
                        val y = chartBottom - (fraction * chartHeight)
                        val value = minAmount + (amountRange * fraction).toLong()

                        drawLine(
                            color = gridColor,
                            start = Offset(chartLeft, y),
                            end = Offset(chartRight, y),
                            strokeWidth = 1f
                        )

                        val textLayoutResult = textMeasurer.measure(
                            CurrencyFormatter.formatAmountCompact(value), style = labelStyle
                        )
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(chartLeft - textLayoutResult.size.width - 8f, y - textLayoutResult.size.height / 2f)
                        )
                    }

                    // Points and Path
                    if (sortedSpending.size > 1) {
                        val path = Path()
                        val points = mutableListOf<Offset>()
                        val stepX = chartWidth / (sortedSpending.size - 1).coerceAtLeast(1)

                        sortedSpending.forEachIndexed { index, data ->
                            val x = chartLeft + index * stepX
                            val fractionY = (data.amount - minAmount).toFloat() / amountRange
                            val y = chartBottom - (fractionY * chartHeight)
                            points.add(Offset(x, y))
                        }

                        path.moveTo(points.first().x, points.first().y)
                        for (i in 0 until points.size - 1) {
                            val p1 = points[i]
                            val p2 = points[i + 1]

                            // Cubic bezier interpolation for smooth curve
                            val controlPoint1 = Offset(p1.x + (p2.x - p1.x) / 2f, p1.y)
                            val controlPoint2 = Offset(p1.x + (p2.x - p1.x) / 2f, p2.y)

                            path.cubicTo(
                                controlPoint1.x, controlPoint1.y,
                                controlPoint2.x, controlPoint2.y,
                                p2.x, p2.y
                            )
                        }

                        // Area fill under the path
                        val fillPath = Path().apply {
                            addPath(path)
                            lineTo(points.last().x, chartBottom)
                            lineTo(points.first().x, chartBottom)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent),
                                startY = chartTop,
                                endY = chartBottom
                            )
                        )

                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = Stroke(width = 6f)
                        )

                        // Draw x-axis labels
                        val labelsToDraw = Math.min(sortedSpending.size, 5) // Draw max 5 labels to avoid overlapping
                        if (labelsToDraw > 0) {
                            val step = Math.max(1, (sortedSpending.size - 1) / (labelsToDraw - 1).coerceAtLeast(1))
                            for (i in sortedSpending.indices step step) {
                                val x = chartLeft + i * stepX
                                val textLayoutResult = textLayoutResults[i]
                                drawText(
                                    textLayoutResult = textLayoutResult,
                                    topLeft = Offset(x - textLayoutResult.size.width / 2f, chartBottom + 8f)
                                )
                            }
                            // ensure last item is always drawn if not included in steps
                            if ((sortedSpending.size - 1) % step != 0) {
                                val lastIndex = sortedSpending.size - 1
                                val x = chartLeft + lastIndex * stepX
                                val textLayoutResult = textLayoutResults[lastIndex]
                                drawText(
                                    textLayoutResult = textLayoutResult,
                                    topLeft = Offset(x - textLayoutResult.size.width, chartBottom + 8f) // align to right
                                )
                            }
                        }
                    } else if (sortedSpending.size == 1) {
                         // single point
                         val x = chartLeft + chartWidth / 2f
                         val fractionY = (sortedSpending.first().amount - minAmount).toFloat() / amountRange
                         val y = chartBottom - (fractionY * chartHeight)

                         drawCircle(
                             color = primaryColor,
                             radius = 6f,
                             center = Offset(x, y)
                         )

                         val textLayoutResult = textLayoutResults.first()
                         drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(x - textLayoutResult.size.width / 2f, chartBottom + 8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdown(
    categories: List<com.sans.finance.data.local.entity.CategorySpent>
) {
    SectionTitle(stringResource(R.string.by_category), icon = Icons.Default.PieChart)

    val totalInCategories = categories.sumOf { it.totalAmount }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (categories.isNotEmpty() && totalInCategories > 0) {
                PieChartWithLabels(
                    categories = categories,
                    totalAmount = totalInCategories
                )
            }

            categories.sortedByDescending { it.totalAmount }.forEachIndexed { index, category ->
                val percent =
                    if (totalInCategories > 0) (category.totalAmount.toFloat() / totalInCategories * 100) else 0f
                val color = pieChartColors[index % pieChartColors.size]

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.0f%%", percent),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = category.categoryName,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        CurrencyFormatter.formatAmount(category.totalAmount),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
fun StatsSimpleCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Long,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                CurrencyFormatter.formatAmount(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun SectionTitle(title: String, icon: ImageVector? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 12.dp)
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
    categories: List<com.sans.finance.data.local.entity.CategorySpent>,
    totalAmount: Long
) {
    if (categories.isEmpty() || totalAmount == 0L) return

    val textMeasurer = rememberTextMeasurer()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val radius = Math.min(canvasWidth, canvasHeight) / 3f
            val center = Offset(canvasWidth / 2, canvasHeight / 2)

            var startAngle = -90f

            categories.sortedByDescending { it.totalAmount }.forEachIndexed { index, category ->
                val sweepAngle = (category.totalAmount.toFloat() / totalAmount) * 360f
                val color = pieChartColors[index % pieChartColors.size]

                // Draw pie slice
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )

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
                    color = color,
                    style = Stroke(width = 2f)
                )

                // Draw text
                val percentage = (category.totalAmount.toFloat() / totalAmount) * 100
                val percentageText = String.format(Locale.US, "%.1f %%", percentage)

                val nameLayoutResult = textMeasurer.measure(
                    text = category.categoryName,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = onSurfaceColor
                    )
                )
                val percentLayoutResult = textMeasurer.measure(
                    text = percentageText,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = onSurfaceColor
                    )
                )

                val textX = if (isRightSide) textEnd.x + 4f else textEnd.x - Math.max(nameLayoutResult.size.width, percentLayoutResult.size.width) - 4f
                val textY = textEnd.y - nameLayoutResult.size.height

                drawText(
                    textLayoutResult = nameLayoutResult,
                    topLeft = Offset(textX, textY)
                )
                drawText(
                    textLayoutResult = percentLayoutResult,
                    topLeft = Offset(textX, textY + nameLayoutResult.size.height)
                )

                startAngle += sweepAngle
            }
        }
    }
}
