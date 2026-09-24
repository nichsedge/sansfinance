package com.sans.finance

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sans.finance.presentation.add_transaction.AddTransactionScreen
import com.sans.finance.presentation.expense_list.ExpenseListScreen
import com.sans.finance.presentation.goals.GoalScreen
import com.sans.finance.presentation.main.MainScreen
import com.sans.finance.presentation.navigation.NavigationTransitions
import com.sans.finance.presentation.navigation.Screen
import com.sans.finance.presentation.search.SearchScreen
import com.sans.finance.presentation.settings.SettingsScreen
import com.sans.finance.presentation.settings.data.DataManagementScreen
import com.sans.finance.ui.theme.SansFinanceTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @javax.inject.Inject
    lateinit var localeManager: com.sans.finance.data.util.LocaleManager

    private val navEventChannel = Channel<Screen>(Channel.BUFFERED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localeManager.updateResources(localeManager.getLocale())
        if (savedInstanceState == null) {
            handleIntent(intent)
        }
        enableEdgeToEdge()
        setContent {
            SansFinanceTheme {
                AppNavigation(navEventFlow = navEventChannel.receiveAsFlow())
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val categoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, -1L)
        val transactionType = intent.getStringExtra(EXTRA_TRANSACTION_TYPE) ?: "EXPENSE"
        val editExpenseId = intent.getLongExtra(EXTRA_EXPENSE_ID, -1L)

        when (intent.action) {
            ACTION_ADD_TRANSACTION -> {
                navEventChannel.trySend(Screen.AddTransaction(categoryId, transactionType))
            }
            ACTION_VIEW_TRANSACTIONS -> {
                navEventChannel.trySend(Screen.ExpenseList)
            }
            ACTION_VIEW_BUDGETS -> {
                navEventChannel.trySend(Screen.Budgets)
            }
            ACTION_VIEW_WEALTH -> {
                navEventChannel.trySend(Screen.Portfolio)
            }
            ACTION_SYNC_PORTFOLIO -> {
                val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.sans.finance.data.worker.CloudSyncAndBackupWorker>().build()
                androidx.work.WorkManager.getInstance(this).enqueue(workRequest)
                navEventChannel.trySend(Screen.Portfolio)
            }
            Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrBlank()) {
                    navEventChannel.trySend(
                        Screen.AddTransaction(
                            initialTitle = sharedText.take(60),
                            initialNotes = sharedText
                        )
                    )
                } else {
                    val streamUri = intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
                    navEventChannel.trySend(
                        Screen.AddTransaction(
                            initialTitle = "Receipt",
                            initialNotes = streamUri?.toString() ?: ""
                        )
                    )
                }
            }
        }
        if (editExpenseId > 0) {
            navEventChannel.trySend(Screen.EditExpense(editExpenseId))
        } else if (intent.getBooleanExtra(EXTRA_NAVIGATE_TO_ADD_TRANSACTION, false)) {
            navEventChannel.trySend(Screen.AddTransaction(categoryId, transactionType))
        }
    }

    companion object {
        const val ACTION_ADD_TRANSACTION = "com.sans.finance.action.ADD_TRANSACTION"
        const val ACTION_VIEW_TRANSACTIONS = "com.sans.finance.action.VIEW_TRANSACTIONS"
        const val ACTION_VIEW_BUDGETS = "com.sans.finance.action.VIEW_BUDGETS"
        const val ACTION_VIEW_WEALTH = "com.sans.finance.action.VIEW_WEALTH"
        const val ACTION_SYNC_PORTFOLIO = "com.sans.finance.action.SYNC_PORTFOLIO"
        const val EXTRA_NAVIGATE_TO_ADD_TRANSACTION = "navigate_to_add_transaction"
        const val EXTRA_CATEGORY_ID = "extra_category_id"
        const val EXTRA_TRANSACTION_TYPE = "extra_transaction_type"
        const val EXTRA_EXPENSE_ID = "extra_expense_id"
    }
}

