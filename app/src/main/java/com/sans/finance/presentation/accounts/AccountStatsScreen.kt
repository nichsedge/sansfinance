package com.sans.finance.presentation.accounts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.core.util.CurrencyFormatter
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
                        Text("Total Stats", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = viewModel::onPreviousMonth) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous"
                                )
                            }
                            Text(
                                monthYearFormat.format(state.selectedDate.time),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = viewModel::onNextMonth) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next"
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
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Balance Section
                Column {
                    Text(
                        "Balance",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        CurrencyFormatter.formatAmount(state.totalBalance),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                }

                // Line Chart
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        TotalStatsLineChart(history = state.netWorthHistory)
                    }
                }

                // Bar Chart
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        TotalStatsBarChart(history = state.incomeExpenseHistory)
                    }
                }
            }
        }
    }
}

@Composable
fun TotalStatsLineChart(history: List<Pair<String, Long>>) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 9.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
    val lineColor = Color(0xFFFF6B6B) // Salmon/Red from screenshot
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val chartTop = 40f
            val chartBottom = size.height - 60f
            val chartLeft = 100f
            val chartRight = size.width - 40f
            val chartHeight = chartBottom - chartTop
            val chartWidth = chartRight - chartLeft

            val minVal = history.minOf { it.second }.coerceAtMost(0L)
            val maxVal = history.maxOf { it.second }.coerceAtLeast(0L)
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
                    strokeWidth = 1f
                )

                val label = CurrencyFormatter.formatAmountCompact(value)
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

                history.forEachIndexed { index, data ->
                    val x = chartLeft + index * stepX
                    val y = chartBottom - ((data.second - minVal) / range) * chartHeight

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 4f)
                )

                // Draw points and labels
                history.forEachIndexed { index, data ->
                    val x = chartLeft + index * stepX
                    val y = chartBottom - ((data.second - minVal) / range) * chartHeight

                    drawCircle(
                        color = lineColor,
                        radius = 6f,
                        center = Offset(x, y)
                    )

                    // Month Label
                    val monthLayout = textMeasurer.measure(data.first, style = labelStyle)
                    drawText(
                        textLayoutResult = monthLayout,
                        topLeft = Offset(x - monthLayout.size.width / 2f, chartBottom + 4f)
                    )

                    // Value Label
                    val valueLayout = textMeasurer.measure(
                        CurrencyFormatter.formatAmountCompact(data.second),
                        style = labelStyle
                    )
                    drawText(
                        textLayoutResult = valueLayout,
                        topLeft = Offset(
                            x - valueLayout.size.width / 2f,
                            chartBottom + 4f + monthLayout.size.height
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun TotalStatsBarChart(history: List<Triple<String, Long, Long>>) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 9.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
    val incomeColor = Color(0xFF64B5F6) // Blue
    val expenseColor = Color(0xFFFF8A65) // Red/Orange
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val chartTop = 40f
            val chartBottom = size.height - 80f
            val chartLeft = 100f
            val chartRight = size.width - 40f
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
                    strokeWidth = 1f
                )

                val label = CurrencyFormatter.formatAmountCompact(value)
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
                val barWidth = barGroupWidth * 0.3f
                val spacing = barGroupWidth * 0.1f

                history.forEachIndexed { index, data ->
                    val groupCenterX = chartLeft + index * barGroupWidth + barGroupWidth / 2f

                    // Income Bar (Blue)
                    val incomeHeight = (data.second / range) * chartHeight
                    drawRect(
                        color = incomeColor.copy(alpha = 0.7f),
                        topLeft = Offset(
                            groupCenterX - barWidth - spacing / 2f,
                            chartBottom - incomeHeight
                        ),
                        size = Size(barWidth, incomeHeight)
                    )

                    // Expense Bar (Red)
                    val expenseHeight = (data.third / range) * chartHeight
                    drawRect(
                        color = expenseColor.copy(alpha = 0.7f),
                        topLeft = Offset(groupCenterX + spacing / 2f, chartBottom - expenseHeight),
                        size = Size(barWidth, expenseHeight)
                    )

                    // Month Label
                    val monthLayout = textMeasurer.measure(data.first, style = labelStyle)
                    drawText(
                        textLayoutResult = monthLayout,
                        topLeft = Offset(
                            groupCenterX - monthLayout.size.width / 2f,
                            chartBottom + 4f
                        )
                    )

                    // Income Value Label (Blue)
                    val incomeLayout = textMeasurer.measure(
                        CurrencyFormatter.formatAmountCompact(data.second),
                        style = labelStyle.copy(color = incomeColor)
                    )
                    drawText(
                        textLayoutResult = incomeLayout,
                        topLeft = Offset(
                            groupCenterX - incomeLayout.size.width / 2f,
                            chartBottom + 4f + monthLayout.size.height
                        )
                    )

                    // Expense Value Label (Red)
                    val expenseLayout = textMeasurer.measure(
                        CurrencyFormatter.formatAmountCompact(data.third),
                        style = labelStyle.copy(color = expenseColor)
                    )
                    drawText(
                        textLayoutResult = expenseLayout,
                        topLeft = Offset(
                            groupCenterX - expenseLayout.size.width / 2f,
                            chartBottom + 4f + monthLayout.size.height + incomeLayout.size.height
                        )
                    )
                }
            }
        }
    }
}
