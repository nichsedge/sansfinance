package com.sans.finance.presentation.transaction_stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sans.finance.R
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.domain.model.CategorySpent
import com.sans.finance.domain.model.DaySpent
import com.sans.finance.presentation.components.CategoryIcon
import com.sans.finance.presentation.components.ExpenseItem
import com.sans.finance.presentation.components.GlassCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

val pieChartColors = listOf(
    Color(0xFFFF6B6B), // Red/Salmon
    Color(0xFFFF9248), // Orange
    Color(0xFFFFB347), // Yellow/Orange
    Color(0xFFFFD166), // Yellow
    Color(0xFFB5E48C), // Light Green
    Color(0xFF86D97F), // Green
    Color(0xFF52B69A), // Teal/Cyan
    Color(0xFF48CAE4), // Light Blue
    Color(0xFF9D4EDD), // Purple
    Color(0xFFC77DFF), // Light Purple
    Color(0xFFFF9F1C),
    Color(0xFF2EC4B6)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionStatsScreen(
    onBack: () -> Unit,
    onExpenseClick: (Long) -> Unit = {},
    viewModel: TransactionStatsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (state.selectedCategory != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.selectedCategory!!.categoryName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = getPeriodText(state),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(stringResource(R.string.statistics), fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = if (state.selectedCategory != null) {
                            { viewModel.onCategorySelected(null) }
                        } else onBack
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.selectedCategory == null) {
                // Period Type Selector
                PeriodTypeSelector(
                    selectedType = state.selectedPeriodType,
                    onTypeSelected = viewModel::onPeriodTypeSelected
                )

                // Date Navigator
                DateNavigator(
                    state = state,
                    onPrevious = viewModel::onPreviousPeriod,
                    onNext = viewModel::onNextPeriod,
                onDateNavigatorClick = {
                    showDatePicker = true
                }
                )

                // Income / Expense Overview Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatsSimpleCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.income),
                        amount = state.totalIncomeForPeriod,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        isSelected = state.selectedTransactionType == TransactionType.INCOME,
                        onClick = { viewModel.onTransactionTypeSelected(TransactionType.INCOME) },
                        currencyCode = state.currentCurrency
                    )
                    StatsSimpleCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.expenses),
                        amount = state.totalExpenseForPeriod,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        isSelected = state.selectedTransactionType == TransactionType.EXPENSE,
                        onClick = { viewModel.onTransactionTypeSelected(TransactionType.EXPENSE) },
                        currencyCode = state.currentCurrency
                    )
                }

                // Cash Flow and Savings Rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Cash Flow",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                CurrencyFormatter.formatAmount(state.cashFlow, state.currentCurrency),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (state.cashFlow >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Savings Rate",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            com.sans.finance.presentation.components.CircularGauge(
                                progress = state.savingsRate.toFloat(),
                                size = 48.dp,
                                strokeWidth = 6.dp,
                                color = if (state.savingsRate >= 0.2) MaterialTheme.colorScheme.tertiary
                                else if (state.savingsRate >= 0) Color(0xFFFFC107)
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                if (state.isLoading) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Category Breakdown with Pie Chart
                    CategoryBreakdown(
                        categories = state.breakdown,
                        onCategoryClick = viewModel::onCategorySelected,
                        currencyCode = state.currentCurrency
                    )
                }
            } else {
                // Category Detail View
                CategoryDetailView(
                    state = state,
                    onTrendTimeScopeSelected = viewModel::onTrendTimeScopeSelected,
                    onExpenseClick = onExpenseClick,
                    onBack = { viewModel.onCategorySelected(null) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showDatePicker) {
        if (state.selectedPeriodType == TransactionStatsPeriodType.CUSTOM) {
            CustomDateRangePickerDialog(
                onDismiss = { showDatePicker = false },
                onRangeSelected = { start, end ->
                    viewModel.onCustomDateRangeSelected(start, end)
                    showDatePicker = false
                }
            )
        } else {
            com.sans.finance.presentation.components.MonthYearPickerDialog(
                onDismissRequest = { showDatePicker = false },
                onDateSelected = { month, year ->
                    viewModel.onDateSelected(month, year)
                    showDatePicker = false
                },
                initialMonth = state.currentPeriodDate.get(Calendar.MONTH),
                initialYear = state.currentPeriodDate.get(Calendar.YEAR)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodTypeSelector(
    selectedType: TransactionStatsPeriodType,
    onTypeSelected: (TransactionStatsPeriodType) -> Unit
) {
    val types = TransactionStatsPeriodType.entries
    val options = listOf(
        stringResource(R.string.weekly),
        stringResource(R.string.monthly),
        stringResource(R.string.annually),
        stringResource(R.string.custom)
    )

    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        types.forEachIndexed { index, type ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                onClick = { onTypeSelected(type) },
                selected = selectedType == type,
                label = {
                    Text(
                        options[index],
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            )
        }
    }
}

@Composable
fun DateNavigator(
    state: TransactionStatsState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDateNavigatorClick: () -> Unit
) {
    val periodText = getPeriodText(state)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = state.selectedPeriodType != TransactionStatsPeriodType.CUSTOM
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
        }

        Surface(
            onClick = onDateNavigatorClick,
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            enabled = true
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.selectedPeriodType == TransactionStatsPeriodType.CUSTOM) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = periodText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        IconButton(
            onClick = onNext,
            enabled = state.selectedPeriodType != TransactionStatsPeriodType.CUSTOM
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
        }
    }
}

@Composable
fun getPeriodText(state: TransactionStatsState): String {
    val cal = state.currentPeriodDate
    val zone = ZoneId.systemDefault()
    val currentLocale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    return when (state.selectedPeriodType) {
        TransactionStatsPeriodType.WEEKLY -> {
            val start = cal.clone() as Calendar
            start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
            val end = start.clone() as Calendar
            end.add(Calendar.DAY_OF_YEAR, 6)
            val dtf = DateTimeFormatter.ofPattern("dd MMM", currentLocale)
            val startStr = dtf.format(Instant.ofEpochMilli(start.timeInMillis).atZone(zone))
            val endStr = dtf.format(Instant.ofEpochMilli(end.timeInMillis).atZone(zone))
            "$startStr - $endStr"
        }

        TransactionStatsPeriodType.MONTHLY -> {
            val dtf = DateTimeFormatter.ofPattern("MMMM yyyy", currentLocale)
            dtf.format(Instant.ofEpochMilli(cal.timeInMillis).atZone(zone))
        }

        TransactionStatsPeriodType.ANNUALLY -> {
            val dtf = DateTimeFormatter.ofPattern("yyyy", currentLocale)
            dtf.format(Instant.ofEpochMilli(cal.timeInMillis).atZone(zone))
        }

        TransactionStatsPeriodType.CUSTOM -> {
            if (state.customStartDate != null && state.customEndDate != null) {
                val dtf = DateTimeFormatter.ofPattern("dd MMM yyyy", currentLocale)
                val startStr = dtf.format(Instant.ofEpochMilli(state.customStartDate).atZone(zone))
                val endStr = dtf.format(Instant.ofEpochMilli(state.customEndDate).atZone(zone))
                "$startStr - $endStr"
            } else {
                stringResource(R.string.select_date_range)
            }
        }
    }
}

@Composable
fun StatsSimpleCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Long,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    currencyCode: String
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color else color.copy(alpha = 0.3f)
        ),
        shape = MaterialTheme.shapes.large,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, color) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                CurrencyFormatter.formatAmount(amount, currencyCode),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun CategoryBreakdown(
    categories: List<CategorySpent>,
    onCategoryClick: (CategorySpent) -> Unit,
    currencyCode: String
) {
    SectionTitle(stringResource(R.string.by_category), icon = Icons.Default.PieChart)

    val totalInCategories = categories.sumOf { it.totalAmount }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (categories.isNotEmpty() && totalInCategories > 0) {
                PieChartWithLabels(
                    categories = categories,
                    totalAmount = totalInCategories,
                    currencyCode = currencyCode
                )

                val sortedCategories = remember(categories) { categories.sortedByDescending { it.totalAmount } }
                sortedCategories.forEachIndexed { index, category ->
                    val percent =
                        if (totalInCategories > 0) (category.totalAmount.toFloat() / totalInCategories * 100) else 0f
                    val color = pieChartColors[index % pieChartColors.size]

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategoryClick(category) }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(color),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.0f%%", percent),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = category.categoryName,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            CurrencyFormatter.formatAmount(category.totalAmount, currencyCode),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.no_data_available))
                }
            }
        }
    }
}

