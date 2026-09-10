package com.sans.finance.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
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
import com.sans.finance.presentation.navigation.NavigationTransitions
import com.sans.finance.presentation.navigation.Screen
import com.sans.finance.presentation.portfolio.PortfolioScreen
import com.sans.finance.presentation.settings.SettingsScreen
import com.sans.finance.presentation.wealth.WealthScreen

@Composable
fun MainScreen(
    rootNavController: NavHostController
) {
    val navController = rememberNavController()
    val configuration = LocalConfiguration.current
    val useNavRail = configuration.screenWidthDp >= 600

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showFab = currentDestination?.hierarchy?.any {
        it.hasRoute(Screen.Dashboard::class) ||
        it.hasRoute(Screen.ExpenseList::class) ||
        it.hasRoute(Screen.Wealth::class)
    } == true

    Scaffold(
        bottomBar = {
            if (!useNavRail) {
                BottomNavigationBar(navController)
            }
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = {
                        rootNavController.navigate(Screen.AddTransaction())
                    },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Transaction"
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (useNavRail) {
                SideNavigationRail(navController)
            }
            Box(modifier = Modifier.weight(1f)) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Dashboard,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = NavigationTransitions.tabEnterTransition,
                    exitTransition = NavigationTransitions.tabExitTransition,
                    popEnterTransition = NavigationTransitions.tabEnterTransition,
                    popExitTransition = NavigationTransitions.tabExitTransition
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
                                rootNavController.navigate(Screen.AddTransaction())
                            },
                            onStatsClick = {
                                rootNavController.navigate(Screen.TransactionStats)
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
                            onOpenRecurringExpenses = { rootNavController.navigate(Screen.RecurringExpenses) },
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
                            },
                            onBack = {
                                navController.popBackStack()
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
                            },
                            onBack = {
                                navController.popBackStack()
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
                            },
                            onNavigateToReSyncDryRun = {
                                rootNavController.navigate(Screen.ReSyncDryRun)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SideNavigationRail(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            Icon(
                imageVector = Icons.Default.Dashboard,
                contentDescription = null,
                modifier = Modifier.padding(vertical = 12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.fillMaxHeight()
    ) {
        val items = getNavItems()

        items.forEach { (screen, label, icon) ->
            val isSelected = isNavItemSelected(currentDestination, screen)

            NavigationRailItem(
                icon = { Icon(icon, contentDescription = label) },
                label = {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                alwaysShowLabel = true,
                selected = isSelected,
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.tertiary,
                    selectedTextColor = MaterialTheme.colorScheme.tertiary,
                    indicatorColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        val items = getNavItems()

        items.forEach { (screen, label, icon) ->
            val isSelected = isNavItemSelected(currentDestination, screen)

            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                },
                alwaysShowLabel = true,
                selected = isSelected,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.tertiary,
                    selectedTextColor = MaterialTheme.colorScheme.tertiary,
                    indicatorColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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

private fun getNavItems() = listOf(
    Triple(Screen.Dashboard, "Home", Icons.Default.Dashboard),
    Triple(Screen.ExpenseList, "Transactions", Icons.AutoMirrored.Filled.CompareArrows),
    Triple(Screen.Wealth, "Wealth", Icons.Default.AccountBalanceWallet),
    Triple(Screen.Settings, "Settings", Icons.Default.Settings)
)

private fun isNavItemSelected(currentDestination: androidx.navigation.NavDestination?, screen: Screen): Boolean {
    return currentDestination?.hierarchy?.any { it.hasRoute(screen::class) } == true ||
            (screen is Screen.Wealth && currentDestination?.hierarchy?.any { it.hasRoute(Screen.Accounts::class) } == true) ||
            (screen is Screen.Wealth && currentDestination?.hierarchy?.any { it.hasRoute(Screen.Portfolio::class) } == true)
}
