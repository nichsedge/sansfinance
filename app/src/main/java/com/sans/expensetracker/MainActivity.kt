package com.sans.expensetracker

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
import com.sans.expensetracker.presentation.add_expense.AddExpenseScreen
import com.sans.expensetracker.presentation.expense_list.ExpenseListScreen
import com.sans.expensetracker.presentation.navigation.Screen
import com.sans.expensetracker.presentation.main.MainScreen

import com.sans.expensetracker.presentation.settings.SettingsScreen
import com.sans.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @javax.inject.Inject
    lateinit var localeManager: com.sans.expensetracker.data.util.LocaleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localeManager.updateResources(localeManager.getLocale())
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
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
            com.sans.expensetracker.presentation.installments.InstallmentsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<Screen.Stats> {
            com.sans.expensetracker.presentation.stats.StatsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<Screen.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLanguageToggle = onLanguageToggle,
                onNavigateToBudgets = { navController.navigate(Screen.Budgets) }
            )
        }
        composable<Screen.Accounts> {
            com.sans.expensetracker.presentation.accounts.AccountScreen()
        }
        composable<Screen.Budgets> {
            com.sans.expensetracker.presentation.budgeting.BudgetScreen()
        }
    }
}
