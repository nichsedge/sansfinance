package com.sans.expensetracker.presentation.scan_invoice

import android.content.Context
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class SuggestedTransaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val amount: Long,
    val category: String,
    val dateString: String? = null,
    val isAccepted: Boolean = true
)

data class ScanInvoiceState(
    val modelUri: Uri? = null,
    val cachedModelPath: String? = null,
    val imageUri: Uri? = null,
    val isProcessing: Boolean = false,
    val suggestedTransactions: List<SuggestedTransaction> = emptyList(),
    val aiThinking: String? = null,
    val errorMessage: String? = null
)

sealed class ScanInvoiceEvent {
    data class ModelSelected(val uri: Uri) : ScanInvoiceEvent()
    data class CacheModelFile(val context: Context, val uri: Uri) : ScanInvoiceEvent()
    data class ImageSelected(val context: Context, val uri: Uri) : ScanInvoiceEvent()
    data class ToggleTransactionAcceptance(val id: String) : ScanInvoiceEvent()
    object SaveAcceptedTransactions : ScanInvoiceEvent()
}

@HiltViewModel
class ScanInvoiceViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val aiPreferences: AiPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(ScanInvoiceState())
    val state: StateFlow<ScanInvoiceState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            aiPreferences.getAiModelPath().collect { path ->
                if (path != null && File(path).exists()) {
                    _state.update { it.copy(cachedModelPath = path) }
                }
            }
        }
    }

    fun onEvent(event: ScanInvoiceEvent) {
        when (event) {
            is ScanInvoiceEvent.ModelSelected -> {
                _state.update { it.copy(modelUri = event.uri, cachedModelPath = null) }
            }
            is ScanInvoiceEvent.CacheModelFile -> {
                viewModelScope.launch {
                    val path = cacheFile(event.context, event.uri, "model.litertlm")
                    aiPreferences.setAiModelPath(path)
                    _state.update { it.copy(cachedModelPath = path) }
                }
            }
            is ScanInvoiceEvent.ImageSelected -> {
                _state.update { it.copy(imageUri = event.uri, isProcessing = true, suggestedTransactions = emptyList(), aiThinking = null, errorMessage = null) }
                viewModelScope.launch(Dispatchers.IO) {
                    val path = cacheFile(event.context, event.uri, "input_invoice.jpg")
                    processInference(event.context, path)
                }
            }
            is ScanInvoiceEvent.ToggleTransactionAcceptance -> {
                _state.update { currentState ->
                    currentState.copy(
                        suggestedTransactions = currentState.suggestedTransactions.map {
                            if (it.id == event.id) it.copy(isAccepted = !it.isAccepted) else it
                        }
                    )
                }
            }
            is ScanInvoiceEvent.SaveAcceptedTransactions -> {
                viewModelScope.launch {
                    val accepted = _state.value.suggestedTransactions.filter { it.isAccepted }

                    // Fetch categories to try and match the string or assign default
                    val categories = expenseRepository.getAllCategories().firstOrNull() ?: emptyList()
                    val defaultCategoryId = categories.firstOrNull()?.id ?: 1L

                    accepted.forEach { tx ->
                        // Basic fuzzy match or fallback
                        val matchedCategory = categories.find { it.name.equals(tx.category, ignoreCase = true) }
                        val catId = matchedCategory?.id ?: defaultCategoryId

                        // Parse date if available
                        val txDate = try {
                            tx.dateString?.let {
                                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(it)?.time
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
                                merchant = "Scanned Invoice",
                                tags = listOf("AI Scanned")
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun cacheFile(context: Context, uri: Uri, fileName: String): String {
        return withContext(Dispatchers.IO) {
            val file = File(context.cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        }
    }

    private suspend fun processInference(context: Context, imagePath: String) {
        try {
            val modelPath = _state.value.cachedModelPath ?: run {
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(isProcessing = false, errorMessage = "No model loaded. Please select a model file.") }
                }
                return
            }

            // Use GPU backend for Dimensity 9300+ (supports OpenCL).
            // GPU backend requires libvndksupport.so and libOpenCL.so declared in the manifest.
            // NPU backend requires a special NPU-compiled .litertlm file per SoC model (not a generic model).
            val gpuBackend = Backend.GPU()
            val engineConfig = EngineConfig(
                modelPath = modelPath,
                backend = gpuBackend,
                visionBackend = gpuBackend,
                // Caching compiled model artifacts speeds up loading after the first run
                cacheDir = context.cacheDir.absolutePath
            )

            Engine(engineConfig).use { engine ->
                engine.initialize()

                val categoriesList = expenseRepository.getAllCategories().firstOrNull() ?: emptyList()
                val categoryNames = categoriesList.joinToString(", ") { it.name }
                val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

                val conversationConfig = ConversationConfig(
                    systemInstruction = Contents.of("You are a helpful assistant that acts as a JSON API. Only output valid JSON, no extra text outside the JSON array unless inside <think> tags.")
                )

                engine.createConversation(conversationConfig).use { conversation ->
                    val prompt = "Extract all purchased items from this invoice. First, analyze the invoice step by step, identifying items, amounts, categories, and dates. Enclose this reasoning strictly inside <think>...</think> tags. Then, respond with ONLY a valid JSON array of the extracted items. Each object in the array must have these properties: 'title' (string), 'amount' (integer representing the value multiplied by 100, e.g., 529,000 IDR becomes 52900000, 50.00 becomes 5000), 'category' (string, choose the closest match from: [${categoryNames}], else use 'Misc'), and 'dateString' (string, format YYYY-MM-DD. If the invoice date is missing, use today's date: ${todayDate})."

                    val messageResponse = if (imagePath.isNotEmpty() && File(imagePath).exists()) {
                        conversation.sendMessage(
                            Contents.of(
                                Content.ImageFile(imagePath),
                                Content.Text(prompt)
                            )
                        )
                    } else {
                        withContext(Dispatchers.Main) {
                            _state.update { it.copy(isProcessing = false, errorMessage = "Image file not found. Please select the invoice image again.") }
                        }
                        return
                    }

                    // Extract the text content from the model response
                    val resultText = messageResponse.contents.contents
                        .filterIsInstance<Content.Text>()
                        .joinToString("\n") { it.text }

                    // Extract thinking block
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
                        val jsonArray = JSONArray(cleanJson)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            suggestions.add(
                                SuggestedTransaction(
                                    title = obj.optString("title", "Unknown Item"),
                                    amount = obj.optLong("amount", 0L),
                                    category = obj.optString("category", "Uncategorized"),
                                    dateString = if (obj.has("dateString") && !obj.isNull("dateString")) obj.getString("dateString") else null
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("ScanInvoiceViewModel", "Could not parse AI response", e)
                        parseError = "Could not parse AI response. Raw: ${cleanJson.take(200)}"
                    }

                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(
                                isProcessing = false,
                                suggestedTransactions = suggestions,
                                aiThinking = thinking,
                                errorMessage = parseError
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ScanInvoiceViewModel", "Error during inference", e)
            val msg = e.localizedMessage ?: e.javaClass.simpleName
            withContext(Dispatchers.Main) {
                _state.update { it.copy(isProcessing = false, errorMessage = "Error during inference: $msg") }
            }
        }
    }
}
