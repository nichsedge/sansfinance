package com.sans.finance.presentation.portfolio.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.data.local.entity.PortfolioTargetEntity
import com.sans.finance.domain.model.AssetClassHealth
import com.sans.finance.domain.model.RiskLevel
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Modern Material 3 Bottom Sheet for configuring asset class threshold targets.
 * Enforces the strict rule: individual and total portfolio targets must NEVER exceed 100%.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetClassTargetBottomSheet(
    target: AssetClassHealth,
    allTargets: List<AssetClassHealth>,
    currentCurrency: String,
    onDismiss: () -> Unit,
    onConfirmTarget: (assetClass: String, newPercentage: Double) -> Unit,
    onConfirmAllTargets: (List<PortfolioTargetEntity>) -> Unit,
    onResetToDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Current state for editing this target
    var targetPercentage by remember {
        mutableDoubleStateOf(target.targetPercentage.coerceIn(0.0, 100.0))
    }

    // Toggle between focused single asset mode and all assets allocation mode
    var isAllAssetsMode by remember { mutableStateOf(false) }

    // Multi-asset state map
    val allTargetsMap = remember(allTargets) {
        mutableStateMapOf<String, Double>().apply {
            allTargets.forEach { put(it.assetClass, it.targetPercentage.coerceIn(0.0, 100.0)) }
        }
    }

    val otherTargetsSum = remember(allTargets, target.assetClass) {
        allTargets
            .filter { !it.assetClass.equals(target.assetClass, ignoreCase = true) }
            .sumOf { it.targetPercentage.coerceIn(0.0, 100.0) }
    }

    val newTotalTarget = otherTargetsSum + targetPercentage
    val maxAvailableForThis = (100.0 - otherTargetsSum).coerceIn(0.0, 100.0)
    val isOverBudget = newTotalTarget > 100.001

    val (assetIcon, assetColor) = remember(target.assetClass) {
        getCategoryIconAndColor(target.assetClass)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(assetColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = assetIcon,
                            contentDescription = null,
                            tint = assetColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isAllAssetsMode) "Portfolio Allocation" else target.assetClass,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isAllAssetsMode) "Target weights" else "Target threshold",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = { isAllAssetsMode = !isAllAssetsMode },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (isAllAssetsMode) "Single" else "All Classes",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            if (!isAllAssetsMode) {
                // Single Asset Class Threshold Revamped View
                SingleAssetThresholdSection(
                    target = target,
                    currentCurrency = currentCurrency,
                    targetPercentage = targetPercentage,
                    onPercentageChange = { targetPercentage = it },
                    otherTargetsSum = otherTargetsSum,
                    newTotalTarget = newTotalTarget,
                    maxAvailable = maxAvailableForThis,
                    isOverBudget = isOverBudget,
                    assetColor = assetColor,
                    onCapToAvailable = {
                        targetPercentage = (maxAvailableForThis * 10).roundToInt() / 10.0
                    },
                    onAutoBalanceOthers = {
                        // Keep current targetPercentage, scale other targets to fit remaining budget
                        val remainingBudget = (100.0 - targetPercentage).coerceAtLeast(0.0)
                        if (otherTargetsSum > 0.0) {
                            val scale = remainingBudget / otherTargetsSum
                            val updatedEntities = allTargets.map { item ->
                                val pct = if (item.assetClass.equals(target.assetClass, ignoreCase = true)) {
                                    targetPercentage
                                } else {
                                    (item.targetPercentage * scale * 10).roundToInt() / 10.0
                                }
                                PortfolioTargetEntity(
                                    assetClass = item.assetClass,
                                    targetPercentage = pct.coerceIn(0.0, 100.0),
                                    description = "",
                                    riskLevel = item.riskLevel.name
                                )
                            }
                            onConfirmAllTargets(updatedEntities)
                            onDismiss()
                        } else {
                            targetPercentage = 100.0
                        }
                    },
                    onConfirm = {
                        onConfirmTarget(target.assetClass, targetPercentage)
                        onDismiss()
                    }
                )
            } else {
                // Multi-Asset Allocation Manager View
                AllAssetsAllocationSection(
                    allTargets = allTargets,
                    allTargetsMap = allTargetsMap,
                    onResetToDefaults = {
                        onResetToDefaults()
                        onDismiss()
                    },
                    onSaveAll = {
                        val entities = allTargets.map { item ->
                            PortfolioTargetEntity(
                                assetClass = item.assetClass,
                                targetPercentage = (allTargetsMap[item.assetClass] ?: item.targetPercentage).coerceIn(0.0, 100.0),
                                description = "",
                                riskLevel = item.riskLevel.name
                            )
                        }
                        onConfirmAllTargets(entities)
                        onDismiss()
                    }
                )
            }
        }
    }
}

