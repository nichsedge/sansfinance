package com.sans.expensetracker.presentation.scan_invoice

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.expensetracker.domain.model.Expense
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.firstOrNull
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Content
import com.sans.expensetracker.domain.preferences.AiPreferences
import org.json.JSONArray
import org.json.JSONObject

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
    val aiThinking: String? = null
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
    private val expenseRepository: com.sans.expensetracker.domain.repository.ExpenseRepository,
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
                _state.update { it.copy(imageUri = event.uri, isProcessing = true, suggestedTransactions = emptyList(), aiThinking = null) }
                viewModelScope.launch {
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

    private fun processInference(context: Context, imagePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val modelPath = _state.value.cachedModelPath ?: return@launch

                val engineConfig = EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir), // Utilizing user's Dimensity 9300+ NPU
                    visionBackend = Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir)
                )

                Engine(engineConfig).use { engine ->
                    engine.initialize()

                    val categoriesList = expenseRepository.getAllCategories().firstOrNull() ?: emptyList()
                    val categoryNames = categoriesList.joinToString(", ") { it.name }
                    val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

                    val conversationConfig = ConversationConfig(
                        systemInstruction = Contents.of("You are a helpful assistant that acts as a JSON API.")
                    )

                    engine.createConversation(conversationConfig).use { conversation ->
                        val prompt = "Extract all purchased items from this invoice. First, analyze the invoice step by step, identifying items, amounts, categories, and dates. Enclose this reasoning strictly inside <think>...</think> tags. Then, respond with a valid JSON array of the extracted items. Each object in the array must have these properties: 'title' (string), 'amount' (integer representing cents, multiply the displayed exact value by 100, e.g., if it says 529.000 or 529,000 the amount should be 52900000. If 50.00 it becomes 5000), 'category' (string, choose the closest match from these existing categories if possible: [${categoryNames}], else use 'Misc'), and 'dateString' (string, format YYYY-MM-DD. If the invoice date is missing or missing the year, use today's date: ${todayDate})."

                        val messageResponse = if (imagePath.isNotEmpty() && File(imagePath).exists()) {
                            conversation.sendMessage(
                                Contents.of(
                                    Content.ImageFile(imagePath),
                                    Content.Text(prompt)
                                )
                            )
                        } else {
                            // Fallback if image doesn't exist
                            conversation.sendMessage(prompt)
                        }

                        // Using toString() on the contents to extract the string result from the model safely
                        val resultText = messageResponse.contents.contents.filterIsInstance<Content.Text>().joinToString("\n") { it.text }

                        // Extract thinking block
                        var thinking: String? = null
                        var jsonText = resultText
                        val thinkStart = resultText.indexOf("<think>")
                        val thinkEnd = resultText.indexOf("</think>")
                        if (thinkStart != -1 && thinkEnd != -1 && thinkEnd > thinkStart) {
                            thinking = resultText.substring(thinkStart + 7, thinkEnd).trim()
                            jsonText = resultText.substring(thinkEnd + 8).trim()
                        } else if (thinkStart != -1) {
                            // Unclosed think block, shouldn't normally happen but just in case
                            thinking = resultText.substring(thinkStart + 7).trim()
                            jsonText = ""
                        }

                        // Basic cleanup for markdown json blocks if the model wrapped it
                        val cleanJson = jsonText.replace("```json", "").replace("```", "").trim()

                        val suggestions = mutableListOf<SuggestedTransaction>()
                        try {
                            val jsonArray = JSONArray(cleanJson)
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                suggestions.add(
                                    SuggestedTransaction(
                                        title = obj.optString("title", "Unknown"),
                                        amount = obj.optLong("amount", 0L),
                                        category = obj.optString("category", "Uncategorized"),
                                        dateString = if (obj.has("dateString")) obj.getString("dateString") else null
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // No more mock data fallback
                        }

                        withContext(Dispatchers.Main) {
                            _state.update {
                                it.copy(
                                    isProcessing = false,
                                    suggestedTransactions = suggestions,
                                    aiThinking = thinking
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(isProcessing = false) }
                }
            }
        }
    }
}
