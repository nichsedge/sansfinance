package com.sans.finance.presentation.forecasting

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.core.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WealthForecastingScreen(
    onBack: () -> Unit,
    viewModel: WealthForecastingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wealth Trajectory", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Future Wealth Projection",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        CurrencyFormatter.formatAmount(
                            state.projections.lastOrNull()?.value ?: 0L,
                            state.currentCurrency
                        ),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Estimated in ${state.projectionYears} years",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }

            // Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.3f
                    )
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Expected Annual ROI", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${(state.expectedRoi * 100).toInt()}% per year",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Slider(
                        value = state.expectedRoi,
                        onValueChange = { viewModel.updateRoi(it) },
                        valueRange = 0f..0.20f,
                        steps = 19
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoItem(
                            "Monthly Savings",
                            CurrencyFormatter.formatAmount(
                                state.monthlySavings,
                                state.currentCurrency
                            )
                        )
                        InfoItem(
                            "Current Wealth",
                            CurrencyFormatter.formatAmount(
                                state.currentNetWorth,
                                state.currentCurrency
                            )
                        )
                    }
                }
            }

            // Chart
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Growth Trajectory",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TrajectoryChart(
                        projections = state.projections,
                        currencyCode = state.currentCurrency
                    )
                }
            }

            // FIRE Index
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(8.dp))
                        Text("FIRE INDEX", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Target (25x Expenses)", style = MaterialTheme.typography.labelSmall)
                            Text(CurrencyFormatter.formatAmountCompact(state.fireNumber, state.currentCurrency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Years to Freedom", style = MaterialTheme.typography.labelSmall)
                            Text(state.yearsToFire?.let { "$it Years" } ?: "∞", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }

            // Emergency Fund (Sinking Fund)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Emergency Fund (Sinking Fund)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(12.dp))
                    val progress = if (state.emergencyFundTarget > 0) (state.currentEmergencyFund.toFloat() / state.emergencyFundTarget).coerceIn(0f, 1f) else 0f
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
                        color = if (progress >= 1f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${(progress * 100).toInt()}% Covered", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "${CurrencyFormatter.formatAmountCompact(state.currentEmergencyFund, state.currentCurrency)} / ${CurrencyFormatter.formatAmountCompact(state.emergencyFundTarget, state.currentCurrency)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // Milestones
            Text(
                "KEY MILESTONES",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.5.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(5, 10, 20).forEach { year ->
                    val value = state.projections.find { it.year == year }?.value ?: 0L
                    MilestoneItem(year, value, state.currentCurrency)
                }
            }

            // Educational Note
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "This projection assumes constant monthly savings and a fixed annual ROI. Real-world returns will vary.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MilestoneItem(years: Int, amount: Long, currencyCode: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "$years Y",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("Net Worth", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                CurrencyFormatter.formatAmount(amount, currencyCode),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun TrajectoryChart(projections: List<ProjectionPoint>, currencyCode: String) {
    if (projections.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val tooltipStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onPrimary,
        fontWeight = FontWeight.Bold
    )
    val tooltipYearStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(projections.size) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val padding = 60f
                            val chartLeft = 80f
                            val chartRight = size.width - 20f
                            val chartWidth = chartRight - chartLeft

                            val xFraction = (offset.x - chartLeft) / chartWidth
                            val index = (xFraction * (projections.size - 1)).toInt()
                                .coerceIn(0, projections.size - 1)
                            selectedIndex = index
                        },
                        onDrag = { change, _ ->
                            val padding = 60f
                            val chartLeft = 80f
                            val chartRight = size.width - 20f
                            val chartWidth = chartRight - chartLeft

                            val xFraction = (change.position.x - chartLeft) / chartWidth
                            val index = (xFraction * (projections.size - 1)).toInt()
                                .coerceIn(0, projections.size - 1)
                            selectedIndex = index
                        },
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null }
                    )
                }
                .pointerInput(projections.size) {
                    detectTapGestures(
                        onPress = { offset ->
                            val padding = 60f
                            val chartLeft = 80f
                            val chartRight = size.width - 20f
                            val chartWidth = chartRight - chartLeft

                            val xFraction = (offset.x - chartLeft) / chartWidth
                            val index = (xFraction * (projections.size - 1)).toInt()
                                .coerceIn(0, projections.size - 1)
                            selectedIndex = index
                            tryAwaitRelease()
                            selectedIndex = null
                        }
                    )
                }
        ) {
            val padding = 60f
            val chartTop = 20f
            val chartBottom = size.height - padding
            val chartLeft = 80f
            val chartRight = size.width - 20f
            val chartHeight = chartBottom - chartTop
            val chartWidth = chartRight - chartLeft

            val maxVal = projections.maxOf { it.value }.coerceAtLeast(1L)
            val range = maxVal.toFloat()

            // Draw Y-axis (Value)
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

                val label = CurrencyFormatter.formatAmountCompact(value, currencyCode)
                val layout = textMeasurer.measure(label, style = labelStyle)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        chartLeft - layout.size.width - 8f,
                        y - layout.size.height / 2f
                    )
                )
            }

            // Draw X-axis (Years)
            val xSteps = listOf(0, 5, 10, 15, 20, 25).filter { it <= projections.last().year }
            xSteps.forEach { year ->
                val xFraction = year.toFloat() / projections.last().year.toFloat()
                val x = chartLeft + (xFraction * chartWidth)

                val label = "${year}y"
                val layout = textMeasurer.measure(label, style = labelStyle)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(x - layout.size.width / 2f, chartBottom + 8f)
                )
            }

            // Draw Trajectory Path
            val path = Path()
            val fillPath = Path()

            projections.forEachIndexed { index, point ->
                val xFraction = point.year.toFloat() / projections.last().year.toFloat()
                val yFraction = point.value.toFloat() / range

                val x = chartLeft + (xFraction * chartWidth)
                val y = chartBottom - (yFraction * chartHeight)

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            fillPath.lineTo(chartLeft + chartWidth, chartBottom)
            fillPath.lineTo(chartLeft, chartBottom)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent)
                )
            )

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 6f)
            )

            // Draw Tooltip if selected
            selectedIndex?.let { index ->
                val point = projections[index]
                val xFraction = point.year.toFloat() / projections.last().year.toFloat()
                val yFraction = point.value.toFloat() / range

                val x = chartLeft + (xFraction * chartWidth)
                val y = chartBottom - (yFraction * chartHeight)

                // Draw Vertical Line
                drawLine(
                    color = primaryColor.copy(alpha = 0.5f),
                    start = Offset(x, chartTop),
                    end = Offset(x, chartBottom),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // Draw Highlight Dot
                drawCircle(
                    color = primaryColor,
                    radius = 6.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )

                // Tooltip Content
                val yearStr = "Year ${point.year}"
                val valueStr = CurrencyFormatter.formatAmountCompact(point.value, currencyCode)

                val valueLayout = textMeasurer.measure(valueStr, tooltipStyle)
                val yearLayout = textMeasurer.measure(yearStr, tooltipYearStyle)

                val tooltipWidth =
                    maxOf(valueLayout.size.width, yearLayout.size.width) + 24.dp.toPx()
                val tooltipHeight = valueLayout.size.height + yearLayout.size.height + 16.dp.toPx()

                var tooltipX = x - tooltipWidth / 2f
                if (tooltipX < chartLeft) tooltipX = chartLeft + 8.dp.toPx()
                if (tooltipX + tooltipWidth > chartRight) tooltipX =
                    chartRight - tooltipWidth - 8.dp.toPx()

                val tooltipY =
                    (y - tooltipHeight - 16.dp.toPx()).coerceAtLeast(chartTop + 8.dp.toPx())

                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(tooltipX, tooltipY),
                    size = Size(tooltipWidth, tooltipHeight),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )

                drawText(
                    textLayoutResult = yearLayout,
                    topLeft = Offset(tooltipX + 12.dp.toPx(), tooltipY + 8.dp.toPx())
                )
                drawText(
                    textLayoutResult = valueLayout,
                    topLeft = Offset(
                        tooltipX + 12.dp.toPx(),
                        tooltipY + 8.dp.toPx() + yearLayout.size.height
                    )
                )
            }
        }
    }
}
