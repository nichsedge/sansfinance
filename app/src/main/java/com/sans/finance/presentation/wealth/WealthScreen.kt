package com.sans.finance.presentation.wealth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sans.finance.core.util.CurrencyFormatter
import androidx.compose.foundation.layout.BoxWithConstraints
import com.sans.finance.presentation.wealth.components.SavingsVelocityCard
import com.sans.finance.presentation.wealth.components.FiStressTestBottomSheet
import com.sans.finance.domain.model.EmergencyFundStressTest
import com.sans.finance.domain.model.StressTestScenarioType
import com.sans.finance.domain.model.WealthDistributionTab
import com.sans.finance.presentation.components.AppTopBar
import com.sans.finance.presentation.components.GlassCard
import com.sans.finance.presentation.components.PrivacyText
import com.sans.finance.presentation.dashboard.SectionHeader
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WealthScreen(
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onOpenAccounts: () -> Unit,
    onOpenPortfolio: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenRecurringExpenses: () -> Unit = {},
    onOpenForecasting: () -> Unit,
    onOpenMonthlyReview: () -> Unit,
    viewModel: WealthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showStressTestSheet by remember { mutableStateOf(false) }
    var selectedScenarioType by remember { mutableStateOf<StressTestScenarioType?>(null) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppTopBar(
                title = "Wealth",
                scrollBehavior = scrollBehavior,
                actions = {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(36.dp)
                                .padding(8.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButton(onClick = { viewModel.triggerCloudSync(context) }) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync Portfolio & Database",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            viewModel.togglePrivacyMode()
                        }
                    ) {
                        Icon(
                            imageVector = if (state.isPrivacyModeEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (state.isPrivacyModeEnabled) "Privacy Mode Enabled" else "Privacy Mode Disabled",
                            tint = if (state.isPrivacyModeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        BoxWithConstraints(
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
        ) {
            val isWide = maxWidth >= 680.dp

            if (isWide) {
                // Adaptive Wide-Screen Dual-Pane Layout (Foldables / Tablets / Landscape)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = paddingValues.calculateStartPadding(layoutDirection) + 20.dp,
                            top = paddingValues.calculateTopPadding() + 12.dp,
                            end = paddingValues.calculateEndPadding(layoutDirection) + 20.dp,
                            bottom = paddingValues.calculateBottomPadding() + 16.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column: Asset Allocation, Financial Hub, Strategy & Planning
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (state.wealthDistribution.isNotEmpty()) {
                            item {
                                WealthAllocationCard(
                                    totalAssets = state.totalAssets,
                                    netWorth = state.netWorth,
                                    distribution = state.wealthDistribution,
                                    selectedTab = state.wealthDistributionTab,
                                    onTabSelected = viewModel::setWealthDistributionTab,
                                    currencyCode = state.currencyCode,
                                    isPrivacyModeEnabled = state.isPrivacyModeEnabled
                                )
                            }
                        }

                        item {
                            SectionHeader("FINANCIAL HUB")
                            Spacer(modifier = Modifier.height(4.dp))
                            FinancialHubBentoGrid(
                                cashAssets = state.cashAssets,
                                accountsCount = state.accountsCount,
                                portfolioValue = state.portfolioValue,
                                sourcesCount = state.portfolioSources.size,
                                liabilities = state.liabilities,
                                activeDebtCount = state.activeDebtCount,
                                goalsCount = state.goalsCount,
                                avgGoalProgress = state.avgGoalProgress,
                                currencyCode = state.currencyCode,
                                isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                                onOpenAccounts = onOpenAccounts,
                                onOpenPortfolio = onOpenPortfolio,
                                onOpenDebts = onOpenDebts,
                                onOpenGoals = onOpenGoals
                            )
                        }

                        item {
                            SectionHeader("STRATEGY & PLANNING")
                            Spacer(modifier = Modifier.height(4.dp))
                            StrategyPlanningCard(
                                onOpenBudgets = onOpenBudgets,
                                onOpenRecurringExpenses = onOpenRecurringExpenses,
                                onOpenForecasting = onOpenForecasting,
                                onOpenMonthlyReview = onOpenMonthlyReview
                            )
                        }

                        item { Spacer(modifier = Modifier.height(32.dp)) }
                    }

                    // Right Column: FI Suite, Emergency Stress Test, Savings Velocity
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            FinancialIndependenceSuiteCard(
                                runwayMonths = state.runwayMonths,
                                monthlyBurn = state.monthlyBurn,
                                cashAssets = state.cashAssets,
                                monthlyPassiveIncome = state.monthlyPassiveIncome,
                                annualPassiveIncome = state.annualPassiveIncome,
                                nextPayoutDateStr = state.nextPayoutDateStr,
                                fiCoveragePct = state.fiCoveragePct,
                                fiStage = state.fiStage,
                                fiNextStageGap = state.fiNextStageGap,
                                currencyCode = state.currencyCode,
                                isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                                stressTest = state.emergencyStressTest,
                                selectedScenarioType = selectedScenarioType,
                                onOpenStressTest = { showStressTestSheet = true },
                                onResetScenario = { selectedScenarioType = null }
                            )
                        }

                        state.savingsVelocity?.let { velocity ->
                            item {
                                SavingsVelocityCard(
                                    summary = velocity,
                                    isPrivacyMode = state.isPrivacyModeEnabled
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(32.dp)) }
                    }
                }
            } else {
                // Compact Single-Column Layout
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = paddingValues.calculateStartPadding(layoutDirection) + 16.dp,
                        top = paddingValues.calculateTopPadding() + 8.dp,
                        end = paddingValues.calculateEndPadding(layoutDirection) + 16.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Hero: Capital & Asset Allocation
                    if (state.wealthDistribution.isNotEmpty()) {
                        item {
                            WealthAllocationCard(
                                totalAssets = state.totalAssets,
                                netWorth = state.netWorth,
                                distribution = state.wealthDistribution,
                                selectedTab = state.wealthDistributionTab,
                                onTabSelected = viewModel::setWealthDistributionTab,
                                currencyCode = state.currencyCode,
                                isPrivacyModeEnabled = state.isPrivacyModeEnabled
                            )
                        }
                    }

                    // 2. Financial Hub (2x2 Bento Grid)
                    item {
                        SectionHeader("FINANCIAL HUB")
                        Spacer(modifier = Modifier.height(4.dp))
                        FinancialHubBentoGrid(
                            cashAssets = state.cashAssets,
                            accountsCount = state.accountsCount,
                            portfolioValue = state.portfolioValue,
                            sourcesCount = state.portfolioSources.size,
                            liabilities = state.liabilities,
                            activeDebtCount = state.activeDebtCount,
                            goalsCount = state.goalsCount,
                            avgGoalProgress = state.avgGoalProgress,
                            currencyCode = state.currencyCode,
                            isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                            onOpenAccounts = onOpenAccounts,
                            onOpenPortfolio = onOpenPortfolio,
                            onOpenDebts = onOpenDebts,
                            onOpenGoals = onOpenGoals
                        )
                    }

                    // 3. Strategy & Planning Section
                    item {
                        SectionHeader("STRATEGY & PLANNING")
                        Spacer(modifier = Modifier.height(4.dp))
                        StrategyPlanningCard(
                            onOpenBudgets = onOpenBudgets,
                            onOpenRecurringExpenses = onOpenRecurringExpenses,
                            onOpenForecasting = onOpenForecasting,
                            onOpenMonthlyReview = onOpenMonthlyReview
                        )
                    }

                    // 4. The Financial Independence & Safety Suite (with integrated Shock Simulator)
                    item {
                        FinancialIndependenceSuiteCard(
                            runwayMonths = state.runwayMonths,
                            monthlyBurn = state.monthlyBurn,
                            cashAssets = state.cashAssets,
                            monthlyPassiveIncome = state.monthlyPassiveIncome,
                            annualPassiveIncome = state.annualPassiveIncome,
                            nextPayoutDateStr = state.nextPayoutDateStr,
                            fiCoveragePct = state.fiCoveragePct,
                            fiStage = state.fiStage,
                            fiNextStageGap = state.fiNextStageGap,
                            currencyCode = state.currencyCode,
                            isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                            stressTest = state.emergencyStressTest,
                            selectedScenarioType = selectedScenarioType,
                            onOpenStressTest = { showStressTestSheet = true },
                            onResetScenario = { selectedScenarioType = null }
                        )
                    }

                    // 5. Savings Velocity & Momentum
                    state.savingsVelocity?.let { velocity ->
                        item {
                            SavingsVelocityCard(
                                summary = velocity,
                                isPrivacyMode = state.isPrivacyModeEnabled
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }

        if (showStressTestSheet) {
            FiStressTestBottomSheet(
                stressTest = state.emergencyStressTest,
                baselineRunwayMonths = state.runwayMonths,
                totalAssets = state.totalAssets,
                annualExpense = state.annualExpense,
                currencyCode = state.currencyCode,
                isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                isManualEnabled = state.isFireManualEnabled,
                manualAnnualExpense = state.manualFireAnnualExpense,
                selectedScenarioType = selectedScenarioType,
                onScenarioSelected = { selectedScenarioType = it },
                onManualToggle = viewModel::setFireManualEnabled,
                onManualAmountChange = viewModel::setManualFireAnnualExpense,
                onDismiss = { showStressTestSheet = false }
            )
        }
    }
}

@Composable
fun WealthAllocationCard(
    totalAssets: Long,
    netWorth: Long,
    distribution: Map<String, Long>,
    selectedTab: WealthDistributionTab,
    onTabSelected: (WealthDistributionTab) -> Unit,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean
) {
    val total = distribution.values.sumOf { kotlin.math.abs(it) }.coerceAtLeast(1L)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Asset Allocation",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        "Capital distribution across assets",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                PrivacyText(
                    amount = totalAssets,
                    currencyCode = currencyCode,
                    isVisible = !isPrivacyModeEnabled,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        CircleShape
                    )
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WealthDistributionTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { onTabSelected(tab) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (tab) {
                                WealthDistributionTab.CURRENCY -> "Currency"
                                WealthDistributionTab.ASSET_CLASS -> "Asset Class"
                                WealthDistributionTab.CATEGORY -> "Category"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Proportional Allocation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                distribution.entries.forEachIndexed { index, entry ->
                    val weight = (kotlin.math.abs(entry.value).toFloat() / total.toFloat()).coerceAtLeast(0.01f)
                    val color = getDistributionColor(index)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Legend Breakdown
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                distribution.entries.forEachIndexed { index, entry ->
                    val entryValue = kotlin.math.abs(entry.value)
                    val percentage = (entryValue.toDouble() / total.toDouble() * 100.0)
                    val color = getDistributionColor(index)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                entry.key,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                "${String.format(Locale.US, "%.1f", percentage)}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PrivacyText(
                            amount = entry.value,
                            currencyCode = currencyCode,
                            isVisible = !isPrivacyModeEnabled,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun getDistributionColor(index: Int): Color {
    return when (index % 5) {
        0 -> Color(0xFF2196F3) // Blue
        1 -> Color(0xFF4CAF50) // Green
        2 -> Color(0xFFFF9800) // Orange
        3 -> Color(0xFF9C27B0) // Purple
        else -> Color(0xFF00BCD4) // Cyan
    }
}

@Composable
fun FinancialIndependenceSuiteCard(
    runwayMonths: Double,
    monthlyBurn: Long,
    cashAssets: Long,
    monthlyPassiveIncome: Long,
    annualPassiveIncome: Long,
    nextPayoutDateStr: String,
    fiCoveragePct: Double,
    fiStage: String,
    fiNextStageGap: Long,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean,
    stressTest: EmergencyFundStressTest? = null,
    selectedScenarioType: StressTestScenarioType? = null,
    onOpenStressTest: () -> Unit,
    onResetScenario: () -> Unit = {}
) {
    val activeScenario = stressTest?.scenarios?.find { it.type == selectedScenarioType }
    val displayedRunway = activeScenario?.runwayMonths ?: runwayMonths

    val statusColor = when {
        displayedRunway >= 12.0 -> Color(0xFF10B981)
        displayedRunway >= 6.0 -> Color(0xFF4CAF50)
        displayedRunway >= 3.0 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    val statusLabel = activeScenario?.tier?.label ?: when {
        runwayMonths >= 12.0 -> "Antifragile (>12 Mo)"
        runwayMonths >= 6.0 -> "Healthy Runway"
        runwayMonths >= 3.0 -> "Moderate Buffer"
        else -> "Needs Attention"
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎯", fontSize = 15.sp)
                    }
                    Column {
                        Text(
                            text = "Financial Independence",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = fiStage,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", fiCoveragePct)}% Covered",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { (fiCoveragePct / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (fiCoveragePct >= 100.0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Lean FI (50%)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Full FI (100%)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Two Pillar Boxes (Runway & Passive Inflows)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pillar 1: Emergency Runway
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("🛡️", fontSize = 13.sp)
                                Text(
                                    "Runway",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selectedScenarioType != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                    modifier = Modifier.clickable { onResetScenario() }
                                ) {
                                    Text(
                                        text = "Shock ✕",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (displayedRunway >= 99.0) ">99 Mo" else "${String.format(Locale.US, "%.1f", displayedRunway)} Mo",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                            fontWeight = FontWeight.Black,
                            color = statusColor,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                // Pillar 2: Passive Inflows
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("💰", fontSize = 13.sp)
                            Text(
                                "Passive Yield",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        PrivacyText(
                            amount = monthlyPassiveIncome,
                            currencyCode = currencyCode,
                            isVisible = !isPrivacyModeEnabled,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF4CAF50),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = "/month",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Milestone Rows
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), MaterialTheme.shapes.medium)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isFortress = runwayMonths >= 12.0
                val isLeanFi = fiCoveragePct >= 50.0
                val isFullFi = fiCoveragePct >= 100.0

                MilestoneRowItem(
                    title = "1. Emergency Fortress (12+ Mo)",
                    status = if (isFortress) "${String.format(Locale.US, "%.0f", runwayMonths)} Mo" else "In Progress",
                    isComplete = isFortress
                )
                MilestoneRowItem(
                    title = "2. Lean FI (50% Living Burn)",
                    status = if (isLeanFi) "Achieved" else "${String.format(Locale.US, "%.1f", fiCoveragePct)}% / 50%",
                    isComplete = isLeanFi
                )
                MilestoneRowItem(
                    title = "3. Full FI (100% Living Burn)",
                    status = if (isFullFi) "Achieved" else "${String.format(Locale.US, "%.1f", fiCoveragePct)}% / 100%",
                    isComplete = isFullFi
                )
            }

            // Next Milestone Gap Banner
            if (fiNextStageGap > 0L) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "💡", fontSize = 13.sp)
                    Text(
                        text = "Gap to next milestone:",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PrivacyText(
                        amount = fiNextStageGap,
                        currencyCode = currencyCode,
                        isVisible = !isPrivacyModeEnabled,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                    Text(
                        text = "/mo",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Stress Test & Simulation Action Pill
            Surface(
                onClick = onOpenStressTest,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Stress Test & Shock Simulator",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (selectedScenarioType != null) {
                                    "Simulating: ${activeScenario?.title ?: "Custom Shock"}"
                                } else {
                                    "Model job loss, market crashes & FIRE targets"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MilestoneRowItem(
    title: String,
    status: String,
    isComplete: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isComplete) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (isComplete) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isComplete) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isComplete) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.size(8.dp))

        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isComplete) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun FinancialHubBentoGrid(
    cashAssets: Long,
    accountsCount: Int,
    portfolioValue: Long,
    sourcesCount: Int,
    liabilities: Long,
    activeDebtCount: Int,
    goalsCount: Int,
    avgGoalProgress: Float,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean,
    onOpenAccounts: () -> Unit,
    onOpenPortfolio: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenGoals: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoTile(
                modifier = Modifier.weight(1f),
                title = "Accounts",
                subtitle = "$accountsCount active accounts",
                amount = cashAssets,
                currencyCode = currencyCode,
                icon = Icons.Default.AccountBalanceWallet,
                iconTint = MaterialTheme.colorScheme.primary,
                isPrivacyModeEnabled = isPrivacyModeEnabled,
                onClick = onOpenAccounts
            )
            BentoTile(
                modifier = Modifier.weight(1f),
                title = "Portfolio",
                subtitle = "$sourcesCount sources",
                amount = portfolioValue,
                currencyCode = currencyCode,
                icon = Icons.Default.PieChart,
                iconTint = Color(0xFF4CAF50),
                isPrivacyModeEnabled = isPrivacyModeEnabled,
                onClick = onOpenPortfolio
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoTile(
                modifier = Modifier.weight(1f),
                title = "Debt Strategist",
                subtitle = if (activeDebtCount > 0) "$activeDebtCount liabilities" else "Debt-free 🎉",
                amount = liabilities,
                currencyCode = currencyCode,
                icon = Icons.Default.Payments,
                iconTint = if (liabilities > 0) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                isPrivacyModeEnabled = isPrivacyModeEnabled,
                onClick = onOpenDebts
            )
            BentoTile(
                modifier = Modifier.weight(1f),
                title = "Goals",
                subtitle = "$goalsCount targets (${(avgGoalProgress * 100).toInt()}%)",
                amount = null,
                customAmountText = if (goalsCount > 0) "${(avgGoalProgress * 100).toInt()}% Done" else "No Goals",
                currencyCode = currencyCode,
                icon = Icons.Default.Flag,
                iconTint = MaterialTheme.colorScheme.tertiary,
                isPrivacyModeEnabled = isPrivacyModeEnabled,
                onClick = onOpenGoals
            )
        }
    }
}

@Composable
private fun BentoTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    amount: Long?,
    customAmountText: String? = null,
    currencyCode: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isPrivacyModeEnabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (amount != null) {
                PrivacyText(
                    amount = amount,
                    currencyCode = currencyCode,
                    isVisible = !isPrivacyModeEnabled,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            } else if (customAmountText != null) {
                Text(
                    text = customAmountText,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.Black,
                    color = iconTint,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StrategyPlanningCard(
    onOpenBudgets: () -> Unit,
    onOpenRecurringExpenses: () -> Unit = {},
    onOpenForecasting: () -> Unit,
    onOpenMonthlyReview: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            StrategyNavRow(
                title = "Budgeting",
                subtitle = "Plan spending & monitor limits",
                leadingIcon = Icons.AutoMirrored.Filled.ShowChart,
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = onOpenBudgets
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            StrategyNavRow(
                title = "Subscriptions & Recurring",
                subtitle = "Fixed commitments & 10y cost impact",
                leadingIcon = Icons.Default.Sync,
                iconTint = MaterialTheme.colorScheme.secondary,
                onClick = onOpenRecurringExpenses
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            StrategyNavRow(
                title = "Net Worth Forecast",
                subtitle = "Future wealth trajectory & simulation",
                leadingIcon = Icons.Default.AutoAwesome,
                iconTint = MaterialTheme.colorScheme.tertiary,
                onClick = onOpenForecasting
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            StrategyNavRow(
                title = "Monthly Review",
                subtitle = "Monthly insights & closure",
                leadingIcon = Icons.Default.CalendarMonth,
                iconTint = Color(0xFF4CAF50),
                onClick = onOpenMonthlyReview
            )
        }
    }
}

@Composable
private fun StrategyNavRow(
    title: String,
    subtitle: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
        )
    }
}


