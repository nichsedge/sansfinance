package com.sans.finance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sans.finance.presentation.add_transaction.AddTransactionScreen
import com.sans.finance.presentation.expense_list.ExpenseListScreen
import com.sans.finance.presentation.goals.GoalScreen
import com.sans.finance.presentation.main.MainScreen
import com.sans.finance.presentation.navigation.Screen
import com.sans.finance.presentation.search.SearchScreen
import com.sans.finance.presentation.settings.SettingsScreen
import com.sans.finance.presentation.settings.data.DataManagementScreen
import com.sans.finance.ui.theme.SansFinanceTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @javax.inject.Inject
    lateinit var localeManager: com.sans.finance.data.util.LocaleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localeManager.updateResources(localeManager.getLocale())
        enableEdgeToEdge()
        setContent {
            SansFinanceTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Main,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable<Screen.Main> {
            MainScreen(
                rootNavController = navController
            )
        }
        composable<Screen.CategorySettings> {
            com.sans.finance.presentation.settings.categories.CategorySettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<Screen.TagSettings> {
            com.sans.finance.presentation.settings.tags.TagSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<Screen.ExpenseList> {
            ExpenseListScreen(
                onAddTransactionClick = {
                    navController.navigate(Screen.AddTransaction)
                },

                onInstallmentsClick = {
                    navController.navigate(Screen.Installments)
                },
                onStatsClick = {
                    navController.navigate(Screen.TransactionStats)
                },
                onRecurringExpensesClick = {
                    navController.navigate(Screen.RecurringExpenses)
                },
                onSearchClick = {
                    navController.navigate(Screen.Search)
                },
                onExpenseClick = { id ->
                    navController.navigate(Screen.EditExpense(id))
                }
            )
        }
        composable<Screen.AddTransaction> {
            AddTransactionScreen(onBack = { navController.popBackStack() })
        }

        composable<Screen.Search> {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onExpenseClick = { id ->
                    navController.navigate(Screen.EditExpense(id))
                }
            )
        }

        composable<Screen.EditExpense> {
            AddTransactionScreen(onBack = { navController.popBackStack() })
        }
        composable<Screen.Installments> {
            com.sans.finance.presentation.installments.InstallmentsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<Screen.TransactionStats> {
            com.sans.finance.presentation.transaction_stats.TransactionStatsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<Screen.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToGoals = { navController.navigate(Screen.Goals) },
                onNavigateToBudgets = { navController.navigate(Screen.Budgets) },
                onNavigateToCategories = { navController.navigate(Screen.CategorySettings) },
                onNavigateToTags = { navController.navigate(Screen.TagSettings) },
                onNavigateToRecurringExpenses = { navController.navigate(Screen.RecurringExpenses) },
                onNavigateToDataManagement = { navController.navigate(Screen.DataManagement) }
            )
        }
        composable<Screen.RecurringExpenses> {
            com.sans.finance.presentation.recurring.RecurringExpensesScreen(
                onNavigateBack = { navController.popBackStack() },
                onExpenseClick = { id -> navController.navigate(Screen.EditExpense(id)) }
            )
        }
        composable<Screen.Accounts> {
            com.sans.finance.presentation.accounts.AccountScreen(
                onStatsClick = { navController.navigate(Screen.AccountStats) }
            )
        }
        composable<Screen.Portfolio> {
            com.sans.finance.presentation.portfolio.PortfolioScreen(
                onDashboardClick = { navController.popBackStack() },
                onForecastingClick = { navController.navigate(Screen.WealthForecasting) }
            )
        }
        composable<Screen.AccountStats> {
            com.sans.finance.presentation.accounts.AccountStatsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<Screen.Budgets> {
            com.sans.finance.presentation.budgeting.BudgetScreen(onBack = { navController.popBackStack() })
        }
        composable<Screen.Goals> {
            GoalScreen(onBack = { navController.popBackStack() })
        }
        composable<Screen.WealthForecasting> {
            com.sans.finance.presentation.forecasting.WealthForecastingScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<Screen.DebtStrategist> {
            com.sans.finance.presentation.debt.DebtStrategistScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<Screen.DataManagement> {
            DataManagementScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
