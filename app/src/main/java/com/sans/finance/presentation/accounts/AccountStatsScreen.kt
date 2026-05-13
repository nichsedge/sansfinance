package com.sans.finance.presentation.accounts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.R
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.presentation.components.GlassCard
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountStatsScreen(
    onBack: () -> Unit,
    viewModel: AccountStatsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.US) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.account_stats), fontWeight = FontWeight.Black)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.5f
                                    ), CircleShape
                                )
                                .padding(horizontal = 4.dp)
                        ) {
                            IconButton(
                                onClick = viewModel::onPreviousMonth,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                monthYearFormat.format(state.selectedDate.time),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = viewModel::onNextMonth,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Balance Section
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primary,
                    alpha = 0.1f
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Cash Liquidity",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            CurrencyFormatter.formatAmount(
                                state.totalBalance,
                                state.currentCurrency
                            ),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Line Chart
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Balance History",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(20.dp))
                        TotalStatsLineChart(
                            history = state.balanceHistory,
                            currencyCode = state.currentCurrency
                        )
                    }
                }

                // Bar Chart
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Income vs Expenses",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(20.dp))
                        TotalStatsBarChart(
                            history = state.incomeExpenseHistory,
                            currencyCode = state.currentCurrency
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun TotalStatsLineChart(history: List<Pair<String, Long>>, currencyCode: String) {
    if (history.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        fontWeight = FontWeight.Bold
    )
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val tooltipStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onPrimary,
        fontWeight = FontWeight.Black
    )
    val tooltipDateStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
        fontWeight = FontWeight.Bold
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(history.size) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val chartLeft = 100f
                            val chartRight = size.width - 20f
                            val chartWidth = chartRight - chartLeft
                            val stepX = chartWidth / (history.size - 1).coerceAtLeast(1)

                            val index = ((offset.x - chartLeft) / stepX).toInt()
                                .coerceIn(0, history.size - 1)
                            selectedIndex = index
                        },
                        onDrag = { change, _ ->
                            val chartLeft = 100f
                            val chartRight = size.width - 20f
                            val chartWidth = chartRight - chartLeft
                            val stepX = chartWidth / (history.size - 1).coerceAtLeast(1)

                            val index = ((change.position.x - chartLeft) / stepX).toInt()
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
                            val chartLeft = 100f
                            val chartRight = size.width - 20f
                            val chartWidth = chartRight - chartLeft
                            val stepX = chartWidth / (history.size - 1).coerceAtLeast(1)

                            val index = ((offset.x - chartLeft) / stepX).toInt()
                                .coerceIn(0, history.size - 1)
                            selectedIndex = index
                            tryAwaitRelease()
                            selectedIndex = null
                        }
                    )
                }
        ) {
            val chartTop = 20f
            val chartBottom = size.height - 40f
            val chartLeft = 100f
            val chartRight = size.width - 20f
            val chartHeight = chartBottom - chartTop
            val chartWidth = chartRight - chartLeft

            val minVal = history.minOf { it.second }.coerceAtMost(0L)
            val maxVal = history.maxOf { it.second }.coerceAtLeast(1L)
            val range = (maxVal - minVal).toFloat().takeIf { it > 0 } ?: 1f

            // Draw Y-axis labels and grid lines
            val yLines = 5
            for (i in 0 until yLines) {
                val fraction = i.toFloat() / (yLines - 1)
                val y = chartBottom - (fraction * chartHeight)
                val value = minVal + (fraction * range).toLong()

                drawLine(
                    color = gridColor,
                    start = Offset(chartLeft, y),
                    end = Offset(chartRight, y),
                    strokeWidth = 2f
                )

                val label = CurrencyFormatter.formatAmountCompact(value, currencyCode)
                val layout = textMeasurer.measure(label, style = labelStyle)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        chartLeft - layout.size.width - 12f,
                        y - layout.size.height / 2f
                    )
                )
            }

            // Draw Line and Points
            if (history.size > 1) {
                val stepX = chartWidth / (history.size - 1)
                val path = Path()
                val points = mutableListOf<Offset>()

                history.forEachIndexed { index, data ->
                    val x = chartLeft + index * stepX
                    val y = chartBottom - ((data.second - minVal) / range) * chartHeight
                    points.add(Offset(x, y))

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(
                        width = 6f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )

                // Draw points and labels
                points.forEachIndexed { index, point ->
                    if (history.size < 15) {
                        drawCircle(
                            color = lineColor,
                            radius = 6f,
                            center = point
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3f,
                            center = point
                        )
                    }

                    // Month Label
                    if (index == 0 || index == history.size - 1 || index % (history.size / 4).coerceAtLeast(
                            1
                        ) == 0
                    ) {
                        val monthLayout =
                            textMeasurer.measure(history[index].first, style = labelStyle)
                        drawText(
                            textLayoutResult = monthLayout,
                            topLeft = Offset(
                                point.x - monthLayout.size.width / 2f,
                                chartBottom + 12f
                            )
                        )
                    }
                }

                // Draw Tooltip if selected
                selectedIndex?.let { index ->
                    val point = points[index]
                    val data = history[index]

                    // Vertical guide line
                    drawLine(
                        color = lineColor.copy(alpha = 0.2f),
                        start = Offset(point.x, chartTop),
                        end = Offset(point.x, chartBottom),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // Highlight circle
                    drawCircle(
                        color = lineColor,
                        radius = 10f,
                        center = point
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 5f,
                        center = point
                    )

                    // Tooltip box
                    val valueStr = CurrencyFormatter.formatAmountCompact(data.second, currencyCode)
                    val dateStr = data.first

                    val valueLayout = textMeasurer.measure(valueStr, tooltipStyle)
                    val dateLayout = textMeasurer.measure(dateStr, tooltipDateStyle)

                    val tooltipWidth = maxOf(valueLayout.size.width, dateLayout.size.width) + 32f
                    val tooltipHeight = valueLayout.size.height + dateLayout.size.height + 24f

                    var tooltipX = point.x - tooltipWidth / 2f
                    if (tooltipX < chartLeft) tooltipX = chartLeft
                    if (tooltipX + tooltipWidth > chartRight) tooltipX = chartRight - tooltipWidth

                    val tooltipY = (point.y - tooltipHeight - 24f).coerceAtLeast(chartTop)

                    drawRoundRect(
                        color = lineColor,
                        topLeft = Offset(tooltipX, tooltipY),
                        size = Size(tooltipWidth, tooltipHeight),
                        cornerRadius = CornerRadius(12f, 12f)
                    )

                    drawText(
                        textLayoutResult = dateLayout,
                        topLeft = Offset(tooltipX + 16f, tooltipY + 12f)
                    )
                    drawText(
                        textLayoutResult = valueLayout,
                        topLeft = Offset(tooltipX + 16f, tooltipY + 12f + dateLayout.size.height)
                    )
                }
            }
        }
    }
}

