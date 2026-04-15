package com.sans.expensetracker.presentation.scan_receipt

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.sans.expensetracker.domain.model.Expense
import com.sans.expensetracker.domain.preferences.AiPreferences
import com.sans.expensetracker.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@Serializable
private data class AiTransactionDto(
    val title: String = "Unknown Item",
    val amount: Long = 0L,
    val category: String = "Misc",
    val dateString: String? = null
)

data class SuggestedTransaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val amount: Long,
    val category: String,
    val dateString: String? = null,
    val isAccepted: Boolean = true
)

data class ScanReceiptState(
    val modelUri: Uri? = null,
    val cachedModelPath: String? = null,
    val imageUri: Uri? = null,
    val isProcessing: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val streamingText: String = "",
    val preprocessedImagePath: String? = null,
    val suggestedTransactions: List<SuggestedTransaction> = emptyList(),
    val availableCategories: List<String> = emptyList(),
    val aiThinking: String? = null,
    val errorMessage: String? = null,
    val noResultsFound: Boolean = false
)

sealed class ScanReceiptEvent {
    data class ModelSelected(val uri: Uri) : ScanReceiptEvent()
    data class CacheModelFile(val context: Context, val uri: Uri) : ScanReceiptEvent()
    data class ImageSelected(val context: Context, val uri: Uri) : ScanReceiptEvent()
    data class ToggleTransactionAcceptance(val id: String) : ScanReceiptEvent()
    object SaveAcceptedTransactions : ScanReceiptEvent()
    data class EditTransactionTitle(val id: String, val title: String) : ScanReceiptEvent()
    data class EditTransactionAmount(val id: String, val amountCents: Long) : ScanReceiptEvent()
    data class EditTransactionCategory(val id: String, val category: String) : ScanReceiptEvent()
    data class EditTransactionDate(val id: String, val dateString: String?) : ScanReceiptEvent()
    object CancelInference : ScanReceiptEvent()
    data class DeleteTransaction(val id: String) : ScanReceiptEvent()
    object ResetForNewScan : ScanReceiptEvent()
}

