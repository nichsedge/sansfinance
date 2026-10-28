package com.sans.finance.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.ai.AiJsonParser
import com.sans.finance.data.ai.AiProviderFactory
import com.sans.finance.data.ai.AiSettingsRepository
import com.sans.finance.domain.model.AccountSummary
import com.sans.finance.domain.model.AiTransactionProposal
import com.sans.finance.domain.model.CategorySummary
import com.sans.finance.domain.model.ChatMessage
import com.sans.finance.domain.model.ChatSender
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.model.StreamEvent
import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.AccountTypeRepository
import com.sans.finance.domain.repository.CategoryRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.PortfolioRepository
import com.sans.finance.domain.usecase.AddTransactionUseCase
import com.sans.finance.domain.usecase.GetWealthMetricsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.sans.finance.domain.usecase.GetCashFlowPacingUseCase
import com.sans.finance.domain.usecase.GetInstallmentHorizonUseCase
import com.sans.finance.domain.usecase.GetDividendYieldSummaryUseCase
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.coroutines.withContext
import android.net.Uri
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject

data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val errorMessage: String? = null,
    val isAiConfigured: Boolean = true,
    val accounts: List<AccountSummary> = emptyList(),
    val categories: List<CategorySummary> = emptyList(),
    val successSnackbarMessage: String? = null,
    val selectedImageUri: String? = null,
    val selectedImageBase64: String? = null,
    val selectedFileName: String? = null,
    val isPdf: Boolean = false
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val aiProviderFactory: AiProviderFactory,
    private val aiSettingsRepository: AiSettingsRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val getWealthMetricsUseCase: GetWealthMetricsUseCase,
    private val portfolioRepository: PortfolioRepository,
    private val accountTypeRepository: AccountTypeRepository,
    private val currencyDao: CurrencyDao,
    private val getCashFlowPacingUseCase: GetCashFlowPacingUseCase,
    private val getInstallmentHorizonUseCase: GetInstallmentHorizonUseCase,
    private val getDividendYieldSummaryUseCase: GetDividendYieldSummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    /** Active streaming job — cancelled on stop or new message. */
    private var streamingJob: Job? = null

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
                val defaultCash = AiJsonParser.findDefaultCashAccount(accountSummaries)
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

    fun onFileSelected(uri: Uri?, context: Context) {
        if (uri == null) {
            _uiState.update { it.copy(selectedImageUri = null, selectedImageBase64 = null, selectedFileName = null, isPdf = false) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mimeType = context.contentResolver.getType(uri) ?: ""
                val isPdf = mimeType.contains("pdf", ignoreCase = true) || uri.toString().endsWith(".pdf", ignoreCase = true)

                var fileName: String? = null
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex)
                    }
                }

                val bitmap: Bitmap? = if (isPdf) {
                    renderPdfToBitmap(uri, context)
                } else {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bmp = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    bmp
                }

                if (bitmap != null) {
                    val maxDim = if (isPdf) 2560 else 1600
                    val ratio = minOf(1f, maxDim.toFloat() / maxOf(bitmap.width, bitmap.height).toFloat())
                    val scaled = if (ratio < 1f) {
                        Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * ratio).toInt(),
                            (bitmap.height * ratio).toInt(),
                            true
                        )
                    } else bitmap

                    val stream = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 78, stream)
                    val bytes = stream.toByteArray()
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

                    _uiState.update {
                        it.copy(
                            selectedImageUri = uri.toString(),
                            selectedImageBase64 = base64,
                            selectedFileName = fileName ?: if (isPdf) "Dokumen.pdf" else "Gambar.jpg",
                            isPdf = isPdf
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AiChatViewModel", "Gagal memproses file: ${e.message}", e)
            }
        }
    }

    private fun renderPdfToBitmap(uri: Uri, context: Context): Bitmap? {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        return try {
            val renderer = android.graphics.pdf.PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            if (pageCount == 0) return null
            val pagesToRender = minOf(pageCount, 4)
            val scale = 2f

            var totalHeight = 0
            var maxWidth = 0
            for (i in 0 until pagesToRender) {
                val page = renderer.openPage(i)
                totalHeight += (page.height * scale).toInt()
                maxWidth = maxOf(maxWidth, (page.width * scale).toInt())
                page.close()
            }

            val finalBitmap = Bitmap.createBitmap(maxWidth, totalHeight, Bitmap.Config.ARGB_8888)
            val finalCanvas = android.graphics.Canvas(finalBitmap)
            finalCanvas.drawColor(android.graphics.Color.WHITE)

            var currentY = 0f
            for (i in 0 until pagesToRender) {
                val page = renderer.openPage(i)
                val pw = (page.width * scale).toInt()
                val ph = (page.height * scale).toInt()
                val pageBitmap = Bitmap.createBitmap(pw, ph, Bitmap.Config.ARGB_8888)
                val pageCanvas = android.graphics.Canvas(pageBitmap)
                pageCanvas.drawColor(android.graphics.Color.WHITE)
                page.render(pageBitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                finalCanvas.drawBitmap(pageBitmap, 0f, currentY, null)
                currentY += ph
                pageBitmap.recycle()
                page.close()
            }
            renderer.close()
            finalBitmap
        } catch (e: Exception) {
            android.util.Log.e("AiChatViewModel", "Gagal merender PDF: ${e.message}", e)
            null
        } finally {
            runCatching { pfd.close() }
        }
    }

    fun onClearSelectedImage() {
        _uiState.update { it.copy(selectedImageUri = null, selectedImageBase64 = null, selectedFileName = null, isPdf = false) }
    }

    private suspend fun executeLocalTool(name: String, argumentsJson: String): String = withContext(Dispatchers.IO) {
        try {
            when (name) {
                "get_safe_to_spend" -> {
                    val pacing = getCashFlowPacingUseCase().first()
                    buildJsonObject {
                        put("dailyAllowance", JsonPrimitive(pacing.dailyAllowance))
                        put("remainingDaysInCycle", JsonPrimitive(pacing.remainingDaysInCycle))
                        put("remainingBudget", JsonPrimitive(pacing.remainingBudget))
                        put("committedUpcomingBills", JsonPrimitive(pacing.committedUpcomingBills))
                        put("netDiscretionaryRemaining", JsonPrimitive(pacing.netDiscretionaryRemaining))
                        put("pacingPcnt", JsonPrimitive(pacing.pacingPcnt))
                        put("currencyCode", JsonPrimitive(pacing.currencyCode))
                    }.toString()
                }
                "get_installment_horizon" -> {
                    val roadmap = getInstallmentHorizonUseCase().first()
                    buildJsonObject {
                        put("currentMonthlyCommitment", JsonPrimitive(roadmap.currentMonthlyCommitment))
                        put("totalOutstandingBalance", JsonPrimitive(roadmap.totalOutstandingBalance))
                        put("currencyCode", JsonPrimitive(roadmap.currencyCode))
                        roadmap.fullyPaidDateEpochMs?.let {
                            val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(it))
                            put("fullyPaidDate", JsonPrimitive(dateStr))
                        }
                        put("milestones", buildJsonArray {
                            roadmap.milestones.take(6).forEach { m ->
                                add(buildJsonObject {
                                    put("monthsAhead", JsonPrimitive(m.monthsAhead))
                                    put("monthlyPaymentDue", JsonPrimitive(m.monthlyPaymentDue))
                                    put("monthlyCashFlowFreed", JsonPrimitive(m.monthlyCashFlowFreed))
                                    put("remainingActivePlansCount", JsonPrimitive(m.remainingActivePlansCount))
                                })
                            }
                        })
                    }.toString()
                }
                "query_expenses" -> {
                    val argsObj = runCatching { org.json.JSONObject(argumentsJson) }.getOrNull()
                    val query = argsObj?.optString("query")?.takeIf { it.isNotBlank() }
                    val categoryName = argsObj?.optString("categoryName")?.takeIf { it.isNotBlank() }
                    val daysAgo = argsObj?.optInt("daysAgo", 30) ?: 30
                    val limit = argsObj?.optInt("limit", 10) ?: 10

                    val since = System.currentTimeMillis() - (daysAgo * 86_400_000L)
                    val allTx = expenseRepository.getExpensesBetween(since, System.currentTimeMillis()).first()
                    val filtered = allTx
                        .filter { tx ->
                            val matchesQuery = query == null || tx.title.contains(query, ignoreCase = true) ||
                                (tx.details != null && tx.details.contains(query, ignoreCase = true))
                            val matchesCategory = categoryName == null ||
                                tx.categoryName.equals(categoryName, ignoreCase = true) ||
                                (tx.categoryName != null && tx.categoryName.contains(categoryName, ignoreCase = true))
                            matchesQuery && matchesCategory
                        }
                        .take(limit)

                    buildJsonObject {
                        put("count", JsonPrimitive(filtered.size))
                        put("items", buildJsonArray {
                            filtered.forEach { tx ->
                                val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(tx.date))
                                add(buildJsonObject {
                                    put("title", JsonPrimitive(tx.title))
                                    put("amount", JsonPrimitive(tx.amount))
                                    put("type", JsonPrimitive(tx.type))
                                    put("date", JsonPrimitive(dateStr))
                                    put("category", JsonPrimitive(tx.categoryName ?: "General"))
                                    put("accountId", JsonPrimitive(tx.accountId))
                                })
                            }
                        })
                    }.toString()
                }
                "get_passive_income_summary" -> {
                    val yield = getDividendYieldSummaryUseCase().first()
                    buildJsonObject {
                        put("totalAnnualIncomeInBase", JsonPrimitive(yield.totalAnnualIncomeInBase))
                        put("totalMonthlyIncomeInBase", JsonPrimitive(yield.totalMonthlyIncomeInBase))
                        put("portfolioYieldOnCost", JsonPrimitive(String.format(java.util.Locale.US, "%.2f%%", yield.portfolioYieldOnCost * 100.0)))
                        put("expenseCoveragePercentage", JsonPrimitive(String.format(java.util.Locale.US, "%.1f%%", yield.expenseCoveragePercentage)))
                        put("baseCurrency", JsonPrimitive(yield.baseCurrency))
                        put("holdingsWithYieldCount", JsonPrimitive(yield.holdingsWithYield.size))
                    }.toString()
                }
                else -> "{ \"status\": \"unknown_tool\" }"
            }
        } catch (e: Exception) {
            android.util.Log.e("AiChatViewModel", "Error executing tool $name: ${e.message}", e)
            "{ \"error\": \"${e.message}\" }"
        }
    }

    fun sendMessage(overrideText: String? = null) {
        val textToSend = (overrideText ?: _uiState.value.inputText).trim()
        val imageUri = _uiState.value.selectedImageUri
        val imageBase64 = _uiState.value.selectedImageBase64

        if ((textToSend.isBlank() && imageUri == null) || _uiState.value.isLoading || _uiState.value.isStreaming) return

        // Cancel any ongoing stream
        streamingJob?.cancel()

        val displayText = if (textToSend.isBlank() && imageUri != null) {
            if (_uiState.value.isPdf) "Analisis dokumen PDF ini dan catat semua transaksinya" else "Analisis struk ini"
        } else textToSend

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = ChatSender.USER,
            text = displayText,
            imageUri = imageUri
        )

        // Create an empty assistant placeholder for streaming
        val assistantId = UUID.randomUUID().toString()
        val assistantPlaceholder = ChatMessage(
            id = assistantId,
            sender = ChatSender.ASSISTANT,
            text = "",
            isStreaming = true
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage + assistantPlaceholder,
                inputText = "",
                selectedImageUri = null,
                selectedImageBase64 = null,
                selectedFileName = null,
                isPdf = false,
                isLoading = true,
                isStreaming = true,
                errorMessage = null
            )
        }

        streamingJob = viewModelScope.launch {
            try {
                val provider = aiProviderFactory.create()
                if (provider == null) {
                    _uiState.update {
                        it.copy(
                            messages = it.messages.filter { msg -> msg.id != assistantId },
                            isLoading = false,
                            isStreaming = false,
                            isAiConfigured = false,
                            errorMessage = "API key AI belum diset. Silakan konfigurasi API key OpenRouter Anda di Pengaturan > AI Settings."
                        )
                    }
                    return@launch
                }

                val accounts = _uiState.value.accounts
                val categories = _uiState.value.categories
                val baseCurrency = accounts.firstOrNull()?.currency ?: "IDR"

                // Fast in-context financial snapshot from Room DB
                val financialSnapshot = runCatching { getFinancialSnapshot(baseCurrency) }.getOrNull()

                // Use streaming with native tools and thinking tokens
                provider.streamChat(
                    userMessage = displayText,
                    accounts = accounts,
                    categories = categories,
                    currency = baseCurrency,
                    conversationHistory = _uiState.value.messages
                        .filter { it.id != assistantId }
                        .takeLast(6),
                    financialContext = financialSnapshot,
                    base64Image = imageBase64,
                    toolExecutor = ::executeLocalTool
                ).collect { event ->
                    when (event) {
                        is StreamEvent.ThinkingDelta -> {
                            _uiState.update { state ->
                                val updated = state.messages.map { msg ->
                                    if (msg.id == assistantId) {
                                        msg.copy(thinkingText = msg.thinkingText + event.text)
                                    } else msg
                                }
                                state.copy(messages = updated, isLoading = false)
                            }
                        }

                        is StreamEvent.TextDelta -> {
                            _uiState.update { state ->
                                val updated = state.messages.map { msg ->
                                    if (msg.id == assistantId) {
                                        msg.copy(text = msg.text + event.text, statusText = null)
                                    } else msg
                                }
                                state.copy(messages = updated, isLoading = false)
                            }
                        }

                        is StreamEvent.StatusUpdate -> {
                            _uiState.update { state ->
                                val updated = state.messages.map { msg ->
                                    if (msg.id == assistantId) {
                                        msg.copy(statusText = event.statusText)
                                    } else msg
                                }
                                state.copy(messages = updated)
                            }
                        }

                        is StreamEvent.Done -> {
                            _uiState.update { state ->
                                val updated = state.messages.map { msg ->
                                    if (msg.id == assistantId) {
                                        msg.copy(
                                            text = event.fullText.ifBlank { msg.text },
                                            proposals = event.proposals,
                                            isStreaming = false,
                                            statusText = null
                                        )
                                    } else msg
                                }
                                state.copy(
                                    messages = updated,
                                    isLoading = false,
                                    isStreaming = false
                                )
                            }
                        }

                        is StreamEvent.Error -> {
                            _uiState.update { state ->
                                val cleaned = state.messages.filter { msg ->
                                    msg.id != assistantId || msg.text.isNotBlank()
                                }.map { msg ->
                                    if (msg.id == assistantId) msg.copy(isStreaming = false, statusText = null)
                                    else msg
                                }
                                state.copy(
                                    messages = cleaned,
                                    isLoading = false,
                                    isStreaming = false,
                                    errorMessage = event.message
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AiChatViewModel", "Gagal memproses AI: ${e.message}", e)
                _uiState.update { state ->
                    val cleaned = state.messages.map { msg ->
                        if (msg.id == assistantId) msg.copy(isStreaming = false, statusText = null) else msg
                    }.filter { msg ->
                        msg.id != assistantId || msg.text.isNotBlank()
                    }
                    state.copy(
                        messages = cleaned,
                        isLoading = false,
                        isStreaming = false,
                        errorMessage = "Gagal memproses AI: ${e.localizedMessage ?: e.message}"
                    )
                }
            }
        }
    }

    /** Stop the active streaming generation. Keeps whatever text has arrived so far. */
    fun stopGeneration() {
        streamingJob?.cancel()
        streamingJob = null
        _uiState.update { state ->
            val updated = state.messages.map { msg ->
                if (msg.isStreaming) msg.copy(isStreaming = false) else msg
            }
            state.copy(
                messages = updated,
                isLoading = false,
                isStreaming = false
            )
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

        return AiJsonParser.findDefaultCashAccount(accounts)
    }

    private fun normalizeDate(date: Long): Long {
        val now = System.currentTimeMillis()
        return when {
            date <= 0L || date == 1726000000000L -> now
            date < 100_000_000_000L -> date * 1000L
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

    private suspend fun getFinancialSnapshot(baseCurrency: String): com.sans.finance.domain.model.FinancialContextSnapshot {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfMonth = cal.timeInMillis
        cal.add(java.util.Calendar.MONTH, 1)
        val endOfMonth = cal.timeInMillis

        val monthLabel = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
            .format(java.util.Date(startOfMonth))

        val income = expenseRepository.getTotalAmountByTypeBetween(startOfMonth, endOfMonth, "INCOME").first() ?: 0L
        val expense = expenseRepository.getTotalAmountByTypeBetween(startOfMonth, endOfMonth, "EXPENSE").first() ?: 0L
        val net = income - expense
        val savingsRate = if (income > 0) net.toFloat() / income.toFloat() else 0f

        val topCategories = expenseRepository.getBreakdownByCategoryBetween(startOfMonth, endOfMonth, "EXPENSE")
            .first()
            .take(5)
            .map { it.categoryName to it.totalAmount }

        // Wealth & Balance Sheet metrics
        val wealthMetrics = runCatching { getWealthMetricsUseCase().first() }.getOrNull()
        val rawAccounts = accountRepository.getAllAccounts().first()
        val accountTypes = accountTypeRepository.getAllAccountTypes().first()
        val holdings = runCatching { portfolioRepository.getLatestSnapshot().first() }.getOrDefault(emptyList())
        val rates = currencyDao.getAllRates().first()
        val ratesMap = rates.associate { it.code to it.rateToIdr }
        val baseRate = if (baseCurrency == "IDR") 1.0 else ratesMap[baseCurrency] ?: 1.0

        val convertToBase: (Long, String) -> Long = { amount, from ->
            if (from == baseCurrency) amount
            else {
                val fromRate = if (from == "IDR") 1.0 else ratesMap[from] ?: 1.0
                val toRate = baseRate
                if (toRate == 0.0) amount else ((amount * fromRate) / toRate).toLong()
            }
        }

        val convertHoldingIdrToCents: (Double) -> Long = { idrValue ->
            if (baseRate > 0) ((idrValue / baseRate) * 100).toLong() else (idrValue * 100).toLong()
        }

        val liabilityTypeNames = accountTypes.filter { it.isLiability }.map { it.name }.toSet()
        val investmentTypeNames = accountTypes.filter { it.isInvestment }.map { it.name }.toSet()

        val liquidAccounts = rawAccounts
            .filter { it.type !in liabilityTypeNames && it.type !in investmentTypeNames }
            .map { "${it.name} (${it.type})" to convertToBase(it.balance, it.currency) }

        val liabilityAccounts = rawAccounts
            .filter { it.type in liabilityTypeNames }
            .map { "${it.name} (${it.type})" to convertToBase(it.balance, it.currency) }

        val investmentAccountAssets = rawAccounts
            .filter { it.type in investmentTypeNames }
            .sumOf { convertToBase(it.balance, it.currency) }

        val liquidCashAssets = wealthMetrics?.cashAssets ?: liquidAccounts.sumOf { it.second }
        val totalLiabilities = wealthMetrics?.liabilities ?: liabilityAccounts.sumOf { it.second }
        val portfolioInvestmentValue = wealthMetrics?.portfolioValue ?: run {
            val holdingsCents = convertHoldingIdrToCents(holdings.sumOf { it.valueIdr })
            holdingsCents + investmentAccountAssets
        }
        val totalAssets = liquidCashAssets + portfolioInvestmentValue
        val netWorth = totalAssets - totalLiabilities

        val portfolioAssetClasses = holdings
            .groupBy { holding ->
                when {
                    holding.assetClass.isNotBlank() -> holding.assetClass
                    holding.category.isNotBlank() -> holding.category.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                    else -> "Other Investments"
                }
            }
            .mapValues { entry ->
                convertHoldingIdrToCents(entry.value.sumOf { it.valueIdr })
            }
            .toList()
            .filter { it.second > 0L }
            .sortedByDescending { it.second }
            .let { list ->
                if (list.isEmpty() && investmentAccountAssets > 0L) {
                    rawAccounts.filter { it.type in investmentTypeNames && it.balance > 0L }
                        .map { it.type to convertToBase(it.balance, it.currency) }
                } else list
            }

        val topHoldings = holdings
            .sortedByDescending { it.valueIdr }
            .take(8)
            .map { holding ->
                val assetName = holding.asset.ifBlank { holding.category }
                val assetClass = holding.assetClass.ifBlank { holding.category }
                "$assetName ($assetClass)" to convertHoldingIdrToCents(holding.valueIdr)
            }
            .let { list ->
                if (list.isEmpty() && investmentAccountAssets > 0L) {
                    rawAccounts.filter { it.type in investmentTypeNames && it.balance > 0L }
                        .map { "${it.name} (${it.type})" to convertToBase(it.balance, it.currency) }
                } else list
            }

        val allMonthExpenses = expenseRepository.getExpensesBetween(startOfMonth, endOfMonth).first()

        val recentTx = allMonthExpenses
            .filter { !it.isInstallment || it.isInstallmentPayment }
            .take(6)
            .map { tx ->
                val dateStr = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(java.util.Date(tx.date))
                "$dateStr: ${tx.title} (${tx.type}) ${com.sans.finance.core.util.CurrencyFormatter.formatAmount(tx.amount, baseCurrency)}"
            }

        // Top Big-Ticket discrete expenses this month (e.g. Rent, Tuition, Major Purchases)
        val topBigTickets = allMonthExpenses
            .filter { it.type == "EXPENSE" && (!it.isInstallment || it.isInstallmentPayment) }
            .sortedByDescending { it.amount }
            .take(5)
            .map { tx ->
                val dateStr = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(java.util.Date(tx.date))
                val cat = tx.categoryName ?: "General"
                "$dateStr: ${tx.title} (${com.sans.finance.core.util.CurrencyFormatter.formatAmount(tx.amount, baseCurrency)}) [$cat]"
            }

        // Previous Month (M-1) metrics
        val calPrev = java.util.Calendar.getInstance().apply {
            timeInMillis = startOfMonth
            add(java.util.Calendar.MONTH, -1)
        }
        val startOfPrevMonth = calPrev.timeInMillis
        val endOfPrevMonth = startOfMonth
        val prevMonthLabel = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
            .format(java.util.Date(startOfPrevMonth))

        val prevIncome = expenseRepository.getTotalAmountByTypeBetween(startOfPrevMonth, endOfPrevMonth, "INCOME").first() ?: 0L
        val prevExpense = expenseRepository.getTotalAmountByTypeBetween(startOfPrevMonth, endOfPrevMonth, "EXPENSE").first() ?: 0L
        val prevNet = prevIncome - prevExpense
        val prevSavingsRate = if (prevIncome > 0) prevNet.toFloat() / prevIncome.toFloat() else 0f

        val prevTopCategories = expenseRepository.getBreakdownByCategoryBetween(startOfPrevMonth, endOfPrevMonth, "EXPENSE")
            .first()
            .take(5)
            .map { it.categoryName to it.totalAmount }

        // 3-Month rolling baseline average expense (M-3, M-2, M-1)
        val cal3M = java.util.Calendar.getInstance().apply {
            timeInMillis = startOfMonth
            add(java.util.Calendar.MONTH, -3)
        }
        val startOf3M = cal3M.timeInMillis
        val total3MExpense = expenseRepository.getTotalAmountByTypeBetween(startOf3M, startOfMonth, "EXPENSE").first() ?: 0L
        val threeMonthAverageExpense = if (total3MExpense > 0L) total3MExpense / 3L else 0L

        return com.sans.finance.domain.model.FinancialContextSnapshot(
            monthLabel = monthLabel,
            totalIncomeThisMonth = income,
            totalExpenseThisMonth = expense,
            netCashflowThisMonth = net,
            savingsRatePercentage = savingsRate,
            topExpenseCategories = topCategories,
            accountBalances = liquidAccounts,
            liabilityAccountBalances = liabilityAccounts,
            recentTransactions = recentTx,
            prevMonthLabel = prevMonthLabel,
            prevMonthIncome = prevIncome,
            prevMonthExpense = prevExpense,
            prevMonthSavingsRate = prevSavingsRate,
            prevMonthTopCategories = prevTopCategories,
            threeMonthAverageExpense = threeMonthAverageExpense,
            topBigTicketExpenses = topBigTickets,
            netWorth = netWorth,
            totalAssets = totalAssets,
            liquidCashAssets = liquidCashAssets,
            portfolioInvestmentValue = portfolioInvestmentValue,
            totalLiabilities = totalLiabilities,
            runwayMonths = wealthMetrics?.runwayMonths ?: 0.0,
            monthlyPassiveIncome = wealthMetrics?.monthlyPassiveIncome ?: 0L,
            annualPassiveIncome = wealthMetrics?.annualPassiveIncome ?: 0L,
            portfolioAssetClassBreakdown = portfolioAssetClasses,
            topPortfolioHoldings = topHoldings
        )
    }
}
