package com.sans.finance.presentation.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.presentation.components.PrivacyText
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onTransactionClick: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.togglePrivacyMode() }) {
                        Icon(
                            imageVector = if (state.isPrivacyModeEnabled) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (state.isPrivacyModeEnabled) "Show balances" else "Hide balances"
                        )
                    }
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
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
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
                MonthlyCashFlowCard(
                    income = state.monthlyIncome,
                    expense = state.monthlyExpense,
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
                    isPrivacyModeEnabled = state.isPrivacyModeEnabled
                )
            }

            if (state.wealthDistribution.isNotEmpty()) {
                item {
                    WealthDistributionCard(
                    distribution = state.wealthDistribution,
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
                    Text(
                        "GOAL PROGRESS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    state.goals.forEach { goal ->
                        DashboardGoalItem(goal, state.currentCurrency, state.isPrivacyModeEnabled)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            if (state.upcomingBills.isNotEmpty()) {
                item {
                    Text(
                        "UPCOMING BILLS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    state.upcomingBills.forEach { bill ->
                        DashboardBillItem(bill, state.currentCurrency, state.isPrivacyModeEnabled)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }


        }
    }
}

@Composable
fun NetWorthCard(
    netWorth: Long,
    assets: Long,
    liabilities: Long,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Total Net Worth",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            PrivacyText(
                amount = netWorth,
                currencyCode = currencyCode,
                isVisible = !isPrivacyModeEnabled,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BreakdownItem("Assets", assets, Color(0xFF4CAF50), currencyCode, isPrivacyModeEnabled)
                VerticalDivider(modifier = Modifier.height(40.dp))
                BreakdownItem("Liabilities", liabilities, Color(0xFFF44336), currencyCode, isPrivacyModeEnabled)
            }
        }
    }
}

@Composable
fun BreakdownItem(label: String, amount: Long, color: Color, currencyCode: String, isPrivacyModeEnabled: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
        )
        PrivacyText(
            amount = amount,
            currencyCode = currencyCode,
            isVisible = !isPrivacyModeEnabled,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun ForecastCard(projectedBalance: Long, trendData: List<Long>, currencyCode: String, isPrivacyModeEnabled: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "30-Day Forecast",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                PrivacyText(
                    amount = projectedBalance,
                    currencyCode = currencyCode,
                    isVisible = !isPrivacyModeEnabled,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            com.sans.finance.presentation.components.Sparkline(
                data = trendData,
                modifier = Modifier
                    .width(100.dp)
                    .height(40.dp),
                color = MaterialTheme.colorScheme.secondary,
                lineWidth = 3f
            )
        }
    }
}

@Composable
fun WealthDistributionCard(distribution: Map<String, Long>, currencyCode: String, isPrivacyModeEnabled: Boolean) {
    val total = distribution.values.sumOf { kotlin.math.abs(it) }.coerceAtLeast(1L)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Wealth Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Simple Bar Chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(MaterialTheme.shapes.small)
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
                    Box(modifier = Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .background(color))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                distribution.entries.forEachIndexed { index, entry ->
                    val color = when (index % 4) {
                        0 -> MaterialTheme.colorScheme.primary
                        1 -> MaterialTheme.colorScheme.secondary
                        2 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color, MaterialTheme.shapes.small)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            entry.key,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        PrivacyText(
                            amount = entry.value,
                            currencyCode = currencyCode,
                            isVisible = !isPrivacyModeEnabled,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyCashFlowCard(income: Long, expense: Long, savingsRate: Float, currencyCode: String, isPrivacyModeEnabled: Boolean) {
    val animatedSavings by animateFloatAsState(
        targetValue = savingsRate,
        animationSpec = tween(800),
        label = "savingsRate"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "This Month",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Income box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            Color(0xFF1B5E20).copy(alpha = 0.12f),
                            MaterialTheme.shapes.medium
                        )
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Income",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    PrivacyText(
                        amount = income,
                        currencyCode = currencyCode,
                        isVisible = !isPrivacyModeEnabled,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
                // Expense box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            Color(0xFFB71C1C).copy(alpha = 0.12f),
                            MaterialTheme.shapes.medium
                        )
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Expense",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF44336)
                        )
                    }
                    PrivacyText(
                        amount = expense,
                        currencyCode = currencyCode,
                        isVisible = !isPrivacyModeEnabled,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
            }
            if (income > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Monthly Savings Rate",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (savingsRate >= 0.2f) "Excellent progress!" 
                            else if (savingsRate >= 0.1f) "Good, keep it up."
                            else "Try to save more.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    com.sans.finance.presentation.components.CircularGauge(
                        progress = savingsRate,
                        size = 64.dp,
                        color = if (savingsRate >= 0.2f) Color(0xFF4CAF50) 
                                else if (savingsRate >= 0.1f) Color(0xFFFFC107) 
                                else Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

@Composable
fun AiAdvisorCard(suggestions: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "AI Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            suggestions.forEach { suggestion ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("•", color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        suggestion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardGoalItem(goal: com.sans.finance.data.local.entity.GoalEntity, currencyCode: String, isPrivacyModeEnabled: Boolean) {
    val progress = (goal.currentAmount.toFloat() / goal.targetAmount.toFloat()).coerceIn(0f, 1f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("•••%", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
fun DashboardBillItem(bill: com.sans.finance.domain.model.Expense, currencyCode: String, isPrivacyModeEnabled: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                alpha = 0.2f
            )
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    bill.note,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("Recurring Payment", style = MaterialTheme.typography.labelSmall)
            }
            PrivacyText(
                amount = bill.amount,
                currencyCode = bill.currency,
                isVisible = !isPrivacyModeEnabled,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun GlobalBudgetCard(budget: Long, spent: Long, daysLeft: Int, currencyCode: String, isPrivacyModeEnabled: Boolean) {
    val progress = (spent.toFloat() / budget.toFloat()).coerceIn(0f, 1f)
    val isOverBudget = spent > budget
    val remaining = (budget - spent).coerceAtLeast(0L)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isOverBudget)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isOverBudget)
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            )
        else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Global Budget",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (isOverBudget) "OVER BUDGET" else "ON TRACK",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "$daysLeft days left",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
                color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PrivacyText(
                        amount = spent,
                        currencyCode = currencyCode,
                        isVisible = !isPrivacyModeEnabled,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (isOverBudget) "Overspent" else "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PrivacyText(
                        amount = if (isOverBudget) spent - budget else remaining,
                        currencyCode = currencyCode,
                        isVisible = !isPrivacyModeEnabled,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