@Composable
fun TotalStatsBarChart(history: List<Triple<String, Long, Long>>, currencyCode: String) {
    if (history.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        fontWeight = FontWeight.Bold
    )
    val incomeColor = MaterialTheme.colorScheme.tertiary
    val expenseColor = MaterialTheme.colorScheme.error
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val tooltipStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Black
    )

    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(history.size) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val chartLeft = 100f
                            val chartRight = size.width - 20f
                            val chartWidth = chartRight - chartLeft
                            val barGroupWidth = chartWidth / history.size
                            val index = ((offset.x - chartLeft) / barGroupWidth).toInt()
                                .coerceIn(0, history.size - 1)
                            selectedIndex = index
                        },
                        onDrag = { change, _ ->
                            val chartLeft = 100f
                            val chartRight = size.width - 20f
                            val chartWidth = chartRight - chartLeft
                            val barGroupWidth = chartWidth / history.size
                            val index = ((change.position.x - chartLeft) / barGroupWidth).toInt()
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
                            val chartLeft = 100f
                            val chartRight = size.width - 20f
                            val chartWidth = chartRight - chartLeft
                            val barGroupWidth = chartWidth / history.size
                            val index = ((offset.x - chartLeft) / barGroupWidth).toInt()
                                .coerceIn(0, history.size - 1)
                            selectedIndex = index
                            tryAwaitRelease()
                            selectedIndex = null
                        }
                    )
                }
        ) {
            val chartTop = 40f
            val chartBottom = size.height - 60f
            val chartLeft = 100f
            val chartRight = size.width - 20f
            val chartHeight = chartBottom - chartTop
            val chartWidth = chartRight - chartLeft

            val maxVal = history.maxOf { maxOf(it.second, it.third) }.coerceAtLeast(1L)
            val range = maxVal.toFloat()

            // Draw Y-axis labels and grid lines
            val yLines = 5
            for (i in 0 until yLines) {
                val fraction = i.toFloat() / (yLines - 1)
                val y = chartBottom - (fraction * chartHeight)
                val value = (fraction * range).toLong()

                drawLine(
                    color = gridColor,
                    start = Offset(chartLeft, y),
                    end = Offset(chartRight, y),
                    strokeWidth = 2f
                )

                val label = CurrencyFormatter.formatAmountCompact(value, currencyCode)
                val layout = textMeasurer.measure(label, style = labelStyle)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        chartLeft - layout.size.width - 12f,
                        y - layout.size.height / 2f
                    )
                )
            }

            // Draw Bars
            if (history.isNotEmpty()) {
                val barGroupWidth = chartWidth / history.size
                val barWidth = barGroupWidth * 0.35f
                val spacing = barGroupWidth * 0.05f

                history.forEachIndexed { index, data ->
                    val groupCenterX = chartLeft + index * barGroupWidth + barGroupWidth / 2f

                    // Highlight background for selected group
                    if (selectedIndex == index) {
                        drawRoundRect(
                            color = highlightColor,
                            topLeft = Offset(chartLeft + index * barGroupWidth + 4f, chartTop),
                            size = Size(barGroupWidth - 8f, chartBottom - chartTop),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                    }

                    // Income Bar (Emerald)
                    val incomeHeight = (data.second / range) * chartHeight
                    drawRoundRect(
                        color = if (selectedIndex == null || selectedIndex == index) incomeColor else incomeColor.copy(
                            alpha = 0.3f
                        ),
                        topLeft = Offset(
                            groupCenterX - barWidth - spacing / 2f,
                            chartBottom - incomeHeight
                        ),
                        size = Size(barWidth, incomeHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )

                    // Expense Bar (Rose)
                    val expenseHeight = (data.third / range) * chartHeight
                    drawRoundRect(
                        color = if (selectedIndex == null || selectedIndex == index) expenseColor else expenseColor.copy(
                            alpha = 0.3f
                        ),
                        topLeft = Offset(groupCenterX + spacing / 2f, chartBottom - expenseHeight),
                        size = Size(barWidth, expenseHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )

                    // Month Label
                    if (index == 0 || index == history.size - 1 || index % (history.size / 4).coerceAtLeast(
                            1
                        ) == 0
                    ) {
                        val monthLayout = textMeasurer.measure(data.first, style = labelStyle)
                        drawText(
                            textLayoutResult = monthLayout,
                            topLeft = Offset(
                                groupCenterX - monthLayout.size.width / 2f,
                                chartBottom + 8f
                            )
                        )
                    }

                    // Tooltip for Bar Chart
                    if (selectedIndex == index) {
                        val incStr = "In: ${
                            CurrencyFormatter.formatAmountCompact(
                                data.second,
                                currencyCode
                            )
                        }"
                        val expStr = "Out: ${
                            CurrencyFormatter.formatAmountCompact(
                                data.third,
                                currencyCode
                            )
                        }"

                        val incLayout =
                            textMeasurer.measure(incStr, tooltipStyle.copy(color = incomeColor))
                        val expLayout =
                            textMeasurer.measure(expStr, tooltipStyle.copy(color = expenseColor))

                        val tWidth = maxOf(incLayout.size.width, expLayout.size.width) + 32f
                        val tHeight = incLayout.size.height + expLayout.size.height + 24f

                        var tX = groupCenterX - tWidth / 2f
                        if (tX < chartLeft) tX = chartLeft
                        if (tX + tWidth > chartRight) tX = chartRight - tWidth

                        val tY = chartTop - 10f

                        drawRoundRect(
                            color = surfaceColor,
                            topLeft = Offset(tX, tY),
                            size = Size(tWidth, tHeight),
                            cornerRadius = CornerRadius(12f, 12f),
                            style = Stroke(width = 2f)
                        )
                        drawRoundRect(
                            color = surfaceColor,
                            topLeft = Offset(tX, tY),
                            size = Size(tWidth, tHeight),
                            cornerRadius = CornerRadius(12f, 12f)
                        )

                        drawText(textLayoutResult = incLayout, topLeft = Offset(tX + 16f, tY + 12f))
                        drawText(
                            textLayoutResult = expLayout,
                            topLeft = Offset(tX + 16f, tY + 12f + incLayout.size.height)
                        )
                    }
                }
            }
        }
    }
}
