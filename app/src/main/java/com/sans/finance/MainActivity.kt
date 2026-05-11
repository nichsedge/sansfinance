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
import com.sans.finance.presentation.add_expense.AddExpenseScreen
import com.sans.finance.presentation.expense_list.ExpenseListScreen
import com.sans.finance.presentation.navigation.Screen
import com.sans.finance.presentation.main.MainScreen
import com.sans.finance.presentation.goals.GoalScreen

import com.sans.finance.presentation.settings.SettingsScreen
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
                AppNavigation(onLanguageToggle = {
                    val current = localeManager.getLocale()
                    val next = if (current.startsWith("en")) "id" else "en"
                    localeManager.setLocale(next)
                    // android.app.LocaleManager.applicationLocales already triggers
                    // a system-level activity recreation automatically — no need to
                    // call recreate() manually (doing so causes a double-recreation crash).
                })
            }
        }
    }
}

@Composable
fun AppNavigation(onLanguageToggle: () -> Unit) {
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
                rootNavController = navController,
                onLanguageToggle = onLanguageToggle
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
                onAddExpenseClick = {
                    navController.navigate(Screen.AddExpense)
                },

                onInstallmentsClick = {
                    navController.navigate(Screen.Installments)
                },
                onStatsClick = {
                    navController.navigate(Screen.Stats)
                },
                onExpenseClick = { id ->
                    navController.navigate(Screen.EditExpense(id))
                }
            )
        }
        composable<Screen.AddExpense> {
            AddExpenseScreen(onBack = { navController.popBackStack() })
        }

        composable<Screen.EditExpense> {
            AddExpenseScreen(onBack = { navController.popBackStack() })
        }
        composable<Screen.Installments> {
            com.sans.finance.presentation.installments.InstallmentsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<Screen.Stats> {
            com.sans.finance.presentation.stats.StatsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<Screen.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLanguageToggle = onLanguageToggle,
                onNavigateToGoals = { navController.navigate(Screen.Goals) },
                onNavigateToBudgets = { navController.navigate(Screen.Budgets) },
                onNavigateToCategories = { navController.navigate(Screen.CategorySettings) },
                onNavigateToTags = { navController.navigate(Screen.TagSettings) }
            )
        }
        composable<Screen.Accounts> {
            com.sans.finance.presentation.accounts.AccountScreen(
                onStatsClick = { navController.navigate(Screen.AccountStats) }
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
    }
}
