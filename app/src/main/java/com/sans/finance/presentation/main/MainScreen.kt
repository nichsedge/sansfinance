package com.sans.finance.presentation.main

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sans.finance.presentation.dashboard.DashboardScreen
import com.sans.finance.presentation.expense_list.ExpenseListScreen
import com.sans.finance.presentation.accounts.AccountScreen
import com.sans.finance.presentation.goals.GoalScreen
import com.sans.finance.presentation.settings.SettingsScreen
import com.sans.finance.presentation.navigation.Screen
import androidx.compose.material.icons.filled.Flag

@Composable
fun MainScreen(
    rootNavController: NavHostController,
    onLanguageToggle: () -> Unit
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
            startDestination = Screen.ExpenseList,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable<Screen.Dashboard> {
                DashboardScreen(
                    onTransactionClick = { id ->
                        rootNavController.navigate(Screen.EditExpense(id))
                    }
                )
            }
            composable<Screen.ExpenseList> {
                ExpenseListScreen(
                    onAddExpenseClick = {
                        rootNavController.navigate(Screen.AddExpense)
                    },
                    onInstallmentsClick = {
                        rootNavController.navigate(Screen.Installments)
                    },
                    onStatsClick = {
                        rootNavController.navigate(Screen.Stats)
                    },
                    onExpenseClick = { id ->
                        rootNavController.navigate(Screen.EditExpense(id))
                    }
                )
            }
            composable<Screen.Accounts> {
                AccountScreen(
                    onStatsClick = {
                        rootNavController.navigate(Screen.AccountStats)
                    }
                )
            }
            composable<Screen.Goals> {
                GoalScreen()
            }
            composable<Screen.Settings> {
                SettingsScreen(
                    onBack = { /* Handled by bottom nav */ },
                    onLanguageToggle = onLanguageToggle,
                    onNavigateToBudgets = {
                        rootNavController.navigate(Screen.Budgets)
                    },
                    onNavigateToCategories = {
                        rootNavController.navigate(Screen.CategorySettings)
                    },
                    onNavigateToTags = {
                        rootNavController.navigate(Screen.TagSettings)
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

    NavigationBar {
        val items = listOf(
            Triple(Screen.Dashboard, "Home", Icons.Default.Dashboard),
            Triple(Screen.ExpenseList, "Trans.", Icons.Default.History),
            Triple(Screen.Accounts, "Accounts", Icons.Default.AccountBalanceWallet),
            Triple(Screen.Goals, "Goals", Icons.Default.Flag),
            Triple(Screen.Settings, "Settings", Icons.Default.Settings)
        )

        items.forEach { (screen, label, icon) ->
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
                selected = currentDestination?.hierarchy?.any { it.hasRoute(screen::class) } == true,
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
