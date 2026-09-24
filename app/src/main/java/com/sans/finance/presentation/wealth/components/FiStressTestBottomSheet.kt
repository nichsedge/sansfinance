package com.sans.finance.presentation.wealth.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.domain.model.EmergencyFundStressTest
import com.sans.finance.domain.model.StressTestScenarioType
import com.sans.finance.presentation.components.PrivacyText
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiStressTestBottomSheet(
    stressTest: EmergencyFundStressTest?,
    baselineRunwayMonths: Double,
    totalAssets: Long,
    annualExpense: Long,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean,
    isManualEnabled: Boolean,
    manualAnnualExpense: Long,
    selectedScenarioType: StressTestScenarioType?,
    onScenarioSelected: (StressTestScenarioType?) -> Unit,
    onManualToggle: (Boolean) -> Unit,
    onManualAmountChange: (Long) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val haptic = LocalHapticFeedback.current
    var selectedSwr by remember { mutableFloatStateOf(0.04f) }
    var expenseMultiplier by remember { mutableFloatStateOf(1.0f) }
    var manualInput by remember(manualAnnualExpense) {
        mutableStateOf((manualAnnualExpense / 100).toString())
    }

    val activeScenario = stressTest?.scenarios?.find { it.type == selectedScenarioType }
    val displayedRunway = activeScenario?.runwayMonths ?: baselineRunwayMonths

    val statusColor = when {
        displayedRunway >= 12.0 -> Color(0xFF10B981)
        displayedRunway >= 6.0 -> Color(0xFF4CAF50)
        displayedRunway >= 3.0 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    val statusLabel = activeScenario?.tier?.label ?: when {
        baselineRunwayMonths >= 12.0 -> "Antifragile (>12 Mo)"
        baselineRunwayMonths >= 6.0 -> "Healthy Runway"
        baselineRunwayMonths >= 3.0 -> "Moderate Buffer"
        else -> "Needs Attention"
    }

    val effectiveExpense = if (isManualEnabled && manualAnnualExpense > 0) manualAnnualExpense else annualExpense
    val simulatedAnnualExpense = (effectiveExpense * expenseMultiplier).toLong()
    val simulatedTargetFire = if (selectedSwr > 0f) (simulatedAnnualExpense / selectedSwr).toLong() else 0L
    val simulatedYearsOfCover = if (simulatedAnnualExpense > 0) totalAssets.toDouble() / simulatedAnnualExpense else 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Stress Test & Shock Simulation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Model economic disruptions & fire parameters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section 1: Scenario Selector Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SELECT SHOCK SCENARIO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                val scenarios = listOf(
                    null to "Baseline",
                    StressTestScenarioType.ZERO_INCOME to "Job Loss",
                    StressTestScenarioType.PARTIAL_INCOME to "-50% Pay",
                    StressTestScenarioType.INFLATION_SURGE to "+25% Surge",
                    StressTestScenarioType.MARKET_DRAWDOWN to "-30% Crash"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    scenarios.forEach { (type, label) ->
                        val isSelected = selectedScenarioType == type
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onScenarioSelected(type)
                                }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Scenario Impact Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = activeScenario?.title ?: "Standard Baseline",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = activeScenario?.description ?: "Current financial trajectory without external shocks.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Projected Safety Runway",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (displayedRunway >= 99.0) ">99 Months" else "${String.format(Locale.US, "%.1f", displayedRunway)} Months",
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                    fontWeight = FontWeight.Black,
                                    color = statusColor
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = statusColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    if (activeScenario != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Stressed Burn",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                PrivacyText(
                                    amount = activeScenario.monthlyBurn,
                                    currencyCode = currencyCode,
                                    isVisible = !isPrivacyModeEnabled,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Net Cash Flow",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                PrivacyText(
                                    amount = activeScenario.netMonthlyCashFlow,
                                    currencyCode = currencyCode,
                                    isVisible = !isPrivacyModeEnabled,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeScenario.netMonthlyCashFlow >= 0) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                    )
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Section 2: FIRE Runway Sandbox
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "FIRE RUNWAY SANDBOX",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // SWR Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SWR:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(0.03f to "3.0%", 0.035f to "3.5%", 0.04f to "4.0% (Standard)").forEach { (swr, label) ->
                        val isSelected = selectedSwr == swr
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSwr = swr },
                            label = {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                // Spending Multiplier Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Spending: ${(expenseMultiplier * 100).toInt()}% (${String.format(Locale.US, "%.1f", simulatedYearsOfCover)} yrs cover)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (isPrivacyModeEnabled) "••••••" else CurrencyFormatter.formatAmount(simulatedAnnualExpense / 12, currencyCode) + "/mo",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = expenseMultiplier,
                        onValueChange = { expenseMultiplier = it },
                        valueRange = 0.5f..1.5f,
                        steps = 9
                    )
                }

                // Simulated Nest Egg Target
                val milestoneBadge = when {
                    simulatedYearsOfCover >= (1.0 / selectedSwr) -> "👑 Full Financial Independence"
                    simulatedYearsOfCover >= 12.5 -> "🏔️ Lean FIRE Range"
                    simulatedYearsOfCover >= 6.0 -> "🧭 Half FI Milestone"
                    simulatedYearsOfCover >= 1.0 -> "⛵ Coast Cushion"
                    else -> "🛡️ Emergency Shield"
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Simulated Nest Egg Target",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            PrivacyText(
                                amount = simulatedTargetFire,
                                currencyCode = currencyCode,
                                isVisible = !isPrivacyModeEnabled,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                milestoneBadge,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Manual Override Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Manual Expense Override",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = isManualEnabled,
                        onCheckedChange = { onManualToggle(it) }
                    )
                }

                if (isManualEnabled) {
                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = {
                            manualInput = it
                            it.toLongOrNull()?.let { amount ->
                                onManualAmountChange(amount * 100)
                            }
                        },
                        label = { Text("Annual Expense ($currencyCode)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
