package com.sans.finance.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.ai.AiProviderFactory
import com.sans.finance.data.ai.AiSettingsRepository
import com.sans.finance.domain.model.AccountSummary
import com.sans.finance.domain.model.AiTransactionProposal
import com.sans.finance.domain.model.CategorySummary
import com.sans.finance.domain.model.ChatMessage
import com.sans.finance.domain.model.ChatSender
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.CategoryRepository
import com.sans.finance.domain.usecase.AddTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAiConfigured: Boolean = true,
    val accounts: List<AccountSummary> = emptyList(),
    val categories: List<CategorySummary> = emptyList(),
    val successSnackbarMessage: String? = null
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val aiProviderFactory: AiProviderFactory,
    private val aiSettingsRepository: AiSettingsRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val addTransactionUseCase: AddTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        addWelcomeMessage()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val accountsFlow = accountRepository.getAllAccounts()
            val categoriesFlow = categoryRepository.getAllCategories()
            val settings = aiSettingsRepository.settings.first()

            val isConfigured = when (settings.provider) {
                com.sans.finance.data.ai.AiProviderType.OFF -> false
                com.sans.finance.data.ai.AiProviderType.OPENAI -> settings.openAiApiKey.isNotBlank()
                com.sans.finance.data.ai.AiProviderType.OPENROUTER -> settings.openRouterApiKey.isNotBlank()
            }

            accountsFlow.collect { rawAccounts ->
                val accountSummaries = rawAccounts.map {
                    AccountSummary(id = it.id, name = it.name, type = it.type, currency = it.currency)
                }
                val defaultCash = com.sans.finance.data.ai.AiJsonParser.findDefaultCashAccount(accountSummaries)
                _uiState.update { state ->
                    val updatedMessages = state.messages.map { msg ->
                        if (msg.proposals.isNotEmpty()) {
                            val fixedProposals = msg.proposals.map { p ->
                                if (accountSummaries.none { it.id == p.accountId } && defaultCash != null) {
                                    p.copy(accountId = defaultCash.id, accountName = defaultCash.name)
                                } else p
                            }
                            msg.copy(proposals = fixedProposals)
                        } else msg
                    }
                    state.copy(
                        accounts = accountSummaries,
                        isAiConfigured = isConfigured,
                        messages = updatedMessages
                    )
                }
            }
        }

        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { rawCategories ->
                val categorySummaries = rawCategories.map {
                    CategorySummary(id = it.id, name = it.name, type = it.type)
                }
                _uiState.update { it.copy(categories = categorySummaries) }
            }
        }
    }

    private fun addWelcomeMessage() {
        val welcome = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = ChatSender.ASSISTANT,
            text = "Halo! Saya Sans Finance Copilot. Anda bisa berdiskusi tentang strategi keuangan, budgeting, dana darurat, atau langsung menempel bukti transfer/struk belanja (QRIS, kupon SBN, e-commerce, gaji, dll) untuk pencatatan otomatis dengan konfirmasi Anda."
        )
        _uiState.update { it.copy(messages = listOf(welcome)) }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage(overrideText: String? = null) {
        val textToSend = (overrideText ?: _uiState.value.inputText).trim()
        if (textToSend.isBlank() || _uiState.value.isLoading) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = ChatSender.USER,
            text = textToSend
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val provider = aiProviderFactory.create()
                if (provider == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAiConfigured = false,
                            errorMessage = "API key AI belum diset. Silakan konfigurasi API key OpenRouter Anda di Pengaturan > AI Settings."
                        )
                    }
                    return@launch
                }

                val accounts = _uiState.value.accounts
                val categories = _uiState.value.categories
                val baseCurrency = accounts.firstOrNull()?.currency ?: "IDR"

                val response = provider.parseReceiptOrChat(
                    userMessage = textToSend,
                    accounts = accounts,
                    categories = categories,
                    currency = baseCurrency,
                    conversationHistory = _uiState.value.messages.takeLast(6)
                )

                val assistantMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sender = ChatSender.ASSISTANT,
                    text = response.reply,
                    proposals = response.proposals
                )

                _uiState.update {
                    it.copy(
                        messages = it.messages + assistantMessage,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AiChatViewModel", "Gagal memproses AI: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Gagal memproses AI: ${e.localizedMessage ?: e.message}"
                    )
                }
            }
        }
    }

    fun confirmProposal(messageId: String, proposal: AiTransactionProposal) {
        viewModelScope.launch {
            try {
                val expense = createExpenseFromProposal(proposal, _uiState.value.accounts)
                addTransactionUseCase(expense)

                _uiState.update { state ->
                    val updatedMessages = state.messages.map { msg ->
                        if (msg.id == messageId) {
                            val updatedProposals = msg.proposals.map { p ->
                                if (p.id == proposal.id) p.copy(isConfirmed = true, isRejected = false) else p
                            }
                            msg.copy(proposals = updatedProposals)
                        } else {
                            msg
                        }
                    }
                    state.copy(
                        messages = updatedMessages,
                        successSnackbarMessage = "Berhasil mencatat ${proposal.title}!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Gagal menyimpan transaksi: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }

    fun confirmAllProposals(messageId: String) {
        viewModelScope.launch {
            try {
                val targetMsg = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return@launch
                val pending = targetMsg.proposals.filter { !it.isConfirmed && !it.isRejected }
                if (pending.isEmpty()) return@launch

                for (proposal in pending) {
                    val expense = createExpenseFromProposal(proposal, _uiState.value.accounts)
                    addTransactionUseCase(expense)
                }

                _uiState.update { state ->
                    val updatedMessages = state.messages.map { msg ->
                        if (msg.id == messageId) {
                            val updatedProposals = msg.proposals.map { p ->
                                if (!p.isRejected) p.copy(isConfirmed = true) else p
                            }
                            msg.copy(proposals = updatedProposals)
                        } else {
                            msg
                        }
                    }
                    state.copy(
                        messages = updatedMessages,
                        successSnackbarMessage = "Berhasil mencatat ${pending.size} transaksi sekaligus!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Gagal menyimpan transaksi massal: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }

    private fun createExpenseFromProposal(proposal: AiTransactionProposal, accounts: List<AccountSummary>): Expense {
        val targetAccount = resolveAccount(proposal, accounts)
        val finalAccountId = targetAccount?.id ?: proposal.accountId.takeIf { it > 0 } ?: 1L
        val currency = targetAccount?.currency ?: accounts.firstOrNull()?.currency ?: "IDR"
        val normalizedDate = normalizeDate(proposal.date)

        return Expense(
            date = normalizedDate,
            title = proposal.title,
            amount = proposal.amountInCents,
            categoryId = proposal.categoryId,
            accountId = finalAccountId,
            type = proposal.type,
            details = proposal.notes.ifBlank { null },
            tags = proposal.tags,
            currency = currency
        )
    }

    private fun resolveAccount(proposal: AiTransactionProposal, accounts: List<AccountSummary>): AccountSummary? {
        val existing = accounts.firstOrNull { it.id == proposal.accountId }
        if (existing != null) return existing

        val byName = if (proposal.accountName.isNotBlank()) {
            accounts.firstOrNull { it.name.equals(proposal.accountName, ignoreCase = true) }
                ?: accounts.firstOrNull { it.name.contains(proposal.accountName, ignoreCase = true) }
        } else null
        if (byName != null) return byName

        return com.sans.finance.data.ai.AiJsonParser.findDefaultCashAccount(accounts)
    }

    private fun normalizeDate(date: Long): Long {
        val now = System.currentTimeMillis()
        return when {
            date <= 0L || date == 1726000000000L -> now
            date < 100_000_000_000L -> date * 1000L // 10-digit epoch timestamp in seconds -> ms
            else -> date
        }
    }

    fun updateProposal(messageId: String, updatedProposal: AiTransactionProposal) {
        _uiState.update { state ->
            val updatedMessages = state.messages.map { msg ->
                if (msg.id == messageId) {
                    val updatedProposals = msg.proposals.map { p ->
                        if (p.id == updatedProposal.id) updatedProposal else p
                    }
                    msg.copy(proposals = updatedProposals)
                } else {
                    msg
                }
            }
            state.copy(messages = updatedMessages)
        }
    }

    fun rejectProposal(messageId: String, proposalId: String) {
        _uiState.update { state ->
            val updatedMessages = state.messages.map { msg ->
                if (msg.id == messageId) {
                    val updatedProposals = msg.proposals.map { p ->
                        if (p.id == proposalId) p.copy(isRejected = true) else p
                    }
                    msg.copy(proposals = updatedProposals)
                } else {
                    msg
                }
            }
            state.copy(messages = updatedMessages)
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(successSnackbarMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
