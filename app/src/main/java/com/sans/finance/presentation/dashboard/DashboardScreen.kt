package com.sans.finance.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.presentation.expense_list.ExpenseItem

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
                title = { Text("Dashboard", fontWeight = FontWeight.Bold) }
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
                    liabilities = state.totalLiabilities
                )
            }

            item {
                MonthlyCashFlowCard(
                    income = state.monthlyIncome,
                    expense = state.monthlyExpense,
                    savingsRate = state.monthlySavingsRate
                )
            }

            item {
                ForecastCard(projectedBalance = state.projectedBalance30Days)
            }

            if (state.wealthDistribution.isNotEmpty()) {
                item {
                    WealthDistributionCard(distribution = state.wealthDistribution)
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
                        DashboardGoalItem(goal)
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
                        DashboardBillItem(bill)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            item {
                Text(
                    "RECENT TRANS.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.5.sp
                )
            }

            items(state.recentTransactions) { transaction ->
                ExpenseItem(
                    expense = transaction,
                    category = null, // In a real app we'd fetch categories, but keeping it simple for now
                    onClick = { onTransactionClick(transaction.id) },
                    onLongClick = {}
                )
            }
        }
    }
}

@Composable
fun NetWorthCard(
    netWorth: Long,
    assets: Long,
    liabilities: Long
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
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
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
            Text(
                CurrencyFormatter.formatAmount(netWorth),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BreakdownItem("Assets", assets, Color(0xFF4CAF50))
                VerticalDivider(modifier = Modifier.height(40.dp))
                BreakdownItem("Liabilities", liabilities, Color(0xFFF44336))
            }
        }
    }
}

@Composable
fun BreakdownItem(label: String, amount: Long, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
        )
        Text(
            CurrencyFormatter.formatAmount(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
@Composable
fun ForecastCard(projectedBalance: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "30-Day Forecast",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    CurrencyFormatter.formatAmount(projectedBalance),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun WealthDistributionCard(distribution: Map<String, Long>) {
    val total = distribution.values.sumOf { kotlin.math.abs(it) }.coerceAtLeast(1L)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
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
                modifier = Modifier.fillMaxWidth().height(16.dp).clip(MaterialTheme.shapes.small)
            ) {
                distribution.entries.forEachIndexed { index, entry ->
                    val weight = (kotlin.math.abs(entry.value).toFloat() / total.toFloat()).coerceAtLeast(0.01f)
                    val color = when(index % 4) {
                        0 -> MaterialTheme.colorScheme.primary
                        1 -> MaterialTheme.colorScheme.secondary
                        2 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                    Box(modifier = Modifier.weight(weight).fillMaxHeight().background(color))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                distribution.entries.forEachIndexed { index, entry ->
                    val color = when(index % 4) {
                        0 -> MaterialTheme.colorScheme.primary
                        1 -> MaterialTheme.colorScheme.secondary
                        2 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).background(color, MaterialTheme.shapes.small))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(entry.key, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text(
                            CurrencyFormatter.formatAmount(entry.value),
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
fun MonthlyCashFlowCard(income: Long, expense: Long, savingsRate: Float) {
    val animatedSavings by animateFloatAsState(
        targetValue = savingsRate,
        animationSpec = tween(800),
        label = "savingsRate"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "This Month",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Income box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF1B5E20).copy(alpha = 0.12f), MaterialTheme.shapes.medium)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Income", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                    }
                    Text(
                        CurrencyFormatter.formatAmount(income),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
                // Expense box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFB71C1C).copy(alpha = 0.12f), MaterialTheme.shapes.medium)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFFF44336), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Expense", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF44336))
                    }
                    Text(
                        CurrencyFormatter.formatAmount(expense),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
            }
            if (income > 0) {
                Text("Savings Rate: ${(savingsRate * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(
                    progress = { animatedSavings },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (savingsRate >= 0.2f) Color(0xFF4CAF50) else if (savingsRate >= 0.1f) Color(0xFFFFC107) else Color(0xFFF44336),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
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
fun DashboardGoalItem(goal: com.sans.finance.data.local.entity.GoalEntity) {
    val progress = (goal.currentAmount.toFloat() / goal.targetAmount.toFloat()).coerceIn(0f, 1f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(goal.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
fun DashboardBillItem(bill: com.sans.finance.domain.model.Expense) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(bill.itemName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("Recurring Payment", style = MaterialTheme.typography.labelSmall)
            }
            Text(
                CurrencyFormatter.formatAmount(bill.amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
