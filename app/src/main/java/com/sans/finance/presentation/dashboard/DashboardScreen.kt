package com.sans.finance.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.presentation.components.CategoryIcon
import com.sans.finance.presentation.components.GlassCard
import com.sans.finance.presentation.components.PrivacyText
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onTransactionClick: (Long) -> Unit,
    onRecurringExpensesClick: () -> Unit,
    onInstallmentsClick: () -> Unit,
    onWealthForecastingClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
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
                .padding(paddingValues),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                NetWorthCard(
                    netWorth = state.netWorth,
                    assets = state.totalAssets,
                    liabilities = state.totalLiabilities,
                    currencyCode = state.currentCurrency,
                    isPrivacyModeEnabled = state.isPrivacyModeEnabled
                )
            }

            item {
                FinancialFreedomCard(
                    yearsOfCover = state.financialFreedomYears,
                    freedomScore = state.financialFreedomScore,
                    totalAssets = state.totalAssets,
                    annualExpense = state.annualExpense,
                    currencyCode = state.currentCurrency,
                    isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                    isManualEnabled = state.isFireManualEnabled,
                    manualAnnualExpense = state.manualFireAnnualExpense,
                    onManualToggle = { viewModel.setFireManualEnabled(it) },
                    onManualAmountChange = { viewModel.setManualFireAnnualExpense(it) }
                )
            }

            item {
                MonthlyCashFlowCard(
                    income = state.monthlyIncome,
                    expense = state.monthlyExpense,
                    cashFlow = state.monthlyCashFlow,
                    savingsRate = state.monthlySavingsRate,
                    currencyCode = state.currentCurrency,
                    isPrivacyModeEnabled = state.isPrivacyModeEnabled
                )
            }

            if (state.globalBudget > 0L) {
                item {
                    GlobalBudgetCard(
                        budget = state.globalBudget,
                        spent = state.globalSpent,
                        daysLeft = state.daysLeftInMonth,
                        currencyCode = state.currentCurrency,
                        isPrivacyModeEnabled = state.isPrivacyModeEnabled
                    )
                }
            }

            item {
                ForecastCard(
                    projectedBalance = state.projectedBalance30Days,
                    trendData = state.last30DaysTrend,
                    currencyCode = state.currentCurrency,
                    isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                    onClick = onWealthForecastingClick
                )
            }

            if (state.wealthDistribution.isNotEmpty()) {
                item {
                    WealthDistributionCard(
                        distribution = state.wealthDistribution,
                        selectedTab = state.wealthDistributionTab,
                        onTabSelected = viewModel::setWealthDistributionTab,
                        currencyCode = state.currentCurrency,
                        isPrivacyModeEnabled = state.isPrivacyModeEnabled
                    )
                }
            }

            if (state.aiSuggestions.isNotEmpty()) {
                item {
                    AiAdvisorCard(suggestions = state.aiSuggestions)
                }
            }

            if (state.goals.isNotEmpty()) {
                item {
                    SectionHeader("GOAL PROGRESS")
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.goals.forEach { goal ->
                            DashboardGoalItem(
                                goal,
                                state.currentCurrency,
                                state.isPrivacyModeEnabled
                            )
                        }
                    }
                }
            }

            if (state.upcomingBills.isNotEmpty()) {
                item {
                    var showBillsMenu by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader("UPCOMING BILLS")
                        Box {
                            Text(
                                "See All",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { showBillsMenu = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            DropdownMenu(
                                expanded = showBillsMenu,
                                onDismissRequest = { showBillsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Recurring Payments") },
                                    onClick = {
                                        showBillsMenu = false
                                        onRecurringExpensesClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Installments") },
                                    onClick = {
                                        showBillsMenu = false
                                        onInstallmentsClick()
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.upcomingBills.forEach { bill ->
                            DashboardBillItem(
                                bill,
                                state.currentCurrency,
                                state.isPrivacyModeEnabled
                            )
                        }
                    }
                }
            }
            if (state.recentTransactions.isNotEmpty()) {
                item {
                    SectionHeader("RECENT TRANSACTIONS")
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.recentTransactions.forEach { transaction ->
                            RecentTransactionItem(
                                transaction = transaction,
                                currencyCode = state.currentCurrency,
                                isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                                onClick = { onTransactionClick(transaction.id) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun NetWorthCard(
    netWorth: Long,
    assets: Long,
    liabilities: Long,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean
) {
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
                "Total Net Worth",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            PrivacyText(
                amount = netWorth,
                currencyCode = currencyCode,
                isVisible = !isPrivacyModeEnabled,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BreakdownItem(
                    "Assets",
                    assets,
                    MaterialTheme.colorScheme.tertiary,
                    currencyCode,
                    isPrivacyModeEnabled
                )
                VerticalDivider(
                    modifier = Modifier.height(32.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                BreakdownItem(
                    "Liabilities",
                    liabilities,
                    MaterialTheme.colorScheme.error,
                    currencyCode,
                    isPrivacyModeEnabled
                )
            }
        }
    }
}

@Composable
fun BreakdownItem(
    label: String,
    amount: Long,
    color: Color,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold
        )
        PrivacyText(
            amount = amount,
            currencyCode = currencyCode,
            isVisible = !isPrivacyModeEnabled,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

@Composable
fun ForecastCard(
    projectedBalance: Long,
    trendData: List<Long>,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean,
    onClick: () -> Unit
) {
    var selectedIndex by remember { mutableStateOf(-1) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (selectedIndex == -1) "30-Day Forecast" else "Historical Point ${30 - selectedIndex}d ago",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
                PrivacyText(
                    amount = if (selectedIndex == -1) projectedBalance else trendData[selectedIndex],
                    currencyCode = currencyCode,
                    isVisible = !isPrivacyModeEnabled,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            com.sans.finance.presentation.components.Sparkline(
                data = trendData,
                modifier = Modifier
                    .width(100.dp)
                    .height(40.dp),
                color = MaterialTheme.colorScheme.secondary,
                lineWidth = 3f,
                onValueSelected = { selectedIndex = it }
            )
        }
    }
}

@Composable
fun WealthDistributionCard(
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
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "Wealth Distribution",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        CircleShape
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WealthDistributionTab.values().forEach { tab ->
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
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stacked Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                distribution.entries.forEachIndexed { index, entry ->
                    val weight =
                        (kotlin.math.abs(entry.value).toFloat() / total.toFloat()).coerceAtLeast(
                            0.01f
                        )
                    val color = when (index % 4) {
                        0 -> MaterialTheme.colorScheme.primary
                        1 -> MaterialTheme.colorScheme.secondary
                        2 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Legend
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                distribution.entries.forEachIndexed { index, entry ->
                    val entryValue = kotlin.math.abs(entry.value)
                    val percentage = (entryValue.toFloat() / total.toFloat() * 100).toInt()

                    val color = when (index % 4) {
                        0 -> MaterialTheme.colorScheme.primary
                        1 -> MaterialTheme.colorScheme.secondary
                        2 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                entry.key,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "$percentage%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PrivacyText(
                            amount = entry.value,
                            currencyCode = currencyCode,
                            isVisible = !isPrivacyModeEnabled,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyCashFlowCard(
    income: Long,
    expense: Long,
    cashFlow: Long,
    savingsRate: Float,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Monthly Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Income box
                FlowBox(
                    label = "Income",
                    amount = income,
                    currencyCode = currencyCode,
                    color = MaterialTheme.colorScheme.tertiary,
                    icon = Icons.Default.ArrowUpward,
                    isPrivacyModeEnabled = isPrivacyModeEnabled,
                    modifier = Modifier.weight(1f)
                )
                // Expense box
                FlowBox(
                    label = "Expense",
                    amount = expense,
                    currencyCode = currencyCode,
                    color = MaterialTheme.colorScheme.error,
                    icon = Icons.Default.ArrowDownward,
                    isPrivacyModeEnabled = isPrivacyModeEnabled,
                    modifier = Modifier.weight(1f)
                )
            }

            // Cash Flow and Savings Rate Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        MaterialTheme.shapes.large
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Cash Flow",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    PrivacyText(
                        amount = cashFlow,
                        currencyCode = currencyCode,
                        isVisible = !isPrivacyModeEnabled,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = if (cashFlow >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Savings Rate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                com.sans.finance.presentation.components.CircularGauge(
                    progress = savingsRate,
                    size = 80.dp,
                    strokeWidth = 10.dp,
                    color = if (savingsRate >= 0.2f) MaterialTheme.colorScheme.tertiary
                    else if (savingsRate >= 0f) Color(0xFFFFC107)
                    else MaterialTheme.colorScheme.error,
                    isPrivacyModeEnabled = isPrivacyModeEnabled
                )
            }
        }
    }
}

@Composable
fun FlowBox(
    label: String,
    amount: Long,
    currencyCode: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPrivacyModeEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.08f), MaterialTheme.shapes.large)
            .border(1.dp, color.copy(alpha = 0.1f), MaterialTheme.shapes.large)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
        PrivacyText(
            amount = amount,
            currencyCode = currencyCode,
            isVisible = !isPrivacyModeEnabled,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

@Composable
fun AiAdvisorCard(suggestions: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "AI Insights",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            suggestions.forEach { suggestion ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        "•",
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        suggestion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardGoalItem(goal: DashboardGoal, currencyCode: String, isPrivacyModeEnabled: Boolean) {
    val progress = goal.progress
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    goal.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!isPrivacyModeEnabled) {
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black
                    )
                } else {
                    Text(
                        "••%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun DashboardBillItem(
    bill: com.sans.finance.domain.model.Expense,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        ),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    icon = bill.categoryIcon ?: "📄",
                    fontSize = 14.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.title.ifBlank { bill.categoryName ?: "Upcoming Bill" },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = if (bill.isInstallmentPayment) "Installment ${bill.installmentMonth}/${bill.installmentTotalMonths}"
                    else "Recurring Payment",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PrivacyText(
                amount = bill.amount,
                currencyCode = bill.currency,
                isVisible = !isPrivacyModeEnabled,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}


@Composable
fun GlobalBudgetCard(
    budget: Long,
    spent: Long,
    daysLeft: Int,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean
) {
    val progress = (spent.toFloat() / budget.toFloat()).coerceIn(0f, 1f)
    val isOverBudget = spent > budget
    val remaining = (budget - spent).coerceAtLeast(0L)
    val color =
        if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (isOverBudget) color.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isOverBudget) color.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(
                alpha = 0.5f
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
                    Text(
                        "Global Budget",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$daysLeft days left",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    if (isOverBudget) "OVER BUDGET" else "ON TRACK",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .background(
                            (if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary).copy(
                                alpha = 0.1f
                            ),
                            CircleShape
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    PrivacyText(
                        amount = spent,
                        currencyCode = currencyCode,
                        isVisible = !isPrivacyModeEnabled,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (isOverBudget) "Overspent" else "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    PrivacyText(
                        amount = if (isOverBudget) spent - budget else remaining,
                        currencyCode = currencyCode,
                        isVisible = !isPrivacyModeEnabled,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = color
                    )
                }
            }
        }
    }
}

@Composable
fun FinancialFreedomCard(
    yearsOfCover: Double,
    freedomScore: Float,
    totalAssets: Long,
    annualExpense: Long,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean,
    isManualEnabled: Boolean,
    manualAnnualExpense: Long,
    onManualToggle: (Boolean) -> Unit,
    onManualAmountChange: (Long) -> Unit
) {
    var showHelp by remember { mutableStateOf(false) }
    var manualInput by remember(manualAnnualExpense) {
        mutableStateOf((manualAnnualExpense / 100).toString())
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = {
                Text(
                    "Financial Freedom 101",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "This score estimates how long your wealth lasts without a salary.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Years of Cover",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val assetsFormatted =
                            if (isPrivacyModeEnabled) "••••" else CurrencyFormatter.formatAmount(
                                totalAssets,
                                currencyCode
                            )
                        val expenseFormatted =
                            if (isPrivacyModeEnabled) "••••" else CurrencyFormatter.formatAmount(
                                annualExpense,
                                currencyCode
                            )
                        Text(
                            "$assetsFormatted ÷ $expenseFormatted (Annual Expense). Your wealth expressed in time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Annual Expenses",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val dailyFormatted =
                            if (isPrivacyModeEnabled) "••••" else CurrencyFormatter.formatAmount(
                                annualExpense / 365,
                                currencyCode
                            )
                        Text(
                            "Estimated at $dailyFormatted/day. We use your rolling 12-month spending to normalize this figure.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "FIRE Goal (25x)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val fireTarget = annualExpense * 25
                        val targetFormatted =
                            if (isPrivacyModeEnabled) "••••" else CurrencyFormatter.formatAmount(
                                fireTarget,
                                currencyCode
                            )
                        Text(
                            "You are free when assets reach $targetFormatted. This allows for a safe 4% withdrawal rate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Manual Expense Override",
                                style = MaterialTheme.typography.labelLarge,
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
                                shape = MaterialTheme.shapes.large
                            )
                            Text(
                                "Override auto-tracking to account for inflation or missing data.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showHelp = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Got it", fontWeight = FontWeight.Bold)
                }
            },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "FINANCIAL FREEDOM",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Your wealth in time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { showHelp = true },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Help",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                com.sans.finance.presentation.components.CircularGauge(
                    progress = freedomScore,
                    size = 80.dp,
                    strokeWidth = 10.dp,
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Years of Cover",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isPrivacyModeEnabled) "••.• years" else String.format(
                            Locale.US,
                            "%.1f years",
                            yearsOfCover
                        ),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val statusText = when {
                        yearsOfCover >= 25.0 -> "You are Financially Free!"
                        yearsOfCover >= 10.0 -> "Decade of freedom secured."
                        yearsOfCover >= 1.0 -> "Over a year of cushion."
                        else -> "Building your foundation."
                    }

                    Text(
                        statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress towards FIRE Target (25x)
            val fireProgress = (yearsOfCover / 25.0).coerceIn(0.0, 1.0).toFloat()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "FIRE Progress (25x Expenses)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${(fireProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LinearProgressIndicator(
                    progress = { fireProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun RecentTransactionItem(
    transaction: com.sans.finance.domain.model.Expense,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isIncome = transaction.type == "INCOME"
            val statusColor =
                if (isIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        (if (isIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant).copy(
                            alpha = 0.1f
                        ),
                        MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    icon = transaction.categoryIcon ?: (if (isIncome) "💰" else "💸"),
                    fontSize = 14.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title.ifBlank { transaction.categoryName ?: "Transaction" },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                val subtitle = if (transaction.title.isNotBlank()) {
                    transaction.details ?: transaction.categoryName ?: ""
                } else {
                    transaction.details ?: ""
                }

                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                PrivacyText(
                    amount = transaction.amount,
                    currencyCode = transaction.currency,
                    isVisible = !isPrivacyModeEnabled,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Black,
                    color = statusColor
                )
                Text(
                    text = com.sans.finance.core.util.DateFormatterUtils.getStandardFormatter()
                        .format(java.util.Date(transaction.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