@Composable
fun CategoryHeroCard(
    category: CategorySpent,
    currencyCode: String,
    periodText: String,
    transactionCount: Int,
    totalExpenseForPeriod: Long
) {
    val avgPerTx = if (transactionCount > 0) category.totalAmount / transactionCount else 0L
    val sharePct = if (totalExpenseForPeriod > 0) {
        (category.totalAmount.toDouble() / totalExpenseForPeriod.toDouble()) * 100.0
    } else 0.0

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    icon = category.categoryIcon,
                    fontSize = 24.sp
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.categoryName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = periodText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = CurrencyFormatter.formatAmount(category.totalAmount, currencyCode),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryStatChip(
                modifier = Modifier.weight(1f),
                label = "Transactions",
                value = "$transactionCount txs"
            )
            CategoryStatChip(
                modifier = Modifier.weight(1f),
                label = "Avg / Tx",
                value = CurrencyFormatter.formatAmountCompact(avgPerTx, currencyCode)
            )
            CategoryStatChip(
                modifier = Modifier.weight(1f),
                label = "Share",
                value = String.format(Locale.US, "%.1f%%", sharePct)
            )
        }
    }
}

@Composable
fun CategoryStatChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CategoryDetailView(
    state: TransactionStatsState,
    onTrendTimeScopeSelected: (TrendTimeScope) -> Unit,
    onExpenseClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    val category = state.selectedCategory ?: return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        CategoryHeroCard(
            category = category,
            currencyCode = state.currentCurrency,
            periodText = getPeriodText(state),
            transactionCount = state.categoryTransactions.size,
            totalExpenseForPeriod = state.totalExpenseForPeriod
        )

        // Spending Trend Chart
        TrendChart(
            trendData = state.categoryTrend,
            period = state.selectedPeriodType,
            timeScope = state.selectedTrendTimeScope,
            onTimeScopeSelected = onTrendTimeScopeSelected,
            currencyCode = state.currentCurrency
        )

        // Transaction Log
        SectionTitle(
            title = "${stringResource(R.string.transactions)} (${state.categoryTransactions.size})",
            icon = Icons.AutoMirrored.Filled.List
        )

        if (state.categoryTransactions.isEmpty()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(24.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.no_data_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 0.dp)
            ) {
                state.categoryTransactions.forEachIndexed { index, transaction ->
                    val accountName = state.accountsMap[transaction.accountId]
                    ExpenseItem(
                        expense = transaction,
                        categoryName = category.categoryName,
                        categoryIcon = category.categoryIcon,
                        accountName = accountName,
                        onClick = { onExpenseClick(transaction.id) }
                    )
                    if (index < state.categoryTransactions.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

enum class ScaleMode {
    ADAPTIVE,
    LINEAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun CustomDateRangePickerDialog(
    onDismiss: () -> Unit,
    onRangeSelected: (Long, Long) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        val start = dateRangePickerState.selectedStartDateMillis
                        val end = dateRangePickerState.selectedEndDateMillis
                        if (start != null && end != null) {
                            onRangeSelected(start, end)
                        }
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }

            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.weight(1f),
                title = {
                    Text(
                        stringResource(R.string.select_date_range),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            )
        }
    }
}