@Composable
fun AppNavigation(
    navEventFlow: Flow<Screen>? = null
) {
    val navController = rememberNavController()

    LaunchedEffect(navEventFlow) {
        navEventFlow?.collect { destination ->
            navController.navigate(destination)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Main,
        enterTransition = NavigationTransitions.defaultEnterTransition,
        exitTransition = NavigationTransitions.defaultExitTransition,
        popEnterTransition = NavigationTransitions.defaultPopEnterTransition,
        popExitTransition = NavigationTransitions.defaultPopExitTransition
    ) {
        composable<Screen.Main>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
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
        composable<Screen.AccountTypeSettings> {
            com.sans.finance.presentation.settings.accounts.AccountTypeSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<Screen.ExpenseList> {
            ExpenseListScreen(
                onAddTransactionClick = {
                    navController.navigate(Screen.AddTransaction())
                },
                onStatsClick = {
                    navController.navigate(Screen.TransactionStats)
                },
                onSearchClick = {
                    navController.navigate(Screen.Search)
                },
                onExpenseClick = { id ->
                    navController.navigate(Screen.EditExpense(id))
                }
            )
        }
        composable<Screen.AddTransaction>(
            enterTransition = NavigationTransitions.modalEnterTransition,
            popExitTransition = NavigationTransitions.modalPopExitTransition
        ) {
            AddTransactionScreen(
                onBack = { navController.popBackStack() },
                onAiChatClick = { navController.navigate(Screen.AiChat) }
            )
        }

        composable<Screen.Search> {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onExpenseClick = { id ->
                    navController.navigate(Screen.EditExpense(id))
                }
            )
        }

        composable<Screen.EditExpense>(
            enterTransition = NavigationTransitions.modalEnterTransition,
            popExitTransition = NavigationTransitions.modalPopExitTransition
        ) {
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
                onNavigateToAccountTypes = { navController.navigate(Screen.AccountTypeSettings) },
                onNavigateToRecurringExpenses = { navController.navigate(Screen.RecurringExpenses) },
                onNavigateToDataManagement = { navController.navigate(Screen.DataManagement) },
                onNavigateToAiSettings = { navController.navigate(Screen.AiSettings) },
                onNavigateToReSyncDryRun = { navController.navigate(Screen.ReSyncDryRun) }
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
                onStatsClick = { navController.navigate(Screen.AccountStats) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<Screen.Portfolio> {
            com.sans.finance.presentation.portfolio.PortfolioScreen(
                onDashboardClick = { navController.popBackStack() },
                onForecastingClick = { navController.navigate(Screen.WealthForecasting) },
                onBack = { navController.popBackStack() }
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
        composable<Screen.AiSettings> {
            com.sans.finance.presentation.settings.ai.AiSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<Screen.ReSyncDryRun> {
            com.sans.finance.presentation.settings.resync.ReSyncDryRunScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<Screen.MonthlyReview> {
            com.sans.finance.presentation.monthly_review.MonthlyReviewScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable<Screen.AiChat> {
            com.sans.finance.presentation.ai.AiChatScreen(
                onBack = { navController.popBackStack() },
                onNavigateToAiSettings = { navController.navigate(Screen.AiSettings) },
                onEditInForm = { proposal ->
                    val amountStr = if (proposal.amountInCents % 100 == 0L) {
                        (proposal.amountInCents / 100).toString()
                    } else {
                        String.format(java.util.Locale.US, "%.2f", proposal.amountInCents / 100.0).trimEnd('0').trimEnd('.')
                    }
                    navController.navigate(
                        Screen.AddTransaction(
                            categoryId = proposal.categoryId,
                            transactionType = proposal.type,
                            initialTitle = proposal.title,
                            initialNotes = proposal.notes,
                            initialAmount = amountStr,
                            initialAccountId = proposal.accountId
                        )
                    )
                }
            )
        }
    }
}
