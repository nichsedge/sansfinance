package com.sans.finance.presentation.add_transaction

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.model.Category
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.BudgetRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.InstallmentRepository
import com.sans.finance.domain.repository.TagRepository
import com.sans.finance.domain.usecase.AddTransactionUseCase
import com.sans.finance.domain.usecase.CheckDuplicateExpenseUseCase
import com.sans.finance.domain.usecase.CreateInstallmentPlanUseCase
import com.sans.finance.domain.usecase.DeleteExpenseUseCase
import com.sans.finance.domain.usecase.GetCategoriesUseCase
import com.sans.finance.domain.usecase.GetDetailsSuggestionsUseCase
import com.sans.finance.domain.usecase.GetExpenseByIdUseCase
import com.sans.finance.domain.usecase.GetFrequencyBasedSuggestionsUseCase
import com.sans.finance.domain.usecase.GetTitleSuggestionsUseCase
import com.sans.finance.domain.usecase.PredictTransactionUseCase
import com.sans.finance.domain.usecase.UpdateExpenseUseCase
import com.sans.finance.presentation.navigation.Screen
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var addTransactionUseCase: AddTransactionUseCase
    private lateinit var updateExpenseUseCase: UpdateExpenseUseCase
    private lateinit var deleteExpenseUseCase: DeleteExpenseUseCase
    private lateinit var getExpenseByIdUseCase: GetExpenseByIdUseCase
    private lateinit var getCategoriesUseCase: GetCategoriesUseCase
    private lateinit var createInstallmentPlanUseCase: CreateInstallmentPlanUseCase
    private lateinit var getTitleSuggestionsUseCase: GetTitleSuggestionsUseCase
    private lateinit var getDetailsSuggestionsUseCase: GetDetailsSuggestionsUseCase
    private lateinit var installmentRepository: InstallmentRepository
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var tagRepository: TagRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var currencyDao: CurrencyDao
    private lateinit var checkDuplicateExpenseUseCase: CheckDuplicateExpenseUseCase
    private lateinit var predictTransactionUseCase: PredictTransactionUseCase
    private lateinit var getFrequencyBasedSuggestionsUseCase: GetFrequencyBasedSuggestionsUseCase
    private lateinit var localeManager: LocaleManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        addTransactionUseCase = mockk(relaxed = true)
        updateExpenseUseCase = mockk(relaxed = true)
        deleteExpenseUseCase = mockk(relaxed = true)
        getExpenseByIdUseCase = mockk(relaxed = true)
        getCategoriesUseCase = mockk(relaxed = true)
        createInstallmentPlanUseCase = mockk(relaxed = true)
        getTitleSuggestionsUseCase = mockk(relaxed = true)
        getDetailsSuggestionsUseCase = mockk(relaxed = true)
        installmentRepository = mockk(relaxed = true)
        expenseRepository = mockk(relaxed = true)
        tagRepository = mockk(relaxed = true)
        accountRepository = mockk(relaxed = true)
        budgetRepository = mockk(relaxed = true)
        currencyDao = mockk(relaxed = true)
        checkDuplicateExpenseUseCase = mockk(relaxed = true)
        predictTransactionUseCase = mockk(relaxed = true)
        getFrequencyBasedSuggestionsUseCase = mockk(relaxed = true)
        localeManager = mockk(relaxed = true)

        every { localeManager.getCurrency() } returns "USD"
        every { localeManager.getEnabledCurrencies() } returns listOf("USD", "IDR")
        every { currencyDao.getAllRates() } returns flowOf(emptyList())
        every { tagRepository.getVisibleTags() } returns flowOf(emptyList())
        every { accountRepository.getAllAccounts() } returns flowOf(emptyList())
        every { budgetRepository.getAllBudgets() } returns flowOf(emptyList())
        every { expenseRepository.getAllExpenses() } returns flowOf(emptyList())

        val testCategories = listOf(
            Category(id = 1L, name = "Food", type = "EXPENSE", icon = "🍔"),
            Category(id = 2L, name = "Groceries", type = "EXPENSE", icon = "🛒"),
            Category(id = 3L, name = "Salary", type = "INCOME", icon = "💰")
        )
        every { getCategoriesUseCase.invoke() } returns flowOf(testCategories)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testEditTransactionLoadsAndPreservesAllFields() {
        val testExpense = Expense(
            id = 42L,
            date = 1725000000000L,
            title = "Whole Foods Market",
            amount = 5425L, // 54.25
            categoryId = 2L, // Groceries (not 1L Food)
            accountId = 5L,
            toAccountId = null,
            type = "EXPENSE",
            details = "Weekly grocery run",
            currency = "USD",
            tags = listOf("Groceries", "Organic"),
            status = "Paid"
        )
        coEvery { getExpenseByIdUseCase(42L) } returns testExpense

        mockkStatic("androidx.navigation.SavedStateHandleKt")
        val savedStateHandle = mockk<SavedStateHandle>(relaxed = true)
        every { savedStateHandle.toRoute<Screen.AddTransaction>() } throws IllegalArgumentException()
        every { savedStateHandle.toRoute<Screen.EditExpense>() } returns Screen.EditExpense(42L)

        val viewModel = AddTransactionViewModel(
            addTransactionUseCase = addTransactionUseCase,
            updateExpenseUseCase = updateExpenseUseCase,
            deleteExpenseUseCase = deleteExpenseUseCase,
            getExpenseByIdUseCase = getExpenseByIdUseCase,
            getCategoriesUseCase = getCategoriesUseCase,
            createInstallmentPlanUseCase = createInstallmentPlanUseCase,
            getTitleSuggestionsUseCase = getTitleSuggestionsUseCase,
            getDetailsSuggestionsUseCase = getDetailsSuggestionsUseCase,
            installmentRepository = installmentRepository,
            expenseRepository = expenseRepository,
            tagRepository = tagRepository,
            accountRepository = accountRepository,
            budgetRepository = budgetRepository,
            currencyDao = currencyDao,
            checkDuplicateExpenseUseCase = checkDuplicateExpenseUseCase,
            predictTransactionUseCase = predictTransactionUseCase,
            getFrequencyBasedSuggestionsUseCase = getFrequencyBasedSuggestionsUseCase,
            localeManager = localeManager,
            savedStateHandle = savedStateHandle
        )

        assertTrue(viewModel.isEditMode)
        assertEquals("54.25", viewModel.amount)
        assertEquals("Whole Foods Market", viewModel.title)
        assertEquals("Weekly grocery run", viewModel.details)
        assertEquals(2L, viewModel.categoryId)
        assertEquals(5L, viewModel.accountId)
        assertEquals("EXPENSE", viewModel.transactionType)
        assertEquals(1725000000000L, viewModel.selectedDate)
        assertEquals("USD", viewModel.currency)
        assertEquals(listOf("Groceries", "Organic"), viewModel.selectedTags)
        assertEquals("Paid", viewModel.status)
    }
}
