package com.sans.finance.presentation.forecasting

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.presentation.components.AppTopBar
import com.sans.finance.presentation.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WealthForecastingScreen(
    onBack: () -> Unit,
    viewModel: WealthForecastingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Wealth Trajectory",
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primary,
                alpha = 0.15f
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Future Wealth Projection",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        CurrencyFormatter.formatAmount(
                            state.projections.lastOrNull()?.value ?: 0L,
                            state.currentCurrency
                        ),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (state.extraMonthlyContribution > 0) {
                        Text(
                            "What-if: " + CurrencyFormatter.formatAmount(
                                state.whatIfProjections.lastOrNull()?.value ?: 0L,
                                state.currentCurrency
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Text(
                        "Estimated in ${state.projectionYears} years",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "EXPECTED ANNUAL ROI",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "${(state.expectedRoi * 100).toInt()}% per year",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                    Slider(
                        value = state.expectedRoi,
                        onValueChange = { viewModel.updateRoi(it) },
                        valueRange = 0f..0.20f,
                        steps = 19
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "MARKET VOLATILITY (STD DEV)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "${(state.volatility * 100).toInt()}% annual volatility",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                    Slider(
                        value = state.volatility,
                        onValueChange = { viewModel.updateVolatility(it) },
                        valueRange = 0.05f..0.35f,
                        steps = 29
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "EXTRA MONTHLY SAVINGS (WHAT-IF)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        CurrencyFormatter.formatAmount(state.extraMonthlyContribution, state.currentCurrency),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Black
                    )
                    Slider(
                        value = state.extraMonthlyContribution.toFloat(),
                        onValueChange = { viewModel.updateExtraContribution(it.toLong()) },
                        valueRange = 0f..100_000_000f,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

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

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "PROJECTION HORIZON",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "${state.projectionYears} years",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                    Slider(
                        value = state.projectionYears.toFloat(),
                        onValueChange = { viewModel.updateProjectionYears(it.toInt()) },
                        valueRange = 5f..50f,
                        steps = 8
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "EMERGENCY FUND TARGET",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "${state.emergencyFundMonths} months of expenses",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                    Slider(
                        value = state.emergencyFundMonths.toFloat(),
                        onValueChange = { viewModel.updateEmergencyFundMonths(it.toInt()) },
                        valueRange = 1f..24f,
                        steps = 22
                    )
                }
            }

            // Chart
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Growth Trajectory",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "1,000 Monte Carlo stochastic runs",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Chart Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp, 3.dp)
                                    .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Planned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp, 6.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), MaterialTheme.shapes.extraSmall)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("10th–90th %ile", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp, 2.dp)
                                    .background(MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.extraSmall)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Median", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TrajectoryChart(
                        projections = state.projections,
                        whatIfProjections = if (state.extraMonthlyContribution > 0) state.whatIfProjections else null,
                        p10Projections = state.monteCarloP10,
                        p50Projections = state.monteCarloP50,
                        p90Projections = state.monteCarloP90,
                        currencyCode = state.currentCurrency
                    )
                }
            }

            // Monte Carlo Probability & FIRE Index
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "RETIREMENT PROBABILITY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (state.fireSuccessRate >= 0.85f) Color(0xFF4CAF50).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${(state.fireSuccessRate * 100).toInt()}% Success",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = if (state.fireSuccessRate >= 0.85f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("Target FIRE Number", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                CurrencyFormatter.formatAmountCompact(state.fireNumber, state.currentCurrency),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Est. Freedom", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                state.yearsToFire?.let { "$it Years" } ?: "∞",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    // 3-Tile Percentile Bento Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pessimistic
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Pessimistic", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("10th %ile", style = TextStyle(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    CurrencyFormatter.formatAmountCompact(state.monteCarloP10.lastOrNull()?.value ?: 0L, state.currentCurrency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Median
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Median", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("50th %ile", style = TextStyle(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    CurrencyFormatter.formatAmountCompact(state.monteCarloP50.lastOrNull()?.value ?: 0L, state.currentCurrency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Optimistic
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Optimistic", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("90th %ile", style = TextStyle(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    CurrencyFormatter.formatAmountCompact(state.monteCarloP90.lastOrNull()?.value ?: 0L, state.currentCurrency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            // Emergency Fund
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Emergency Fund (${state.emergencyFundMonths} Months)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black
                        )
                        val progress = if (state.emergencyFundTarget > 0) (state.currentEmergencyFund.toFloat() / state.emergencyFundTarget).coerceIn(0f, 1f) else 0f
                        Surface(
                            shape = CircleShape,
                            color = if (progress >= 1f) Color(0xFF4CAF50).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "${(progress * 100).toInt()}% Covered",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (progress >= 1f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    val progress = if (state.emergencyFundTarget > 0) (state.currentEmergencyFund.toFloat() / state.emergencyFundTarget).coerceIn(0f, 1f) else 0f
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(MaterialTheme.shapes.small),
                        color = if (progress >= 1f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Target: ${CurrencyFormatter.formatAmountCompact(state.emergencyFundTarget, state.currentCurrency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Current: ${CurrencyFormatter.formatAmountCompact(state.currentEmergencyFund, state.currentCurrency)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Educational Note
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.large,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
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
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun TrajectoryChart(
    projections: List<ProjectionPoint>,
    whatIfProjections: List<ProjectionPoint>? = null,
    p10Projections: List<ProjectionPoint> = emptyList(),
    p50Projections: List<ProjectionPoint> = emptyList(),
    p90Projections: List<ProjectionPoint> = emptyList(),
    currencyCode: String
) {
    if (projections.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val whatIfColor = MaterialTheme.colorScheme.tertiary
    val mcFanColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val p50Color = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val tooltipStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onPrimary,
        fontWeight = FontWeight.Bold
    )
    val tooltipYearStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
    )

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(projections.size) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val chartLeft = 96f
                            val chartRight = size.width - 24f
                            val chartWidth = chartRight - chartLeft
                            val index = ((offset.x - chartLeft) / chartWidth * (projections.size - 1)).toInt()
                                .coerceIn(0, projections.size - 1)
                            if (selectedIndex != index) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                selectedIndex = index
                            }
                        },
                        onDrag = { change, _ ->
                            val chartLeft = 96f
                            val chartRight = size.width - 24f
                            val chartWidth = chartRight - chartLeft
                            val index = ((change.position.x - chartLeft) / chartWidth * (projections.size - 1)).toInt()
                                .coerceIn(0, projections.size - 1)
                            if (selectedIndex != index) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                selectedIndex = index
                            }
                        },
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null }
                    )
                }
                .pointerInput(projections.size) {
                    detectTapGestures(
                        onPress = { offset ->
                            val chartLeft = 96f
                            val chartRight = size.width - 24f
                            val chartWidth = chartRight - chartLeft
                            val index = ((offset.x - chartLeft) / chartWidth * (projections.size - 1)).toInt()
                                .coerceIn(0, projections.size - 1)
                            if (selectedIndex != index) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                selectedIndex = index
                            }
                            tryAwaitRelease()
                            selectedIndex = null
                        }
                    )
                }
        ) {
            val padding = 60f
            val chartTop = 28f
            val chartBottom = size.height - padding
            val chartLeft = 96f
            val chartRight = size.width - 24f
            val chartHeight = chartBottom - chartTop
            val chartWidth = chartRight - chartLeft

            val maxValNormal = projections.maxOf { it.value }
            val maxValWhatIf = whatIfProjections?.maxOf { it.value } ?: 0L
            val maxValP90 = p90Projections.maxOfOrNull { it.value } ?: 0L
            val peakVal = maxOf(maxValNormal, maxValWhatIf, maxValP90).coerceAtLeast(1L)
            val range = (peakVal.toDouble() * 1.08).toFloat()

            // Draw Y-axis
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
                drawText(textLayoutResult = layout, color = labelStyle.color, topLeft = Offset(chartLeft - layout.size.width - 8f, y - layout.size.height / 2f))
            }

            // Draw X-axis
            val xSteps = listOf(0, 5, 10, 15, 20, 25, 30, 40, 50).filter { it <= projections.last().year }
            xSteps.forEach { year ->
                val xFraction = year.toFloat() / projections.last().year.toFloat()
                val x = chartLeft + (xFraction * chartWidth)
                val layout = textMeasurer.measure("${year}y", style = labelStyle)
                drawText(textLayoutResult = layout, color = labelStyle.color, topLeft = Offset(x - layout.size.width / 2f, chartBottom + 8f))
            }

            // Helper to build path
            fun buildPath(data: List<ProjectionPoint>): Path {
                val path = Path()
                data.forEachIndexed { index, point ->
                    val x = chartLeft + (point.year.toFloat() / data.last().year.toFloat() * chartWidth)
                    val y = chartBottom - (point.value.toFloat() / range * chartHeight)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                return path
            }

            // Draw Monte Carlo Fan Area (P10 to P90 corridor)
            if (p10Projections.size == p90Projections.size && p10Projections.isNotEmpty()) {
                val fanPath = Path()
                p90Projections.forEachIndexed { index, point ->
                    val x = chartLeft + (point.year.toFloat() / p90Projections.last().year.toFloat() * chartWidth)
                    val y = chartBottom - (point.value.toFloat() / range * chartHeight)
                    if (index == 0) fanPath.moveTo(x, y) else fanPath.lineTo(x, y)
                }
                for (i in p10Projections.indices.reversed()) {
                    val point = p10Projections[i]
                    val x = chartLeft + (point.year.toFloat() / p10Projections.last().year.toFloat() * chartWidth)
                    val y = chartBottom - (point.value.toFloat() / range * chartHeight)
                    fanPath.lineTo(x, y)
                }
                fanPath.close()
                drawPath(fanPath, mcFanColor)
            }

            // Deterministic normal path
            drawPath(buildPath(projections), primaryColor, style = Stroke(width = 5f))

            // What-if path
            whatIfProjections?.let {
                drawPath(buildPath(it), whatIfColor, style = Stroke(width = 3.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)))
            }

            // Monte Carlo Median (P50) path
            if (p50Projections.isNotEmpty()) {
                drawPath(buildPath(p50Projections), p50Color.copy(alpha = 0.7f), style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)))
            }

            // Selected Tooltip
            selectedIndex?.let { idx ->
                val p = projections[idx]
                val x = chartLeft + (p.year.toFloat() / projections.last().year.toFloat() * chartWidth)
                val y = chartBottom - (p.value.toFloat() / range * chartHeight)

                drawLine(
                    color = primaryColor.copy(alpha = 0.5f),
                    start = Offset(x, chartTop),
                    end = Offset(x, chartBottom),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                drawCircle(primaryColor, 6.dp.toPx(), Offset(x, y))

                val pWhatIf = whatIfProjections?.getOrNull(idx)
                val p10 = p10Projections.getOrNull(idx)
                val p50 = p50Projections.getOrNull(idx)
                val p90 = p90Projections.getOrNull(idx)

                // Tooltip box
                val yearStr = "Year ${p.year}"
                val valStr = "Base: " + CurrencyFormatter.formatAmountCompact(p.value, currencyCode)
                val mcStr = if (p50 != null && p10 != null && p90 != null) {
                    "MC 50%: ${CurrencyFormatter.formatAmountCompact(p50.value, currencyCode)} (${CurrencyFormatter.formatAmountCompact(p10.value, currencyCode)} - ${CurrencyFormatter.formatAmountCompact(p90.value, currencyCode)})"
                } else null

                val whatIfStr = pWhatIf?.let { "What-if: " + CurrencyFormatter.formatAmountCompact(it.value, currencyCode) }

                val layouts = listOfNotNull(
                    textMeasurer.measure(yearStr, tooltipYearStyle),
                    textMeasurer.measure(valStr, tooltipStyle),
                    mcStr?.let { textMeasurer.measure(it, tooltipStyle.copy(color = Color(0xFFFFD54F))) },
                    whatIfStr?.let { textMeasurer.measure(it, tooltipStyle.copy(color = whatIfColor)) }
                )

                val tooltipWidth = layouts.maxOf { it.size.width } + 24.dp.toPx()
                val tooltipHeight = layouts.sumOf { it.size.height } + 16.dp.toPx()
                val tx = (x - tooltipWidth / 2f).coerceIn(chartLeft + 4f, chartRight - tooltipWidth - 4f)
                val ty = (y - tooltipHeight - 16.dp.toPx()).coerceAtLeast(chartTop + 4f)

                drawRoundRect(primaryColor.copy(alpha = 0.92f), Offset(tx, ty), Size(tooltipWidth, tooltipHeight), CornerRadius(8.dp.toPx()))
                var currentY = ty + 8.dp.toPx()
                layouts.forEach {
                    drawText(textLayoutResult = it, color = Color.White, topLeft = Offset(tx + 12.dp.toPx(), currentY))
                    currentY += it.size.height
                }
            }
        }
    }
}

