package com.sans.finance.presentation.settings.data

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.util.CsvExporter
import com.sans.finance.data.util.CsvParser
import com.sans.finance.data.util.PortfolioCsvExporter
import com.sans.finance.data.util.PortfolioCsvParser
import com.sans.finance.data.util.PortfolioJsonExporter
import com.sans.finance.data.util.PortfolioJsonImporter
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.PortfolioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

enum class ImportExportType {
    TRANSACTIONS, PORTFOLIO
}

enum class ExportFormat {
    CSV, JSON
}

data class DataManagementState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val activeImportType: ImportExportType? = null,
    val activeExportType: ImportExportType? = null,
    val activeExportFormat: ExportFormat? = null
)

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val portfolioRepository: PortfolioRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(DataManagementState())
    val state: StateFlow<DataManagementState> = _state.asStateFlow()

    fun setImportType(type: ImportExportType) {
        _state.value = _state.value.copy(activeImportType = type)
    }

    fun setExportType(type: ImportExportType, format: ExportFormat) {
        _state.value = _state.value.copy(activeExportType = type, activeExportFormat = format)
    }

    fun onImportFileSelected(uri: Uri) {
        val type = _state.value.activeImportType ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                when (type) {
                    ImportExportType.TRANSACTIONS -> importTransactions(uri)
                    ImportExportType.PORTFOLIO -> importPortfolio(uri)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(message = "Import failed: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun onExportFileSelected(uri: Uri) {
        val type = _state.value.activeExportType ?: return
        val format = _state.value.activeExportFormat ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                when (type) {
                    ImportExportType.TRANSACTIONS -> exportTransactions(uri, format)
                    ImportExportType.PORTFOLIO -> exportPortfolio(uri, format)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(message = "Export failed: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        prettyPrint = true
    }

    private suspend fun importTransactions(uri: Uri) {
        val content = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
            ?: throw Exception("Could not read file")

        val expenses = try {
            if (content.trim().startsWith("[") || content.trim().startsWith("{")) {
                if (content.trim().startsWith("{")) {
                    // Might be a single object or wrapped
                    listOf(json.decodeFromString<Expense>(content))
                } else {
                    json.decodeFromString<List<Expense>>(content)
                }
            } else {
                CsvParser.parse(content)
            }
        } catch (e: Exception) {
            throw Exception("Format not recognized or invalid: ${e.message}")
        }

        if (expenses.isEmpty()) {
            _state.value = _state.value.copy(message = "No transactions found in file")
            return
        }

        expenses.forEach { expenseRepository.insertExpense(it) }
        _state.value = _state.value.copy(message = "Successfully imported ${expenses.size} transactions")
    }

    private suspend fun importPortfolio(uri: Uri) {
        val content = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
            ?: throw Exception("Could not read file")

        val (date, items, exchangeRate) = try {
            if (content.trim().startsWith("{")) {
                PortfolioJsonImporter.parseContent(content)
            } else {
                PortfolioCsvParser.parse(content)
            }
        } catch (e: Exception) {
            throw Exception("Format not recognized or invalid: ${e.message}")
        }

        if (items.isEmpty()) {
            _state.value = _state.value.copy(message = "No holdings found in file")
            return
        }
        portfolioRepository.importSnapshot(date, items, exchangeRate)
        _state.value = _state.value.copy(message = "Successfully imported portfolio snapshot")
    }

    private suspend fun exportTransactions(uri: Uri, format: ExportFormat) {
        val expenses = expenseRepository.getAllExpenses().first()
        val content = when (format) {
            ExportFormat.CSV -> CsvExporter.toCsv(expenses)
            ExportFormat.JSON -> json.encodeToString(expenses)
        }

        if (content.isBlank()) throw Exception("Generated content is empty")

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.bufferedWriter().use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
        }
        _state.value = _state.value.copy(message = "Transactions exported successfully")
    }

    private suspend fun exportPortfolio(uri: Uri, format: ExportFormat) {
        val allDates = portfolioRepository.getAllSnapshotDates().first()
        if (allDates.isEmpty()) throw Exception("No portfolio snapshots found")

        var exportDate: Long? = null
        var holdings: List<PortfolioHoldingEntity> = emptyList<PortfolioHoldingEntity>()

        // 1. Try to get holdings for the absolute latest date
        val latestDate = allDates.first()
        holdings = portfolioRepository.getSnapshotByDateSync(latestDate)
        exportDate = latestDate

        // 2. If latest is empty, search for any snapshot that has holdings
        if (holdings.isEmpty()) {
            for (date in allDates) {
                val h = portfolioRepository.getSnapshotByDateSync(date)
                if (h.isNotEmpty()) {
                    holdings = h
                    exportDate = date
                    break
                }
            }
        }

        if (holdings.isEmpty()) {
            throw Exception("Found ${allDates.size} snapshots, but all are empty")
        }

        val finalDate = exportDate ?: latestDate
        val content = when (format) {
            ExportFormat.CSV -> PortfolioCsvExporter.toCsv(finalDate, holdings)
            ExportFormat.JSON -> PortfolioJsonExporter.toSnapshotJson(finalDate, holdings)
        }

        if (content.isBlank()) throw Exception("Generated content is empty")

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.bufferedWriter().use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
        }
        _state.value = _state.value.copy(message = "Exported ${holdings.size} holdings as ${format.name}")
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
