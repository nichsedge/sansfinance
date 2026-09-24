package com.sans.finance.presentation.ai

import com.sans.finance.data.ai.AiProvider
import com.sans.finance.data.ai.AiProviderFactory
import com.sans.finance.data.ai.AiProviderType
import com.sans.finance.data.ai.AiSettings
import com.sans.finance.data.ai.AiSettingsRepository
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.domain.model.AccountSummary
import com.sans.finance.domain.model.AiAssistantResponse
import com.sans.finance.domain.model.AiTransactionProposal
import com.sans.finance.domain.model.Category
import com.sans.finance.domain.model.CategorySummary
import com.sans.finance.domain.model.ChatMessage
import com.sans.finance.domain.model.ChatSender
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.CategoryRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.usecase.AddTransactionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiChatViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var aiProviderFactory: AiProviderFactory
    private lateinit var aiSettingsRepository: AiSettingsRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var addTransactionUseCase: AddTransactionUseCase
    private lateinit var aiProvider: AiProvider

    private val accountsFlow = MutableStateFlow<List<AccountEntity>>(emptyList())
    private val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    private val settingsFlow = MutableStateFlow(
        AiSettings(
            provider = AiProviderType.OPENROUTER,
            openRouterApiKey = "sk-or-v1-test",
            openRouterModel = "openai/gpt-4.1-mini"
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        aiProviderFactory = mockk(relaxed = true)
        aiSettingsRepository = mockk(relaxed = true)
        accountRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        expenseRepository = mockk(relaxed = true)
        addTransactionUseCase = mockk(relaxed = true)
        aiProvider = mockk(relaxed = true)

        every { accountRepository.getAllAccounts() } returns accountsFlow
        every { categoryRepository.getAllCategories() } returns categoriesFlow
        every { aiSettingsRepository.settings } returns settingsFlow
        every { expenseRepository.getTotalAmountByTypeBetween(any(), any(), any()) } returns flowOf(0L)
        every { expenseRepository.getBreakdownByCategoryBetween(any(), any(), any()) } returns flowOf(emptyList())
        every { expenseRepository.getExpensesBetween(any(), any()) } returns flowOf(emptyList())

        every {
            aiProvider.streamChat(any(), any(), any(), any(), any(), any())
        } answers {
            val userMsg = firstArg<String>()
            val accs = secondArg<List<AccountSummary>>()
            val cats = thirdArg<List<CategorySummary>>()
            val curr = arg<String>(3)
            val hist = arg<List<ChatMessage>>(4)
            val snap = arg<com.sans.finance.domain.model.FinancialContextSnapshot?>(5)
            kotlinx.coroutines.flow.flow {
                val resp = aiProvider.parseReceiptOrChat(userMsg, accs, cats, curr, hist, snap)
                if (resp.proposals.isNotEmpty()) {
                    val jsonProposals = org.json.JSONArray().apply {
                        resp.proposals.forEach { p ->
                            put(org.json.JSONObject().apply {
                                put("id", p.id)
                                put("title", p.title)
                                put("amount", p.amountInCents / 100.0)
                                put("type", p.type)
                                put("date", p.date)
                                put("accountId", p.accountId)
                                put("accountName", p.accountName)
                                put("categoryId", p.categoryId)
                                put("categoryName", p.categoryName)
                                put("notes", p.notes)
                                put("tags", org.json.JSONArray(p.tags))
                            })
                        }
                    }
                    val fullPayload = org.json.JSONObject().apply {
                        put("reply", resp.reply)
                        put("proposals", jsonProposals)
                    }.toString()
                    emit(com.sans.finance.domain.model.StreamEvent.Done(fullPayload))
                } else {
                    emit(com.sans.finance.domain.model.StreamEvent.Done(resp.reply))
                }
            }
        }

        coEvery { aiProviderFactory.create() } returns aiProvider
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has welcome message and config`() = runTest {
        val viewModel = AiChatViewModel(
            aiProviderFactory = aiProviderFactory,
            aiSettingsRepository = aiSettingsRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            expenseRepository = expenseRepository,
            addTransactionUseCase = addTransactionUseCase
        )

        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals(ChatSender.ASSISTANT, state.messages.first().sender)
        assertTrue(state.isAiConfigured)
    }

    @Test
    fun `sendMessage with bulk receipt text parses multiple proposals from AI`() = runTest {
        accountsFlow.value = listOf(
            AccountEntity(id = 1L, name = "CIMB Niaga", type = "CHECKING", balance = 10_000_000L)
        )
        categoriesFlow.value = listOf(
            Category(id = 5L, name = "Investasi", icon = "ic_invest", type = "INCOME")
        )

        val proposal1 = AiTransactionProposal(
            id = "p-1",
            title = "Kupon SBN ORI024",
            amountInCents = 150_000_000L,
            type = "INCOME",
            accountId = 1L,
            accountName = "CIMB Niaga",
            categoryId = 5L,
            categoryName = "Investasi"
        )
        val proposal2 = AiTransactionProposal(
            id = "p-2",
            title = "Kupon SBN SR019",
            amountInCents = 75_000_000L,
            type = "INCOME",
            accountId = 1L,
            accountName = "CIMB Niaga",
            categoryId = 5L,
            categoryName = "Investasi"
        )

        coEvery {
            aiProvider.parseReceiptOrChat(any(), any(), any(), any(), any(), any())
        } returns AiAssistantResponse(
            reply = "Ditemukan 2 pembayaran kupon SBN.",
            proposals = listOf(proposal1, proposal2)
        )

        val viewModel = AiChatViewModel(
            aiProviderFactory = aiProviderFactory,
            aiSettingsRepository = aiSettingsRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            expenseRepository = expenseRepository,
            addTransactionUseCase = addTransactionUseCase
        )

        viewModel.onInputTextChanged("Kupon ORI024 1.5jt dan Kupon SR019 750rb di CIMB")
        viewModel.sendMessage()

        val state = viewModel.uiState.value
        assertEquals(3, state.messages.size)
        val lastMsg = state.messages.last()
        assertEquals(ChatSender.ASSISTANT, lastMsg.sender)
        assertEquals(2, lastMsg.proposals.size)
        assertEquals("Kupon SBN ORI024", lastMsg.proposals[0].title)
        assertEquals("Kupon SBN SR019", lastMsg.proposals[1].title)
    }

    @Test
    fun `confirmProposal calls addTransactionUseCase and marks confirmed`() = runTest {
        coEvery { addTransactionUseCase(any()) } returns 101L

        val proposal = AiTransactionProposal(
            id = "p-1",
            title = "Kupon SBN ORI026",
            amountInCents = 150_000_000L,
            type = "INCOME",
            accountId = 1L,
            accountName = "CIMB Niaga",
            categoryId = 5L,
            categoryName = "Investasi"
        )

        coEvery {
            aiProvider.parseReceiptOrChat(any(), any(), any(), any(), any(), any())
        } returns AiAssistantResponse(
            reply = "1 kupon terdeteksi",
            proposals = listOf(proposal)
        )

        val viewModel = AiChatViewModel(
            aiProviderFactory = aiProviderFactory,
            aiSettingsRepository = aiSettingsRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            expenseRepository = expenseRepository,
            addTransactionUseCase = addTransactionUseCase
        )

        viewModel.sendMessage("receipt")
        val assistantMsg = viewModel.uiState.value.messages.last()

        viewModel.confirmProposal(assistantMsg.id, proposal)

        coVerify {
            addTransactionUseCase(
                match { expense: Expense ->
                    expense.title == "Kupon SBN ORI026" &&
                        expense.amount == 150_000_000L &&
                        expense.type == "INCOME" &&
                        expense.accountId == 1L &&
                        expense.categoryId == 5L
                }
            )
        }

        val updatedMsg = viewModel.uiState.value.messages.first { it.id == assistantMsg.id }
        assertTrue(updatedMsg.proposals.first().isConfirmed)
        assertNotNull(viewModel.uiState.value.successSnackbarMessage)
    }

    @Test
    fun `confirmAllProposals saves all pending transactions in bulk`() = runTest {
        coEvery { addTransactionUseCase(any()) } returns 200L

        val proposal1 = AiTransactionProposal(
            id = "p-1",
            title = "Kupon ORI024",
            amountInCents = 100_000_000L,
            type = "INCOME",
            accountId = 1L,
            categoryId = 5L
        )
        val proposal2 = AiTransactionProposal(
            id = "p-2",
            title = "Kupon SR019",
            amountInCents = 50_000_000L,
            type = "INCOME",
            accountId = 1L,
            categoryId = 5L
        )

        coEvery {
            aiProvider.parseReceiptOrChat(any(), any(), any(), any(), any(), any())
        } returns AiAssistantResponse(
            reply = "2 kupon",
            proposals = listOf(proposal1, proposal2)
        )

        val viewModel = AiChatViewModel(
            aiProviderFactory = aiProviderFactory,
            aiSettingsRepository = aiSettingsRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            expenseRepository = expenseRepository,
            addTransactionUseCase = addTransactionUseCase
        )

        viewModel.sendMessage("bulk receipts")
        val msgId = viewModel.uiState.value.messages.last().id

        viewModel.confirmAllProposals(msgId)

        coVerify(exactly = 2) {
            addTransactionUseCase(any())
        }

        val updatedMsg = viewModel.uiState.value.messages.first { it.id == msgId }
        assertTrue(updatedMsg.allConfirmed)
        assertTrue(viewModel.uiState.value.successSnackbarMessage?.contains("2 transaksi") == true)
    }

    @Test
    fun `rejectProposal marks specific proposal rejected`() = runTest {
        val proposal = AiTransactionProposal(
            id = "p-reject",
            title = "Biaya Admin",
            amountInCents = 650_000L,
            type = "EXPENSE",
            accountId = 1L,
            categoryId = 2L
        )

        coEvery {
            aiProvider.parseReceiptOrChat(any(), any(), any(), any(), any(), any())
        } returns AiAssistantResponse(
            reply = "1 biaya",
            proposals = listOf(proposal)
        )

        val viewModel = AiChatViewModel(
            aiProviderFactory = aiProviderFactory,
            aiSettingsRepository = aiSettingsRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            expenseRepository = expenseRepository,
            addTransactionUseCase = addTransactionUseCase
        )

        viewModel.sendMessage("admin fee")
        val msgId = viewModel.uiState.value.messages.last().id

        viewModel.rejectProposal(msgId, "p-reject")

        val updatedMsg = viewModel.uiState.value.messages.first { it.id == msgId }
        assertTrue(updatedMsg.proposals.first().isRejected)
    }

    @Test
    fun `confirmProposal defaults to cash account and account currency when proposal account unrecognised`() = runTest {
        accountsFlow.value = listOf(
            AccountEntity(id = 28L, name = "114538727842", type = "Investment", balance = 0L, currency = "IDR"),
            AccountEntity(id = 7L, name = "Dompet Tunai", type = "Cash", balance = 500_000L, currency = "IDR")
        )

        coEvery { addTransactionUseCase(any()) } returns 105L

        val proposal = AiTransactionProposal(
            id = "p-cash-fallback",
            title = "Makan Siang",
            amountInCents = 35_000_00L,
            type = "EXPENSE",
            date = 1726000000000L, // Legacy dummy date
            accountId = 999L, // Non-existent account ID
            accountName = "",
            categoryId = 2L,
            categoryName = "Food"
        )

        coEvery {
            aiProvider.parseReceiptOrChat(any(), any(), any(), any(), any(), any())
        } returns AiAssistantResponse(
            reply = "Pengeluaran terdeteksi",
            proposals = listOf(proposal)
        )

        val viewModel = AiChatViewModel(
            aiProviderFactory = aiProviderFactory,
            aiSettingsRepository = aiSettingsRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            expenseRepository = expenseRepository,
            addTransactionUseCase = addTransactionUseCase
        )

        viewModel.sendMessage("makan siang 35rb")
        val assistantMsg = viewModel.uiState.value.messages.last()

        viewModel.confirmProposal(assistantMsg.id, proposal)

        coVerify {
            addTransactionUseCase(
                match { expense: Expense ->
                    expense.title == "Makan Siang" &&
                        expense.amount == 35_000_00L &&
                        expense.accountId == 7L && // Resolved to Cash account!
                        expense.currency == "IDR" &&
                        expense.date != 1726000000000L // Normalized away from legacy dummy date!
                }
            )
        }
    }
}
