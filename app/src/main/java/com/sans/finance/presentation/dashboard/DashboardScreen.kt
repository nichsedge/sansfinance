package com.sans.finance.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sans.finance.presentation.components.AppTopBar

import androidx.compose.material.icons.filled.AutoAwesome

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onTransactionClick: (Long) -> Unit,
    onRecurringExpensesClick: () -> Unit,
    onInstallmentsClick: () -> Unit,
    onWealthForecastingClick: () -> Unit,
    onAiChatClick: (() -> Unit)? = null,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val haptic = LocalHapticFeedback.current

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppTopBar(
                title = "Dashboard",
                scrollBehavior = scrollBehavior,
                actions = {
                    onAiChatClick?.let { onChat ->
                        IconButton(onClick = onChat) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "SansAI",
                                tint = MaterialTheme.colorScheme.primary
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
        val surfaceColor = MaterialTheme.colorScheme.surface
        val primaryColor = MaterialTheme.colorScheme.primary
        val backgroundBrush = remember(surfaceColor, primaryColor) {
            Brush.verticalGradient(
                colors = listOf(
                    surfaceColor,
                    primaryColor.copy(alpha = 0.05f)
                )
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush),
            contentPadding = PaddingValues(
                start = paddingValues.calculateStartPadding(layoutDirection) + 16.dp,
                top = paddingValues.calculateTopPadding() + 8.dp,
                end = paddingValues.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Hero: Net Worth Overview
            item(key = "net_worth") {
                NetWorthCard(
                    netWorth = state.netWorth,
                    assets = state.totalAssets,
                    liabilities = state.totalLiabilities,
                    currencyCode = state.currentCurrency,
                    isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                    onTogglePrivacyMode = viewModel::togglePrivacyMode
                )
            }

            // 2. Operational Pulse: Monthly Cash Flow & Savings Rate
            item(key = "cash_flow") {
                MonthlyCashFlowCard(
                    income = state.monthlyIncome,
                    expense = state.monthlyExpense,
                    cashFlow = state.monthlyCashFlow,
                    savingsRate = state.monthlySavingsRate,
                    currencyCode = state.currentCurrency,
                    isPrivacyModeEnabled = state.isPrivacyModeEnabled
                )
            }

            // 3. Budget & Velocity
            if (state.globalBudget > 0L) {
                item(key = "global_budget") {
                    GlobalBudgetCard(
                        budget = state.globalBudget,
                        spent = state.globalSpent,
                        daysLeft = state.daysLeftInMonth,
                        spendingVelocity = state.spendingVelocity,
                        currencyCode = state.currentCurrency,
                        isPrivacyModeEnabled = state.isPrivacyModeEnabled
                    )
                }
            }

            // 4. Category Budgets
            if (state.categoryBudgets.isNotEmpty()) {
                item(key = "category_budgets") {
                    SectionHeader("CATEGORY BUDGETS")
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.categoryBudgets.forEach { budget ->
                            CategoryBudgetItem(
                                budget = budget,
                                currencyCode = state.currentCurrency,
                                isPrivacyModeEnabled = state.isPrivacyModeEnabled
                            )
                        }
                    }
                }
            }

            // 5. Actionable Obligations: Upcoming Bills & Installments
            if (state.upcomingBills.isNotEmpty()) {
                item(key = "upcoming_bills") {
                    UpcomingBillsCard(
                        bills = state.upcomingBills,
                        currencyCode = state.currentCurrency,
                        isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                        onRecurringExpensesClick = onRecurringExpensesClick,
                        onInstallmentsClick = onInstallmentsClick
                    )
                }
            }

            // 6. Forecasting Sparkline
            item(key = "forecast") {
                ForecastCard(
                    projectedBalance = state.projectedBalance30Days,
                    trendData = state.last30DaysTrend,
                    currencyCode = state.currentCurrency,
                    isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                    onClick = onWealthForecastingClick
                )
            }

            // 7. AI Advisor Insights
            if (state.aiSuggestions.isNotEmpty()) {
                item(key = "ai_advisor") {
                    AiAdvisorCard(suggestions = state.aiSuggestions)
                }
            }

            // 8. Goal Progress (if any)
            if (state.goals.isNotEmpty()) {
                item(key = "goals") {
                    SectionHeader("GOAL PROGRESS")
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

            // 9. Recent Activity / Transactions
            if (state.recentTransactions.isNotEmpty()) {
                item(key = "recent_transactions") {
                    RecentTransactionsCard(
                        transactions = state.recentTransactions,
                        currencyCode = state.currentCurrency,
                        isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                        onTransactionClick = onTransactionClick
                    )
                }
            }

            item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

