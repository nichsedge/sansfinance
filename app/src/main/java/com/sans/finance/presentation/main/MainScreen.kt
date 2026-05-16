package com.sans.finance.presentation.main

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sans.finance.presentation.accounts.AccountScreen
import com.sans.finance.presentation.dashboard.DashboardScreen
import com.sans.finance.presentation.expense_list.ExpenseListScreen
import com.sans.finance.presentation.navigation.Screen
import com.sans.finance.presentation.portfolio.PortfolioScreen
import com.sans.finance.presentation.settings.SettingsScreen
import com.sans.finance.presentation.wealth.WealthScreen

@Composable
fun MainScreen(
    rootNavController: NavHostController
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable<Screen.Dashboard> {
                DashboardScreen(
                    onTransactionClick = { id ->
                        rootNavController.navigate(Screen.EditExpense(id))
                    },
                    onRecurringExpensesClick = {
                        rootNavController.navigate(Screen.RecurringExpenses)
                    },
                    onInstallmentsClick = {
                        rootNavController.navigate(Screen.Installments)
                    },
                    onWealthForecastingClick = {
                        rootNavController.navigate(Screen.WealthForecasting)
                    }
                )
            }
            composable<Screen.ExpenseList> {
                ExpenseListScreen(
                    onAddTransactionClick = {
                        rootNavController.navigate(Screen.AddTransaction)
                    },
                    onInstallmentsClick = {
                        rootNavController.navigate(Screen.Installments)
                    },
                    onStatsClick = {
                        rootNavController.navigate(Screen.TransactionStats)
                    },
                    onRecurringExpensesClick = {
                        rootNavController.navigate(Screen.RecurringExpenses)
                    },
                    onSearchClick = {
                        rootNavController.navigate(Screen.Search)
                    },
                    onExpenseClick = { id ->
                        rootNavController.navigate(Screen.EditExpense(id))
                    }
                )
            }
            composable<Screen.Wealth> {
                WealthScreen(
                    onOpenAccounts = { navController.navigate(Screen.Accounts) },
                    onOpenPortfolio = { navController.navigate(Screen.Portfolio) },
                    onOpenDebts = { rootNavController.navigate(Screen.DebtStrategist) },
                    onOpenGoals = { rootNavController.navigate(Screen.Goals) },
                    onOpenBudgets = { rootNavController.navigate(Screen.Budgets) },
                    onOpenForecasting = { rootNavController.navigate(Screen.WealthForecasting) },
                    onOpenMonthlyReview = { rootNavController.navigate(Screen.MonthlyReview(0)) }
                )
            }
            composable<Screen.Accounts> {
                AccountScreen(
                    onStatsClick = {
                        rootNavController.navigate(Screen.AccountStats)
                    },
                    onDebtStrategistClick = {
                        rootNavController.navigate(Screen.DebtStrategist)
                    }
                )
            }
            composable<Screen.Portfolio> {
                PortfolioScreen(
                    onDashboardClick = {
                        navController.navigate(Screen.Dashboard) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onForecastingClick = {
                        rootNavController.navigate(Screen.WealthForecasting)
                    }
                )
            }
            composable<Screen.Settings> {
                SettingsScreen(
                    onNavigateToGoals = {
                        rootNavController.navigate(Screen.Goals)
                    },
                    onNavigateToBudgets = {
                        rootNavController.navigate(Screen.Budgets)
                    },
                    onNavigateToCategories = {
                        rootNavController.navigate(Screen.CategorySettings)
                    },
                    onNavigateToTags = {
                        rootNavController.navigate(Screen.TagSettings)
                    },
                    onNavigateToAccountTypes = {
                        rootNavController.navigate(Screen.AccountTypeSettings)
                    },
                    onNavigateToRecurringExpenses = {
                        rootNavController.navigate(Screen.RecurringExpenses)
                    },
                    onNavigateToDataManagement = {
                        rootNavController.navigate(Screen.DataManagement)
                    },
                    onNavigateToAiSettings = {
                        rootNavController.navigate(Screen.AiSettings)
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple(Screen.Dashboard, "Home", Icons.Default.Dashboard),
            Triple(Screen.ExpenseList, "Transactions", Icons.AutoMirrored.Filled.CompareArrows),
            Triple(Screen.Wealth, "Wealth", Icons.Default.AccountBalanceWallet),
            Triple(Screen.Settings, "Settings", Icons.Default.Settings)
        )

        items.forEach { (screen, label, icon) ->
            val isSelected =
                currentDestination?.hierarchy?.any { it.hasRoute(screen::class) } == true ||
                    (screen is Screen.Wealth && currentDestination?.hierarchy?.any { it.hasRoute(Screen.Accounts::class) } == true) ||
                    (screen is Screen.Wealth && currentDestination?.hierarchy?.any { it.hasRoute(Screen.Portfolio::class) } == true)

            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = {
                    Text(
                        label,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                },
                alwaysShowLabel = false,
                selected = isSelected,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.tertiary,
                    selectedTextColor = MaterialTheme.colorScheme.tertiary,
                    indicatorColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                onClick = {
                    navController.navigate(screen) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
