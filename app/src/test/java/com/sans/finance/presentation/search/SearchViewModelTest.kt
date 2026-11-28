package com.sans.finance.presentation.search

import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.data.local.entity.ExchangeRateEntity
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.model.Category
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.model.Installment
import com.sans.finance.domain.model.InstallmentItem
import com.sans.finance.domain.model.UserPreferences
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.InstallmentRepository
import com.sans.finance.domain.repository.TagRepository
import com.sans.finance.domain.repository.UserPreferencesRepository
import com.sans.finance.domain.usecase.GetCategoriesUseCase
import com.sans.finance.presentation.expense_list.DateRangeFilter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var installmentRepository: InstallmentRepository
    private lateinit var tagRepository: TagRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var getCategoriesUseCase: GetCategoriesUseCase
    private lateinit var currencyDao: CurrencyDao
    private lateinit var localeManager: LocaleManager

    private val filteredExpensesFlow = MutableStateFlow<List<Expense>>(emptyList())
    private val ratesFlow = MutableStateFlow<List<ExchangeRateEntity>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        expenseRepository = mockk(relaxed = true)
        every {
            expenseRepository.getFilteredExpenses(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        } returns filteredExpensesFlow

        tagRepository = mockk()
        every { tagRepository.getAllTags() } returns flowOf(listOf("Food", "Tech"))

        userPreferencesRepository = mockk()
        every { userPreferencesRepository.userPreferences } returns flowOf(UserPreferences())

        accountRepository = mockk(relaxed = true)
        every { accountRepository.getAllAccounts() } returns flowOf(emptyList())

        installmentRepository = mockk(relaxed = true)

        getCategoriesUseCase = mockk()
        every { getCategoriesUseCase.invoke() } returns flowOf(
            listOf(Category(id = 1, name = "Food", icon = "🍔", type = "EXPENSE", orderIndex = 0))
        )

        currencyDao = mockk(relaxed = true)
        every { currencyDao.getAllRates() } returns ratesFlow

        localeManager = mockk()
        every { localeManager.getCurrency() } returns "IDR"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SearchViewModel(
        repository = expenseRepository,
        accountRepository = accountRepository,
        installmentRepository = installmentRepository,
        tagRepository = tagRepository,
        userPreferencesRepository = userPreferencesRepository,
        getCategoriesUseCase = getCategoriesUseCase,
        currencyDao = currencyDao,
        localeManager = localeManager
    )

    private fun sampleExpense(
        id: Long,
        amount: Long,
        title: String = "Test $id",
        type: String = "EXPENSE"
    ) = Expense(
        id = id,
        date = 1_700_000_000_000L,
        title = title,
        amount = amount,
        categoryId = 1,
        type = type
    )

    @Test
    fun `initial state loads currency, categories, tags and all-time date range`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("IDR", state.currentCurrency)
        assertEquals(1, state.categories.size)
        assertEquals(listOf("Food", "Tech"), state.availableTags)
        assertEquals(DateRangeFilter.ALL_TIME, state.activeDateFilter)
        assertEquals(0L, state.startDate)
        assertEquals(Long.MAX_VALUE, state.endDate)
    }

    @Test
    fun `search query update properly updates state and triggers search`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateSearchQuery("coffee")
        assertEquals("coffee", viewModel.state.value.searchQuery)

        filteredExpensesFlow.value = listOf(
            sampleExpense(id = 1, amount = 30_000, title = "Coffee Latte"),
            sampleExpense(id = 2, amount = 50_000, title = "Coffee Beans")
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.expenses.size)
        assertEquals(80_000L, state.totalExpense)
        assertEquals(0L, state.totalIncome)
        assertEquals(-80_000L, state.netAmount)
        assertFalse(state.isLoading)
    }

    @Test
    fun `filter toggles update filter state`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleCategoryFilter(1L)
        assertTrue(viewModel.state.value.selectedCategoryIds.contains(1L))
        viewModel.toggleCategoryFilter(1L)
        assertFalse(viewModel.state.value.selectedCategoryIds.contains(1L))

        viewModel.toggleAccountFilter(5L)
        assertTrue(viewModel.state.value.selectedAccountIds.contains(5L))

        viewModel.toggleTagFilter("Tech")
        assertTrue(viewModel.state.value.selectedTags.contains("Tech"))

        viewModel.toggleTypeFilter("INCOME")
        assertTrue(viewModel.state.value.selectedTypes.contains("INCOME"))

        viewModel.updateAmountFilter(10_000L, 50_000L)
        assertEquals(10_000L, viewModel.state.value.minAmount)
        assertEquals(50_000L, viewModel.state.value.maxAmount)
    }

    @Test
    fun `clearFilters resets all search and filter fields`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateSearchQuery("test")
        viewModel.toggleCategoryFilter(1L)
        viewModel.toggleAccountFilter(2L)
        viewModel.toggleTagFilter("Food")
        viewModel.toggleTypeFilter("EXPENSE")
        viewModel.updateAmountFilter(100L, 500L)
        viewModel.updateDateRange(DateRangeFilter.THIS_MONTH)

        viewModel.clearFilters()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("", state.searchQuery)
        assertTrue(state.selectedCategoryIds.isEmpty())
        assertTrue(state.selectedAccountIds.isEmpty())
        assertTrue(state.selectedTags.isEmpty())
        assertTrue(state.selectedTypes.isEmpty())
        assertEquals(null, state.minAmount)
        assertEquals(null, state.maxAmount)
        assertEquals(DateRangeFilter.ALL_TIME, state.activeDateFilter)
        assertEquals(0L, state.startDate)
        assertEquals(Long.MAX_VALUE, state.endDate)
    }

    @Test
    fun `openInstallmentDetail finds installment and sets state`() = runTest {
        val installment = Installment(
            id = 10L,
            expenseId = 1L,
            totalAmount = 1_200_000L,
            expenseName = "Phone",
            durationMonths = 12,
            monthlyPayment = 100_000L,
            remainingBalance = 1_200_000L,
            nextDueDate = 1_700_000_000_000L,
            status = "Active"
        )
        val items = listOf(
            InstallmentItem(
                id = 101L,
                installmentId = 10L,
                monthNumber = 1,
                amount = 100_000L,
                dueDate = 1_700_000_000_000L,
                status = "Pending"
            )
        )
        every { installmentRepository.getAllInstallments() } returns flowOf(listOf(installment))
        every { installmentRepository.getInstallmentItems(10L) } returns flowOf(items)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val exp = sampleExpense(id = 1L, amount = 100_000L, title = "Phone")
        viewModel.openInstallmentDetail(exp)
        advanceUntilIdle()

        assertEquals(installment, viewModel.state.value.selectedInstallment)
        assertEquals(items, viewModel.state.value.selectedInstallmentItems)

        viewModel.closeInstallmentDetail()
        assertNull(viewModel.state.value.selectedInstallment)
        assertTrue(viewModel.state.value.selectedInstallmentItems.isEmpty())
    }

    @Test
    fun `openRecurringDetail and closeRecurringDetail update state`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val exp = sampleExpense(id = 5L, amount = 50_000L, title = "Netflix")
        viewModel.openRecurringDetail(exp)
        assertEquals(exp, viewModel.state.value.selectedRecurringExpense)

        viewModel.closeRecurringDetail()
        assertNull(viewModel.state.value.selectedRecurringExpense)
    }

    @Test
    fun `toggleInstallmentItemStatus toggles between Paid and Pending`() = runTest {
        val installment = Installment(
            id = 10L,
            expenseId = 1L,
            totalAmount = 1_200_000L,
            expenseName = "Phone",
            durationMonths = 12,
            monthlyPayment = 100_000L,
            remainingBalance = 1_200_000L,
            nextDueDate = 1_700_000_000_000L,
            status = "Active"
        )
        val itemsFlow = MutableStateFlow(
            listOf(
                InstallmentItem(
                    id = 101L,
                    installmentId = 10L,
                    monthNumber = 1,
                    amount = 100_000L,
                    dueDate = 1_700_000_000_000L,
                    status = "Pending"
                )
            )
        )
        every { installmentRepository.getAllInstallments() } returns flowOf(listOf(installment))
        every { installmentRepository.getInstallmentItems(10L) } returns itemsFlow

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openInstallmentDetail(sampleExpense(id = 1L, amount = 100_000L, title = "Phone"))
        advanceUntilIdle()

        viewModel.toggleInstallmentItemStatus(101L, "Pending")
        advanceUntilIdle()

        coVerify { installmentRepository.updateInstallmentItemStatus(101L, "Paid") }
    }

    @Test
    fun `deleteInstallmentPlan deletes underlying expense and closes detail`() = runTest {
        val installment = Installment(
            id = 10L,
            expenseId = 1L,
            totalAmount = 1_200_000L,
            expenseName = "Phone",
            durationMonths = 12,
            monthlyPayment = 100_000L,
            remainingBalance = 1_200_000L,
            nextDueDate = 1_700_000_000_000L,
            status = "Active"
        )
        val exp = sampleExpense(id = 1L, amount = 1_200_000L, title = "Phone")
        coEvery { expenseRepository.getExpenseById(1L) } returns exp
        every { installmentRepository.getAllInstallments() } returns flowOf(listOf(installment))
        every { installmentRepository.getInstallmentItems(10L) } returns flowOf(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openInstallmentDetail(exp)
        advanceUntilIdle()

        viewModel.deleteInstallmentPlan(installment)
        advanceUntilIdle()

        coVerify { expenseRepository.deleteExpense(exp) }
        assertNull(viewModel.state.value.selectedInstallment)
    }

    @Test
    fun `deleteRecurringExpense deletes expense and closes detail`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val exp = sampleExpense(id = 5L, amount = 50_000L, title = "Netflix")
        viewModel.openRecurringDetail(exp)
        viewModel.deleteRecurringExpense(exp)
        advanceUntilIdle()

        coVerify { expenseRepository.deleteExpense(exp) }
        assertNull(viewModel.state.value.selectedRecurringExpense)
    }

    @Test
    fun `search totals convert SGD and USD transactions to main currency IDR`() = runTest {
        ratesFlow.value = listOf(
            ExchangeRateEntity(code = "SGD", rateToIdr = 12_000.0),
            ExchangeRateEntity(code = "USD", rateToIdr = 16_000.0)
        )
        filteredExpensesFlow.value = listOf(
            sampleExpense(id = 1L, amount = 100_00L, type = "EXPENSE").copy(currency = "IDR"),
            sampleExpense(id = 2L, amount = 10_00L, type = "EXPENSE").copy(currency = "SGD"),
            sampleExpense(id = 3L, amount = 5_00L, type = "INCOME").copy(currency = "USD")
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(12_010_000L, state.totalExpense)
        assertEquals(8_000_000L, state.totalIncome)
        assertEquals(8_000_000L - 12_010_000L, state.netAmount)
    }
}
