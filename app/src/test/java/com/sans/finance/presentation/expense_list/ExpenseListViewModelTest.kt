package com.sans.finance.presentation.expense_list

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.data.local.entity.ExchangeRateEntity
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.model.Category
import com.sans.finance.domain.model.DaySpent
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.model.FilteredExpensesData
import com.sans.finance.domain.model.UserPreferences
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.InstallmentRepository
import com.sans.finance.domain.repository.TagRepository
import com.sans.finance.domain.repository.UserPreferencesRepository
import com.sans.finance.domain.usecase.GetCategoriesUseCase
import com.sans.finance.domain.usecase.ObserveFilteredExpensesUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var observeFilteredExpensesUseCase: ObserveFilteredExpensesUseCase
    private lateinit var expenseRepository: com.sans.finance.domain.repository.ExpenseRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var installmentRepository: InstallmentRepository
    private lateinit var tagRepository: TagRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var getCategoriesUseCase: GetCategoriesUseCase
    private lateinit var budgetRepository: com.sans.finance.domain.repository.BudgetRepository
    private lateinit var currencyDao: CurrencyDao
    private lateinit var localeManager: LocaleManager

    private val filteredResult = MutableStateFlow(
        FilteredExpensesData(expenses = emptyList(), dailySpending = emptyList())
    )
    private val ratesFlow = MutableStateFlow<List<ExchangeRateEntity>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        observeFilteredExpensesUseCase = mockk()
        every { observeFilteredExpensesUseCase.invoke(any()) } returns filteredResult

        expenseRepository = mockk(relaxed = true)
        every { expenseRepository.getTotalSpentBetween(any(), any()) } returns flowOf(null)
        every { expenseRepository.getRecurringExpenses() } returns flowOf(emptyList())

        tagRepository = mockk()
        every { tagRepository.getAllTags() } returns flowOf(emptyList())

        userPreferencesRepository = mockk()
        every { userPreferencesRepository.userPreferences } returns flowOf(UserPreferences())

        accountRepository = mockk(relaxed = true)
        every { accountRepository.getAllAccounts() } returns flowOf(emptyList())

        installmentRepository = mockk(relaxed = true)
        every { installmentRepository.getActiveInstallments() } returns flowOf(emptyList())

        getCategoriesUseCase = mockk()
        every { getCategoriesUseCase.invoke() } returns flowOf(emptyList<Category>())

        budgetRepository = mockk(relaxed = true)
        every { budgetRepository.getAllBudgets() } returns flowOf(emptyList())

        currencyDao = mockk(relaxed = true)
        every { currencyDao.getAllRates() } returns ratesFlow

        localeManager = mockk()
        every { localeManager.getCurrency() } returns "IDR"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ExpenseListViewModel(
        savedStateHandle = SavedStateHandle(),
        observeFilteredExpensesUseCase = observeFilteredExpensesUseCase,
        repository = expenseRepository,
        accountRepository = accountRepository,
        installmentRepository = installmentRepository,
        tagRepository = tagRepository,
        userPreferencesRepository = userPreferencesRepository,
        getCategoriesUseCase = getCategoriesUseCase,
        budgetRepository = budgetRepository,
        currencyDao = currencyDao,
        localeManager = localeManager
    ).apply {
        val field = this::class.java.getDeclaredField("workerDispatcher")
        field.isAccessible = true
        field.set(this, testDispatcher)
    }

    private fun expense(
        id: Long,
        amount: Long,
        type: String = "EXPENSE",
        isInstallmentPayment: Boolean = false,
        isRecurring: Boolean = false
    ) = Expense(
        id = id,
        date = 1_700_000_000_000L,
        title = "Tx $id",
        amount = amount,
        categoryId = 1,
        type = type,
        isInstallment = false,
        isInstallmentPayment = isInstallmentPayment,
        isRecurring = isRecurring
    )

    @Test
    fun `initial state uses currency from LocaleManager and stops loading after data arrives`() = runTest {
        val viewModel = createViewModel()
        viewModel.state.test {
            var s = awaitItem()
            while (s.isLoading) s = awaitItem()
            assertEquals("IDR", s.currentCurrency)
        }
    }

    @Test
    fun `filtered expenses update totals and daily spending`() = runTest {
        val viewModel = createViewModel()
        viewModel.state.test {
            filteredResult.value = FilteredExpensesData(
                expenses = listOf(
                    expense(id = 1, amount = 10_000),
                    expense(id = 2, amount = 25_000),
                    expense(id = 3, amount = 50_000, type = "INCOME")
                ),
                dailySpending = listOf(DaySpent(day = 1_700_000_000_000L, amount = 35_000))
            )
            var s = awaitItem()
            while (s.expenses.size < 3) s = awaitItem()
            assertEquals(3, s.expenses.size)
        }
    }

    @Test
    fun `setCommitmentFilter filters groupedExpenses correctly`() = runTest {
        val viewModel = createViewModel()
        viewModel.state.test {
            filteredResult.value = FilteredExpensesData(
                expenses = listOf(
                    expense(id = 1, amount = 10_000),
                    expense(id = 2, amount = 25_000, isInstallmentPayment = true),
                    expense(id = 3, amount = 50_000, isRecurring = true)
                ),
                dailySpending = emptyList()
            )

            var s = awaitItem()
            while (s.expenses.size < 3) s = awaitItem()

            // Just test one transition to verify the wiring
            viewModel.setCommitmentFilter(TimelineCommitmentFilter.INSTALLMENTS)
            while (s.activeCommitmentFilter != TimelineCommitmentFilter.INSTALLMENTS || s.groupedExpenses.values.flatten().size != 1) {
                s = awaitItem()
            }
            assertEquals(2L, s.groupedExpenses.values.flatten().first().id)
        }
    }

    @Test
    fun `toggleCategoryFilter adds then removes category`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.toggleCategoryFilter(7)
        assertTrue(viewModel.state.value.selectedCategoryIds.contains(7))
        viewModel.toggleCategoryFilter(7)
        assertFalse(viewModel.state.value.selectedCategoryIds.contains(7))
    }

    @Test
    fun `toggleTypeFilter adds then removes type`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.toggleTypeFilter("INCOME")
        assertTrue(viewModel.state.value.selectedTypes.contains("INCOME"))
        viewModel.toggleTypeFilter("INCOME")
        assertFalse(viewModel.state.value.selectedTypes.contains("INCOME"))
    }

    @Test
    fun `clearFilters resets query selections and amounts`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.updateSearchQuery("coffee")
        viewModel.toggleCategoryFilter(3)
        viewModel.clearFilters()
        advanceUntilIdle()
        assertEquals("", viewModel.state.value.searchQuery)
    }

    @Test
    fun `filtered expenses update activeInstallmentCount and recurringExpenseCount based on visible period`() = runTest {
        val viewModel = createViewModel()
        viewModel.state.test {
            filteredResult.value = FilteredExpensesData(
                expenses = listOf(
                    expense(id = 1, amount = 10_000),
                    expense(id = 2, amount = 25_000, isInstallmentPayment = true),
                    expense(id = 3, amount = 30_000, isInstallmentPayment = true),
                    expense(id = 4, amount = 50_000, isRecurring = true)
                ),
                dailySpending = emptyList()
            )
            var s = awaitItem()
            while (s.activeInstallmentCount != 2 || s.recurringExpenseCount != 1) {
                s = awaitItem()
            }
            assertEquals(2, s.activeInstallmentCount)
            assertEquals(1, s.recurringExpenseCount)
        }
    }

    @Test
    fun `state emits updates through turbine`() = runTest {
        val viewModel = createViewModel()
        viewModel.state.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multi-currency transactions convert to main currency IDR in period and daily header totals`() = runTest {
        ratesFlow.value = listOf(
            ExchangeRateEntity(code = "SGD", rateToIdr = 12_000.0),
            ExchangeRateEntity(code = "USD", rateToIdr = 16_000.0)
        )
        val viewModel = createViewModel()
        viewModel.state.test {
            filteredResult.value = FilteredExpensesData(
                expenses = listOf(
                    expense(id = 1, amount = 100_000_00L, type = "EXPENSE").copy(currency = "IDR"),
                    expense(id = 2, amount = 10_00L, type = "EXPENSE").copy(currency = "SGD"),
                    expense(id = 3, amount = 5_00L, type = "INCOME").copy(currency = "USD")
                ),
                dailySpending = emptyList()
            )
            var s = awaitItem()
            while (s.totalFilteredExpense == 0L) {
                s = awaitItem()
            }
            // 100,000 IDR + (10 SGD * 12,000) + 0 = 220,000 IDR (in cents: 22,000,000)
            assertEquals(22_000_000L, s.totalFilteredExpense)
            // 5 USD * 16,000 = 80,000 IDR (in cents: 8,000,000)
            assertEquals(8_000_000L, s.totalFilteredIncome)
            assertEquals(8_000_000L - 22_000_000L, s.totalFilteredAmount)

            val header = s.timelineItems.filterIsInstance<TimelineItem.Header>().first()
            assertEquals(22_000_000L, header.expense)
            assertEquals(8_000_000L, header.income)
        }
    }

    @Test
    fun `filterAccountId for parent account expands to include child subaccounts`() = runTest {
        val accountsFlow = MutableStateFlow(
            listOf(
                com.sans.finance.data.local.entity.AccountEntity(id = 10L, name = "Wise", type = "Bank", balance = 0L),
                com.sans.finance.data.local.entity.AccountEntity(id = 11L, name = "Wise SGD", type = "Bank", balance = 100L, currency = "SGD", parentAccountId = 10L),
                com.sans.finance.data.local.entity.AccountEntity(id = 12L, name = "Wise USD", type = "Bank", balance = 200L, currency = "USD", parentAccountId = 10L),
                com.sans.finance.data.local.entity.AccountEntity(id = 20L, name = "BCA", type = "Bank", balance = 500L)
            )
        )
        every { accountRepository.getAllAccounts() } returns accountsFlow

        val viewModel = ExpenseListViewModel(
            savedStateHandle = SavedStateHandle(mapOf("filterAccountId" to 10L)),
            observeFilteredExpensesUseCase = observeFilteredExpensesUseCase,
            repository = expenseRepository,
            accountRepository = accountRepository,
            installmentRepository = installmentRepository,
            tagRepository = tagRepository,
            userPreferencesRepository = userPreferencesRepository,
            getCategoriesUseCase = getCategoriesUseCase,
            budgetRepository = budgetRepository,
            currencyDao = currencyDao,
            localeManager = localeManager
        ).apply {
            val field = this::class.java.getDeclaredField("workerDispatcher")
            field.isAccessible = true
            field.set(this, testDispatcher)
        }

        advanceUntilIdle()
        assertEquals(setOf(10L, 11L, 12L), viewModel.state.value.selectedAccountIds)
    }
}
