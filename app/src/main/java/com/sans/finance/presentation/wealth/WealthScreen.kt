package com.sans.finance.presentation.wealth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sans.finance.presentation.components.GlassCard
import com.sans.finance.presentation.components.PrivacyText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WealthScreen(
    contentPadding: PaddingValues = PaddingValues(12.dp),
    onOpenAccounts: () -> Unit,
    onOpenPortfolio: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenForecasting: () -> Unit,
    onOpenMonthlyReview: () -> Unit,
    viewModel: WealthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Wealth",
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
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                WealthSummaryCard(
                    netWorth = state.cashAssets + state.portfolioValue - state.liabilities,
                    assets = state.cashAssets + state.portfolioValue,
                    liabilities = state.liabilities,
                    currencyCode = state.currencyCode,
                    isPrivacyModeEnabled = state.isPrivacyModeEnabled
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item { SectionHeader("FINANCIAL HUB") }

            item {
                WealthNavCard(
                    title = "Accounts",
                    subtitle = "Manage cash, bank, & e-wallets",
                    leadingIcon = Icons.Default.AccountBalanceWallet,
                    onClick = onOpenAccounts
                )
            }
            item {
                WealthNavCard(
                    title = "Portfolio",
                    subtitle = "Investment holdings & performance",
                    leadingIcon = Icons.Default.PieChart,
                    onClick = onOpenPortfolio
                )
            }
            item {
                WealthNavCard(
                    title = "Debt Strategist",
                    subtitle = "Payoff planning & liabilities",
                    leadingIcon = Icons.Default.Payments,
                    onClick = onOpenDebts
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
            item { SectionHeader("STRATEGY & PLANNING") }

            item {
                WealthNavCard(
                    title = "Financial Goals",
                    subtitle = "Track progress & future targets",
                    leadingIcon = Icons.Default.Flag,
                    onClick = onOpenGoals
                )
            }
            item {
                WealthNavCard(
                    title = "Budgeting",
                    subtitle = "Plan spending & monitor limits",
                    leadingIcon = Icons.AutoMirrored.Filled.ShowChart,
                    onClick = onOpenBudgets
                )
            }
            item {
                WealthNavCard(
                    title = "Net Worth Forecast",
                    subtitle = "Future wealth projection",
                    leadingIcon = Icons.AutoMirrored.Filled.ShowChart,
                    onClick = onOpenForecasting
                )
            }
            item {
                WealthNavCard(
                    title = "Monthly Review",
                    subtitle = "Monthly insights & closure",
                    leadingIcon = Icons.Default.Flag,
                    onClick = onOpenMonthlyReview
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
private fun WealthSummaryCard(
    netWorth: Long,
    assets: Long,
    liabilities: Long,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primary,
        alpha = 0.12f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Consolidated Net Worth",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            PrivacyText(
                amount = netWorth,
                currencyCode = currencyCode,
                isVisible = !isPrivacyModeEnabled,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WealthBreakdownItem(
                    "Total Assets",
                    assets,
                    MaterialTheme.colorScheme.tertiary,
                    currencyCode,
                    isPrivacyModeEnabled
                )
                VerticalDivider(
                    modifier = Modifier.height(32.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                WealthBreakdownItem(
                    "Total Liabilities",
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
private fun WealthBreakdownItem(
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
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

@Composable
private fun WealthNavCard(
    title: String,
    subtitle: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