@HiltViewModel
class ScanReceiptViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val aiPreferences: AiPreferences,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ScanReceiptState())
    val state: StateFlow<ScanReceiptState> = _state.asStateFlow()

    // Pre-warmed engine reused across scans within the same ViewModel session.
    // Closed in onCleared() when the screen leaves composition.
    private var engine: Engine? = null
    private var engineInitJob: Job? = null
    private var inferenceJob: Job? = null

    init {
        viewModelScope.launch {
            aiPreferences.getAiModelPath()
                .distinctUntilChanged()
                .collect { path ->
                    if (path != null && File(path).exists()) {
                        _state.update { it.copy(cachedModelPath = path) }
                        initEngineAsync(path)
                    }
                }
        }
        viewModelScope.launch {
            expenseRepository.getAllCategories().collect { cats ->
                _state.update { it.copy(availableCategories = cats.map { c -> c.name }) }
            }
        }
    }

    /**
     * Initializes the LiteRT engine in the background so it is ready before the user
     * picks an image. GPU backend uses OpenCL (libvndksupport.so + libOpenCL.so declared
     * in the manifest as required=false, so the app still runs on devices without them).
     * NPU backend requires a dedicated per-SoC compiled model artifact and cannot be used
     * with generic user-loaded .litertlm files.
     */
    private fun initEngineAsync(modelPath: String) {
        engineInitJob?.cancel()
        engineInitJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                engine?.close()
                engine = null
                val gpuBackend = Backend.GPU()
                val config = EngineConfig(
                    modelPath = modelPath,
                    backend = gpuBackend,
                    visionBackend = gpuBackend,
                    cacheDir = appContext.cacheDir.absolutePath
                )
                engine = Engine(config).also { it.initialize() }
                Log.d("ScanReceiptViewModel", "Engine pre-warmed successfully")
            } catch (e: Exception) {
                Log.e("ScanReceiptViewModel", "Engine pre-warm failed", e)
                engine = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        engineInitJob?.cancel()
        engine?.close()
        engine = null
    }

    fun onEvent(event: ScanReceiptEvent) {
        when (event) {
            is ScanReceiptEvent.ModelSelected -> {
                _state.update { it.copy(modelUri = event.uri, cachedModelPath = null) }
            }
            is ScanReceiptEvent.CacheModelFile -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val fileName = getFileName(event.context, event.uri)
                    val path = cacheFile(event.context, event.uri, fileName)
                    aiPreferences.setAiModelPath(path)
                    _state.update { it.copy(cachedModelPath = path) }
                    initEngineAsync(path)
                }
            }
            is ScanReceiptEvent.ImageSelected -> {
                inferenceJob?.cancel()
                _state.update {
                    it.copy(
                        imageUri = event.uri,
                        isProcessing = true,
                        preprocessedImagePath = null,
                        suggestedTransactions = emptyList(),
                        aiThinking = null,
                        errorMessage = null,
                        noResultsFound = false,
                        streamingText = ""
                    )
                }
                inferenceJob = viewModelScope.launch(Dispatchers.IO) {
                    val imagePath = prepareImageFile(event.context, event.uri)
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(preprocessedImagePath = imagePath) }
                    }
                    processInference(imagePath)
                }
            }
            is ScanReceiptEvent.ToggleTransactionAcceptance -> {
                _state.update { s ->
                    s.copy(suggestedTransactions = s.suggestedTransactions.map {
                        if (it.id == event.id) it.copy(isAccepted = !it.isAccepted) else it
                    })
                }
            }
            is ScanReceiptEvent.SaveAcceptedTransactions -> {
                _state.update { it.copy(isSaving = true) }
                viewModelScope.launch {
                    val accepted = _state.value.suggestedTransactions.filter { it.isAccepted }
                    val categories = expenseRepository.getAllCategories().firstOrNull() ?: emptyList()
                    val defaultCategoryId = categories.firstOrNull()?.id ?: 1L

                    accepted.forEach { tx ->
                        val matchedCategory = categories.find { it.name.equals(tx.category, ignoreCase = true) }
                        val catId = matchedCategory?.id ?: defaultCategoryId

                        val txDate = try {
                            tx.dateString?.let {
                                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it)?.time
                            } ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }

                        expenseRepository.insertExpense(
                            Expense(
                                date = txDate,
                                itemName = tx.title,
                                amount = tx.amount,
                                categoryId = catId,
                                merchant = null,
                                tags = listOf("Scanned Receipt")
                            )
                        )
                    }
                    _state.update { it.copy(isSaving = false, isSaved = true) }
                }
            }
            is ScanReceiptEvent.EditTransactionTitle -> {
                _state.update { s ->
                    s.copy(suggestedTransactions = s.suggestedTransactions.map {
                        if (it.id == event.id) it.copy(title = event.title) else it
                    })
                }
            }
            is ScanReceiptEvent.EditTransactionAmount -> {
                _state.update { s ->
                    s.copy(suggestedTransactions = s.suggestedTransactions.map {
                        if (it.id == event.id) it.copy(amount = event.amountCents) else it
                    })
                }
            }
            is ScanReceiptEvent.EditTransactionCategory -> {
                _state.update { s ->
                    s.copy(suggestedTransactions = s.suggestedTransactions.map {
                        if (it.id == event.id) it.copy(category = event.category) else it
                    })
                }
            }
            is ScanReceiptEvent.EditTransactionDate -> {
                _state.update { s ->
                    s.copy(suggestedTransactions = s.suggestedTransactions.map {
                        if (it.id == event.id) it.copy(dateString = event.dateString) else it
                    })
                }
            }
            is ScanReceiptEvent.CancelInference -> {
                inferenceJob?.cancel()
                inferenceJob = null
                _state.update {
                    it.copy(
                        isProcessing = false,
                        streamingText = "",
                        errorMessage = null
                    )
                }
            }
            is ScanReceiptEvent.DeleteTransaction -> {
                _state.update { s ->
                    val remaining = s.suggestedTransactions.filter { it.id != event.id }
                    s.copy(
                        suggestedTransactions = remaining,
                        noResultsFound = remaining.isEmpty()
                    )
                }
            }
            is ScanReceiptEvent.ResetForNewScan -> {
                _state.update {
                    it.copy(
                        imageUri = null,
                        isProcessing = false,
                        isSaved = false,
                        streamingText = "",
                        preprocessedImagePath = null,
                        suggestedTransactions = emptyList(),
                        aiThinking = null,
                        errorMessage = null,
                        noResultsFound = false
                    )
                }
            }
        }
    }

    /**
     * Downscales the receipt image to a max dimension of 1024px before inference.
     * Modern camera sensors produce 12–50 MP images; sending the full resolution is
     * wasteful and slows down model input preparation.
     */
    private suspend fun prepareImageFile(context: Context, uri: Uri): String =
        withContext(Dispatchers.IO) {
            val dest = File(context.cacheDir, "input_receipt.jpg")
            val src = context.contentResolver.openInputStream(uri)!!.use {
                BitmapFactory.decodeStream(it)
            }
            val maxDim = 1024
            val bitmap = if (src.width > maxDim || src.height > maxDim) {
                val scale = maxDim.toFloat() / maxOf(src.width, src.height)
                Bitmap.createScaledBitmap(
                    src,
                    (src.width * scale).toInt(),
                    (src.height * scale).toInt(),
                    true
                ).also { src.recycle() }
            } else src
            FileOutputStream(dest).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            bitmap.recycle()
            dest.absolutePath
        }

    private suspend fun cacheFile(context: Context, uri: Uri, fileName: String): String =
        withContext(Dispatchers.IO) {
            val file = File(context.cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        }

    private fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) result = cursor.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) result = result?.substring(cut + 1)
        }
        return result ?: "model.litertlm"
    }

    private suspend fun processInference(imagePath: String) {
        try {
            val modelPath = _state.value.cachedModelPath ?: run {
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(isProcessing = false, errorMessage = "No model loaded. Please select a model file.") }
                }
                return
            }

            if (imagePath.isEmpty() || !File(imagePath).exists()) {
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(isProcessing = false, errorMessage = "Image file not found. Please select the receipt image again.") }
                }
                return
            }

            // Await engine pre-warm if still in progress
            try {
                engineInitJob?.join()
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Engine init was cancelled (e.g. model changed); fall back to inline init
            }

            // Use pre-warmed engine if available; otherwise create one on-the-fly for this scan
            val storedEngine = engine
            val fallbackEngine: Engine?
            val activeEngine: Engine
            if (storedEngine != null) {
                activeEngine = storedEngine
                fallbackEngine = null
            } else {
                Log.w("ScanReceiptViewModel", "Pre-warmed engine unavailable, initializing inline")
                val gpuBackend = Backend.GPU()
                fallbackEngine = Engine(
                    EngineConfig(
                        modelPath = modelPath,
                        backend = gpuBackend,
                        visionBackend = gpuBackend,
                        cacheDir = appContext.cacheDir.absolutePath
                    )
                ).also { it.initialize() }
                activeEngine = fallbackEngine
            }

            try {
                val categoriesList = expenseRepository.getAllCategories().firstOrNull() ?: emptyList()
                val categoryNames = categoriesList.joinToString(", ") { it.name }
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                val systemInstruction = """
                    You are a receipt parsing API. Output ONLY a valid JSON array.
                    No markdown, no talk. Use <think>...</think> for thoughts.
                """.trimIndent()

                val prompt = """
                    Extract items from this receipt as a JSON array.
                    
                    Inside <think>...</think>, list found items and total.
                    
                    Output only the JSON array with these keys:
                    - "title": (string) item name
                    - "amount": (int) ("amount" or "jumlah") multiply by 100
                    - "category": (string) one of [$categoryNames]
                    - "dateString": (string) YYYY-MM-DD, default $todayDate
                    
                    Ignore taxes, discounts, and totals.
                """.trimIndent()

                val conversationConfig = ConversationConfig(
                    systemInstruction = Contents.of(systemInstruction)
                )

                activeEngine.createConversation(conversationConfig).use { conversation ->
                    val messageFlow = conversation.sendMessageAsync(
                        Contents.of(
                            Content.ImageFile(imagePath),
                            Content.Text(prompt)
                        )
                    )

                    val fullTextBuilder = StringBuilder()

                    withTimeout(120_000L) {
                        messageFlow.collect { messageResponse ->
                            val chunk = messageResponse.contents.contents
                                .filterIsInstance<Content.Text>()
                                .joinToString("\n") { it.text }

                            fullTextBuilder.append(chunk)

                            withContext(Dispatchers.Main) {
                                _state.update { it.copy(streamingText = fullTextBuilder.toString()) }
                            }
                        }
                    }

                    val resultText = fullTextBuilder.toString()

                    // Extract <think>...</think> block
                    var thinking: String? = null
                    var jsonText = resultText
                    val thinkStart = resultText.indexOf("<think>")
                    val thinkEnd = resultText.indexOf("</think>")
                    if (thinkStart != -1 && thinkEnd != -1 && thinkEnd > thinkStart) {
                        thinking = resultText.substring(thinkStart + 7, thinkEnd).trim()
                        jsonText = resultText.substring(thinkEnd + 8).trim()
                    } else if (thinkStart != -1) {
                        thinking = resultText.substring(thinkStart + 7).trim()
                        jsonText = ""
                    }

                    // Strip markdown code fences if the model wrapped the JSON
                    val cleanJson = jsonText
                        .replace(Regex("```json\\s*"), "")
                        .replace(Regex("```\\s*"), "")
                        .trim()

                    val suggestions = mutableListOf<SuggestedTransaction>()
                    var parseError: String? = null
                    try {
                        val json = Json { ignoreUnknownKeys = true }
                        val dtos = json.decodeFromString<List<AiTransactionDto>>(cleanJson)
                        dtos.forEach { dto ->
                            suggestions.add(
                                SuggestedTransaction(
                                    title = dto.title,
                                    amount = dto.amount,
                                    category = dto.category,
                                    dateString = dto.dateString
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("ScanReceiptViewModel", "Could not parse AI response", e)
                        parseError = "Could not parse AI response. Raw: ${cleanJson.take(200)}"
                    }

                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(
                                isProcessing = false,
                                suggestedTransactions = suggestions,
                                aiThinking = thinking,
                                errorMessage = parseError,
                                noResultsFound = suggestions.isEmpty() && parseError == null
                            )
                        }
                    }
                }
            } finally {
                fallbackEngine?.close()
            }
        } catch (e: TimeoutCancellationException) {
            Log.w("ScanReceiptViewModel", "Inference timed out after 120s")
            withContext(Dispatchers.Main) {
                _state.update { it.copy(isProcessing = false, errorMessage = "Receipt analysis timed out. Please try again.") }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) return
            Log.e("ScanReceiptViewModel", "Error during inference", e)
            val msg = e.localizedMessage ?: e.javaClass.simpleName
            withContext(Dispatchers.Main) {
                _state.update { it.copy(isProcessing = false, errorMessage = "Error during inference: $msg") }
            }
        }
    }
}