/**
 * Revamped single asset class threshold slider & control panel.
 */
@Composable
private fun SingleAssetThresholdSection(
    target: AssetClassHealth,
    currentCurrency: String,
    targetPercentage: Double,
    onPercentageChange: (Double) -> Unit,
    otherTargetsSum: Double,
    newTotalTarget: Double,
    maxAvailable: Double,
    isOverBudget: Boolean,
    assetColor: Color,
    onCapToAvailable: () -> Unit,
    onAutoBalanceOthers: () -> Unit,
    onConfirm: () -> Unit
) {
    // Current vs Target Snapshot Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Actual Allocation Column
            Column {
                Text(
                    text = "Current Actual",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${String.format(Locale.US, "%.1f", target.currentPercentage)}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = CurrencyFormatter.formatAmountCompact((target.currentAmount * 100).toLong(), currentCurrency),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Target Allocation Column
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Target Threshold",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${String.format(Locale.US, "%.1f", targetPercentage)}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                val diff = targetPercentage - target.currentPercentage
                val diffSign = if (diff >= 0) "+" else ""
                val diffColor = if (diff >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                Text(
                    text = "$diffSign${String.format(Locale.US, "%.1f", diff)}% drift",
                    style = MaterialTheme.typography.labelSmall,
                    color = diffColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Interactive Stepped Slider & Controls
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TARGET PERCENTAGE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.outline,
                    letterSpacing = 1.sp
                )

                // Stepper Fine Tuning Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val next = (targetPercentage - 5.0).coerceAtLeast(0.0)
                            onPercentageChange((next * 10).roundToInt() / 10.0)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("-5", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    }

                    IconButton(
                        onClick = {
                            val next = (targetPercentage - 1.0).coerceAtLeast(0.0)
                            onPercentageChange((next * 10).roundToInt() / 10.0)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Step Down", modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = {
                            val next = (targetPercentage + 1.0).coerceAtMost(100.0)
                            onPercentageChange((next * 10).roundToInt() / 10.0)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Step Up", modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = {
                            val next = (targetPercentage + 5.0).coerceAtMost(100.0)
                            onPercentageChange((next * 10).roundToInt() / 10.0)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("+5", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Smooth Material 3 Slider (0% to 100%, never exceeds 100%)
            Slider(
                value = targetPercentage.toFloat().coerceIn(0f, 100f),
                onValueChange = { raw ->
                    val rounded = (raw * 2.0).roundToInt() / 2.0 // Snap to 0.5% increments
                    onPercentageChange(rounded.coerceIn(0.0, 100.0))
                },
                valueRange = 0f..100f,
                steps = 199,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    activeTrackColor = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            // Preset Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(0.0, 5.0, 10.0, 20.0, 25.0, 50.0).forEach { preset ->
                    FilterChip(
                        selected = targetPercentage == preset,
                        onClick = { onPercentageChange(preset) },
                        label = { Text("${preset.toInt()}%", style = MaterialTheme.typography.labelSmall) },
                        shape = CircleShape
                    )
                }

                if (maxAvailable > 0.0 && maxAvailable < 100.0) {
                    FilterChip(
                        selected = (targetPercentage * 10).roundToInt() == (maxAvailable * 10).roundToInt(),
                        onClick = onCapToAvailable,
                        label = {
                            Text(
                                "Remaining (${String.format(Locale.US, "%.1f", maxAvailable)}%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        shape = CircleShape
                    )
                }
            }
        }
    }

    // Portfolio Total Target Budget Meter (Strict <= 100% rule)
    PortfolioAllocationBudgetMeter(
        thisAssetTarget = targetPercentage,
        otherAssetsTarget = otherTargetsSum,
        totalTarget = newTotalTarget,
        isOverBudget = isOverBudget,
        maxAvailable = maxAvailable,
        onCapToAvailable = onCapToAvailable,
        onAutoBalanceOthers = onAutoBalanceOthers
    )

    // Save Button
    Button(
        onClick = onConfirm,
        enabled = !isOverBudget,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            if (isOverBudget) "Total Exceeds 100% Budget" else "Save Target (${String.format(Locale.US, "%.1f", targetPercentage)}%)",
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Visual budget meter verifying that total targets across the portfolio never exceed 100%.
 */
@Composable
private fun PortfolioAllocationBudgetMeter(
    thisAssetTarget: Double,
    otherAssetsTarget: Double,
    totalTarget: Double,
    isOverBudget: Boolean,
    maxAvailable: Double,
    onCapToAvailable: () -> Unit,
    onAutoBalanceOthers: () -> Unit
) {
    val badgeColor by animateColorAsState(
        targetValue = if (isOverBudget) MaterialTheme.colorScheme.error else Color(0xFF10B981),
        label = "budget_color"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (isOverBudget) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = if (isOverBudget) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isOverBudget) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "PORTFOLIO TOTAL TARGET",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", totalTarget)}% / 100%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Stacked Linear Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Other Assets segment
                    val otherFraction = (otherAssetsTarget / 100.0).coerceIn(0.0, 1.0).toFloat()
                    if (otherFraction > 0.001f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(otherFraction)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        )
                    }

                    // This Asset segment
                    val thisFraction = (thisAssetTarget / 100.0).coerceIn(0.0, 1.0).toFloat()
                    if (thisFraction > 0.001f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(thisFraction)
                                .background(if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                        )
                    }

                    // Unallocated runway
                    val unallocated = (100.0 - totalTarget).coerceAtLeast(0.0) / 100.0
                    val unallocatedFraction = unallocated.coerceIn(0.0, 1.0).toFloat()
                    if (unallocatedFraction > 0.001f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(unallocatedFraction)
                                .background(Color.Transparent)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Subtitle & Smart Auto-Fix Actions
            if (isOverBudget) {
                val overflow = totalTarget - 100.0
                Text(
                    text = "Target exceeds 100% budget by +${String.format(Locale.US, "%.1f", overflow)}%. Reduce this target or auto-balance others.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCapToAvailable,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            "Cap to ${String.format(Locale.US, "%.1f", maxAvailable)}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onAutoBalanceOthers,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Auto-Balance",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                val unallocated = 100.0 - totalTarget
                Text(
                    text = if (unallocated > 0.1) "${String.format(Locale.US, "%.1f", unallocated)}% unallocated runway remaining."
                    else "Portfolio targets are fully allocated (100%).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * Multi-asset allocation editor enabling adjusting all asset classes with sliders at once.
 */
@Composable
private fun AllAssetsAllocationSection(
    allTargets: List<AssetClassHealth>,
    allTargetsMap: MutableMap<String, Double>,
    onResetToDefaults: () -> Unit,
    onSaveAll: () -> Unit
) {
    val totalSum = allTargetsMap.values.sum()
    val isOverBudget = totalSum > 100.001

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Overall Budget Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(
                    if (isOverBudget) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "TOTAL PORTFOLIO ALLOCATION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Text(
                    if (isOverBudget) "Exceeds 100% budget by +${String.format(Locale.US, "%.1f", totalSum - 100.0)}%"
                    else "${String.format(Locale.US, "%.1f", 100.0 - totalSum)}% unallocated",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Text(
                "${String.format(Locale.US, "%.1f", totalSum)}% / 100%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }

        // List of all asset class sliders
        allTargets.forEach { item ->
            val currentValue = allTargetsMap[item.assetClass] ?: item.targetPercentage
            val (icon, color) = getCategoryIconAndColor(item.assetClass)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(item.assetClass, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "${String.format(Locale.US, "%.1f", currentValue)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black,
                            color = color
                        )
                    }

                    Slider(
                        value = currentValue.toFloat().coerceIn(0f, 100f),
                        onValueChange = { raw ->
                            val rounded = (raw * 2.0).roundToInt() / 2.0
                            allTargetsMap[item.assetClass] = rounded.coerceIn(0.0, 100.0)
                        },
                        valueRange = 0f..100f,
                        steps = 199,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Reset to Defaults & Save Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onResetToDefaults,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Defaults")
            }

            Button(
                onClick = onSaveAll,
                enabled = !isOverBudget,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.weight(2f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isOverBudget) "Exceeds 100%" else "Save All Targets", fontWeight = FontWeight.Bold)
            }
        }
    }
}
