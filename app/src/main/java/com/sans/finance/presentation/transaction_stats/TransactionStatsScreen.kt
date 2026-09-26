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
import androidx.compose.material.icons.filled.Insights
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import com.sans.finance.presentation.components.CategoryIcon
import com.sans.finance.presentation.components.ExpenseItem
import com.sans.finance.presentation.components.GlassCard
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
import com.sans.finance.core.util.DateFormatterUtils
import com.sans.finance.domain.model.CategorySpent
import com.sans.finance.domain.model.DaySpent
import com.sans.finance.domain.model.Expense
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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
    return when (state.selectedPeriodType) {
        TransactionStatsPeriodType.WEEKLY -> {
            val start = cal.clone() as Calendar
            start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
            val end = start.clone() as Calendar
            end.add(Calendar.DAY_OF_YEAR, 6)
            val df = SimpleDateFormat("dd MMM", Locale.getDefault())
            "${df.format(start.time)} - ${df.format(end.time)}"
        }

        TransactionStatsPeriodType.MONTHLY -> {
            val df = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            df.format(cal.time)
        }

        TransactionStatsPeriodType.ANNUALLY -> {
            val df = SimpleDateFormat("yyyy", Locale.getDefault())
            df.format(cal.time)
        }

        TransactionStatsPeriodType.CUSTOM -> {
            if (state.customStartDate != null && state.customEndDate != null) {
                val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                "${df.format(Date(state.customStartDate))} - ${df.format(Date(state.customEndDate))}"
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

                categories.sortedByDescending { it.totalAmount }.forEachIndexed { index, category ->
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
fun TrendChart(
    trendData: List<DaySpent>,
    period: TransactionStatsPeriodType,
    timeScope: TrendTimeScope,
    onTimeScopeSelected: (TrendTimeScope) -> Unit,
    currencyCode: String = "USD"
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val haptic = LocalHapticFeedback.current

    val sortedSpending = remember(trendData) { trendData.sortedBy { it.day } }
    val amounts = remember(sortedSpending) { sortedSpending.map { it.amount } }
    val nonZeroAmounts = remember(amounts) { amounts.filter { it > 0 }.sorted() }
    val totalAmount = remember(amounts) { amounts.sum() }
    val maxAmount = remember(amounts) { amounts.maxOrNull() ?: 0L }
    val medianAmount = remember(nonZeroAmounts) {
        if (nonZeroAmounts.isEmpty()) 0L
        else nonZeroAmounts[nonZeroAmounts.size / 2]
    }
    val avgAmount = remember(sortedSpending, totalAmount) {
        if (sortedSpending.isNotEmpty()) totalAmount / sortedSpending.size else 0L
    }

    val isAnomalyDetected = remember(maxAmount, medianAmount, sortedSpending.size) {
        sortedSpending.size >= 3 && medianAmount > 0 && maxAmount >= 3 * medianAmount
    }
    val outlierThreshold = remember(medianAmount, isAnomalyDetected) {
        if (isAnomalyDetected) (medianAmount * 3).coerceAtLeast(1L) else Long.MAX_VALUE
    }

    var scaleMode by remember(isAnomalyDetected) {
        mutableStateOf(if (isAnomalyDetected) ScaleMode.ADAPTIVE else ScaleMode.ADAPTIVE)
    }

    val isDaily = remember(timeScope, period) {
        timeScope == TrendTimeScope.IN_PERIOD && period != TransactionStatsPeriodType.ANNUALLY
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Header Row: Title & Scale Mode Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.spending_trend).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    scaleMode = if (scaleMode == ScaleMode.ADAPTIVE) ScaleMode.LINEAR else ScaleMode.ADAPTIVE
                },
                shape = RoundedCornerShape(12.dp),
                color = if (scaleMode == ScaleMode.ADAPTIVE) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (scaleMode == ScaleMode.ADAPTIVE) Icons.Default.Bolt else Icons.Default.Timeline,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (scaleMode == ScaleMode.ADAPTIVE) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (scaleMode == ScaleMode.ADAPTIVE) "Adaptive" else "Linear",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (scaleMode == ScaleMode.ADAPTIVE) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Time Range Filter Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf(
                TrendTimeScope.IN_PERIOD to "Period",
                TrendTimeScope.LAST_6_MONTHS to "6M",
                TrendTimeScope.LAST_12_MONTHS to "1Y",
                TrendTimeScope.ALL_TIME to "All"
            )
            tabs.forEach { (scope, label) ->
                val isSelected = timeScope == scope
                FilterChip(
                    selected = isSelected,
                    onClick = { onTimeScopeSelected(scope) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.height(30.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Anomaly Banner (if detected)
        if (isAnomalyDetected) {
            Spacer(modifier = Modifier.height(8.dp))
            val outlier = sortedSpending.maxByOrNull { it.amount }
            val outlierDateStr = if (outlier != null) {
                if (isDaily) SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(outlier.day))
                else SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(outlier.day))
            } else ""

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFB74D).copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFFFB74D),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Fat-tail spike on $outlierDateStr (${CurrencyFormatter.formatAmountCompact(maxAmount, currencyCode)}): scale dynamically compressed to keep regular spending patterns visible.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chart Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            if (sortedSpending.isEmpty()) {
                Text(
                    stringResource(R.string.no_data_available),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val textMeasurer = rememberTextMeasurer()
                val labelStyle = MaterialTheme.typography.labelSmall.copy(
                    color = onSurfaceColor.copy(alpha = 0.65f),
                    fontSize = 10.sp
                )
                var selectedIndex by remember { mutableStateOf<Int?>(null) }

                val tooltipTitleStyle = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                val tooltipValueStyle = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black
                )
                val tooltipBadgeStyle = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFFFFB74D),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )

                val c = maxOf(1.0, medianAmount.toDouble() / 2.0)
                val fMax = kotlin.math.ln(1.0 + maxAmount.toDouble() / c)

                fun normY(amt: Long): Float {
                    if (maxAmount <= 0L) return 0f
                    return when (scaleMode) {
                        ScaleMode.LINEAR -> (amt.toDouble() / maxAmount.toDouble()).toFloat().coerceIn(0f, 1f)
                        ScaleMode.ADAPTIVE -> {
                            if (fMax <= 0.0) 0f
                            else (kotlin.math.ln(1.0 + amt.toDouble() / c) / fMax).toFloat().coerceIn(0f, 1f)
                        }
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(sortedSpending.size) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val yAxisLabelWidth = textMeasurer.measure(
                                        CurrencyFormatter.formatAmountCompact(maxAmount, currencyCode),
                                        style = labelStyle
                                    ).size.width.toFloat() + 16f
                                    val chartLeft = yAxisLabelWidth
                                    val chartWidth = size.width - chartLeft
                                    val stepX = chartWidth / (sortedSpending.size - 1).coerceAtLeast(1)
                                    val idx = ((offset.x - chartLeft + stepX / 2f) / stepX).toInt()
                                        .coerceIn(0, sortedSpending.size - 1)
                                    if (selectedIndex != idx) {
                                        selectedIndex = idx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                onDrag = { change, _ ->
                                    val yAxisLabelWidth = textMeasurer.measure(
                                        CurrencyFormatter.formatAmountCompact(maxAmount, currencyCode),
                                        style = labelStyle
                                    ).size.width.toFloat() + 16f
                                    val chartLeft = yAxisLabelWidth
                                    val chartWidth = size.width - chartLeft
                                    val stepX = chartWidth / (sortedSpending.size - 1).coerceAtLeast(1)
                                    val idx = ((change.position.x - chartLeft + stepX / 2f) / stepX).toInt()
                                        .coerceIn(0, sortedSpending.size - 1)
                                    if (selectedIndex != idx) {
                                        selectedIndex = idx
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                onDragEnd = { selectedIndex = null },
                                onDragCancel = { selectedIndex = null }
                            )
                        }
                        .pointerInput(sortedSpending.size) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val yAxisLabelWidth = textMeasurer.measure(
                                        CurrencyFormatter.formatAmountCompact(maxAmount, currencyCode),
                                        style = labelStyle
                                    ).size.width.toFloat() + 16f
                                    val chartLeft = yAxisLabelWidth
                                    val chartWidth = size.width - chartLeft
                                    val stepX = chartWidth / (sortedSpending.size - 1).coerceAtLeast(1)
                                    val idx = ((offset.x - chartLeft + stepX / 2f) / stepX).toInt()
                                        .coerceIn(0, sortedSpending.size - 1)
                                    selectedIndex = idx
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    tryAwaitRelease()
                                    selectedIndex = null
                                }
                            )
                        }
                ) {
                    val yAxisLabelText = CurrencyFormatter.formatAmountCompact(maxAmount, currencyCode)
                    val yAxisLabelWidth = textMeasurer.measure(yAxisLabelText, style = labelStyle).size.width.toFloat() + 16f
                    val bottomPadding = 32f

                    val chartLeft = yAxisLabelWidth
                    val chartRight = size.width
                    val chartTop = 16f
                    val chartBottom = size.height - bottomPadding

                    val chartWidth = chartRight - chartLeft
                    val chartHeight = chartBottom - chartTop

                    // Grid and Y-axis labels
                    val yValues = when (scaleMode) {
                        ScaleMode.LINEAR -> {
                            listOf(0L, (maxAmount * 0.33f).toLong(), (maxAmount * 0.66f).toLong(), maxAmount)
                        }
                        ScaleMode.ADAPTIVE -> {
                            val v1 = if (fMax > 0) (c * (kotlin.math.exp(0.33 * fMax) - 1.0)).toLong() else 0L
                            val v2 = if (fMax > 0) (c * (kotlin.math.exp(0.66 * fMax) - 1.0)).toLong() else 0L
                            listOf(0L, v1, v2, maxAmount).distinct().sorted()
                        }
                    }

                    yValues.forEach { value ->
                        val fractionY = normY(value)
                        val y = chartBottom - (fractionY * chartHeight)

                        drawLine(
                            color = gridColor,
                            start = Offset(chartLeft, y),
                            end = Offset(chartRight, y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )

                        val textLayoutResult = textMeasurer.measure(
                            CurrencyFormatter.formatAmountCompact(value, currencyCode),
                            style = labelStyle
                        )
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(
                                chartLeft - textLayoutResult.size.width - 8f,
                                y - textLayoutResult.size.height / 2f
                            )
                        )
                    }

                    // Curve & Area Fill
                    if (sortedSpending.isNotEmpty()) {
                        val points = mutableListOf<Offset>()
                        val stepX = if (sortedSpending.size > 1) {
                            chartWidth / (sortedSpending.size - 1)
                        } else {
                            chartWidth / 2f
                        }

                        sortedSpending.forEachIndexed { index, data ->
                            val x = if (sortedSpending.size == 1) chartLeft + chartWidth / 2f else chartLeft + index * stepX
                            val fractionY = normY(data.amount)
                            val y = chartBottom - (fractionY * chartHeight)
                            points.add(Offset(x, y))
                        }

                        val path = Path()
                        path.moveTo(points.first().x, points.first().y)

                        if (points.size > 1) {
                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                val dx = (p2.x - p1.x) * 0.45f
                                val cp1 = Offset(p1.x + dx, p1.y)
                                val cp2 = Offset(p2.x - dx, p2.y)
                                path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
                            }
                        }

                        val fillPath = Path().apply {
                            addPath(path)
                            lineTo(points.last().x, chartBottom)
                            lineTo(points.first().x, chartBottom)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.35f),
                                    primaryColor.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                startY = chartTop,
                                endY = chartBottom
                            )
                        )

                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = Stroke(width = 5f, cap = StrokeCap.Round)
                        )

                        // Outlier Beacon Pins
                        sortedSpending.forEachIndexed { index, data ->
                            if (data.amount >= outlierThreshold && isAnomalyDetected) {
                                val pt = points[index]
                                drawCircle(
                                    color = Color(0xFFFFB74D).copy(alpha = 0.25f),
                                    radius = 18f,
                                    center = pt
                                )
                                drawCircle(
                                    color = Color(0xFFFFB74D),
                                    radius = 8f,
                                    center = pt,
                                    style = Stroke(width = 3f)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 4f,
                                    center = pt
                                )
                            }
                        }

                        // Tooltip when scrubbing
                        selectedIndex?.let { idx ->
                            val pt = points[idx]
                            val data = sortedSpending[idx]
                            val isOutlier = data.amount >= outlierThreshold && isAnomalyDetected

                            // Dashed guide line
                            drawLine(
                                color = primaryColor.copy(alpha = 0.6f),
                                start = Offset(pt.x, chartTop),
                                end = Offset(pt.x, chartBottom),
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )

                            // Glowing point dot
                            drawCircle(color = primaryColor.copy(alpha = 0.3f), radius = 14f, center = pt)
                            drawCircle(color = primaryColor, radius = 7f, center = pt)
                            drawCircle(color = Color.White, radius = 3.5f, center = pt)

                            // Formatted strings
                            val dateStr = if (isDaily) {
                                SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(data.day))
                            } else {
                                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(data.day))
                            }
                            val amtStr = CurrencyFormatter.formatAmount(data.amount, currencyCode)
                            val badgeStr = if (isOutlier && medianAmount > 0) {
                                "⚡ Spike (${String.format(Locale.US, "%.0f", data.amount.toDouble() / medianAmount)}× median)"
                            } else null

                            val dateLayout = textMeasurer.measure(dateStr, tooltipTitleStyle)
                            val amtLayout = textMeasurer.measure(amtStr, tooltipValueStyle)
                            val badgeLayout = badgeStr?.let { textMeasurer.measure(it, tooltipBadgeStyle) }

                            val contentWidth = maxOf(
                                dateLayout.size.width,
                                amtLayout.size.width,
                                badgeLayout?.size?.width ?: 0
                            ).toFloat()
                            val contentHeight = dateLayout.size.height + amtLayout.size.height +
                                    (badgeLayout?.let { it.size.height + 4 } ?: 0)

                            val tWidth = contentWidth + 28f
                            val tHeight = contentHeight + 20f

                            var tX = pt.x - tWidth / 2f
                            if (tX < chartLeft) tX = chartLeft + 8f
                            if (tX + tWidth > chartRight) tX = chartRight - tWidth - 8f

                            var tY = pt.y - tHeight - 20f
                            if (tY < chartTop) tY = pt.y + 20f

                            // Draw Tooltip Container
                            drawRoundRect(
                                color = surfaceVariantColor,
                                topLeft = Offset(tX, tY),
                                size = Size(tWidth, tHeight),
                                cornerRadius = CornerRadius(12f, 12f)
                            )
                            drawRoundRect(
                                color = primaryColor.copy(alpha = 0.4f),
                                topLeft = Offset(tX, tY),
                                size = Size(tWidth, tHeight),
                                cornerRadius = CornerRadius(12f, 12f),
                                style = Stroke(width = 1.5f)
                            )

                            var currentY = tY + 10f
                            drawText(
                                textLayoutResult = dateLayout,
                                topLeft = Offset(tX + 14f, currentY)
                            )
                            currentY += dateLayout.size.height + 2f
                            drawText(
                                textLayoutResult = amtLayout,
                                topLeft = Offset(tX + 14f, currentY)
                            )
                            if (badgeLayout != null) {
                                currentY += amtLayout.size.height + 2f
                                drawText(
                                    textLayoutResult = badgeLayout,
                                    topLeft = Offset(tX + 14f, currentY)
                                )
                            }
                        }

                        // X-axis date labels
                        val labelStep = when {
                            sortedSpending.size <= 7 -> 1
                            sortedSpending.size <= 14 -> 2
                            sortedSpending.size <= 24 -> 4
                            else -> (sortedSpending.size / 5).coerceAtLeast(1)
                        }

                        for (i in sortedSpending.indices step labelStep) {
                            val pt = points[i]
                            val data = sortedSpending[i]
                            val labelText = if (isDaily) {
                                SimpleDateFormat("dd", Locale.getDefault()).format(Date(data.day))
                            } else {
                                SimpleDateFormat("MMM", Locale.getDefault()).format(Date(data.day))
                            }
                            val textLayoutResult = textMeasurer.measure(labelText, style = labelStyle)
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(
                                    pt.x - textLayoutResult.size.width / 2f,
                                    chartBottom + 6f
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mini Metrics Strip
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TrendMetricItem(
                label = "Total",
                value = CurrencyFormatter.formatAmountCompact(totalAmount, currencyCode)
            )
            TrendMetricItem(
                label = if (isDaily) "Daily Avg" else "Monthly Avg",
                value = CurrencyFormatter.formatAmountCompact(avgAmount, currencyCode)
            )
            TrendMetricItem(
                label = "Median",
                value = CurrencyFormatter.formatAmountCompact(medianAmount, currencyCode)
            )
            TrendMetricItem(
                label = "Peak",
                value = CurrencyFormatter.formatAmountCompact(maxAmount, currencyCode)
            )
        }
    }
}

@Composable
fun TrendMetricItem(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SectionTitle(title: String, icon: ImageVector? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 1.5.sp
        )
    }
}


@Composable
fun PieChartWithLabels(
    categories: List<CategorySpent>,
    totalAmount: Long,
    currencyCode: String
) {
    if (categories.isEmpty() || totalAmount == 0L) return

    val textMeasurer = rememberTextMeasurer()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val haptic = LocalHapticFeedback.current
    var selectedIndex by remember { mutableStateOf(-1) }

    val sortedCategories = remember(categories) {
        categories.sortedByDescending { it.totalAmount }.take(12)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(sortedCategories) {
                    detectTapGestures { offset ->
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
                        val radius = Math.min(canvasWidth, canvasHeight) / 3f

                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val distance = Math.sqrt((dx * dx + dy * dy).toDouble())

                        if (distance <= radius && distance >= radius * 0.5f) {
                            var angle =
                                Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f

                            // Adjust for -90 start angle
                            val adjustedAngle = (angle + 90f) % 360f

                            var currentStartAngle = 0f
                            sortedCategories.forEachIndexed { index, category ->
                                val sweep = (category.totalAmount.toFloat() / totalAmount) * 360f
                                if (adjustedAngle >= currentStartAngle && adjustedAngle <= currentStartAngle + sweep) {
                                    if (selectedIndex != index) {
                                        selectedIndex = index
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } else {
                                        selectedIndex = -1
                                    }
                                    return@detectTapGestures
                                }
                                currentStartAngle += sweep
                            }
                        } else {
                            selectedIndex = -1
                        }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val radius = Math.min(canvasWidth, canvasHeight) / 3f
            val center = Offset(canvasWidth / 2, canvasHeight / 2)

            var startAngle = -90f

            sortedCategories.forEachIndexed { index, category ->
                val sweepAngle = (category.totalAmount.toFloat() / totalAmount) * 360f
                val color = pieChartColors[index % pieChartColors.size]
                val isSelected = selectedIndex == index

                // Draw pie slice (Donut style)
                val slicePath = Path().apply {
                    moveTo(center.x, center.y)
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            center.x - radius,
                            center.y - radius,
                            center.x + radius,
                            center.y + radius
                        ),
                        startAngleDegrees = startAngle,
                        sweepAngleDegrees = sweepAngle,
                        forceMoveTo = false
                    )
                    close()
                }

                val innerRadius = radius * 0.6f
                val innerPath = Path().apply {
                    addOval(
                        androidx.compose.ui.geometry.Rect(
                            center.x - innerRadius,
                            center.y - innerRadius,
                            center.x + innerRadius,
                            center.y + innerRadius
                        )
                    )
                }

                clipPath(innerPath, clipOp = androidx.compose.ui.graphics.ClipOp.Difference) {
                    drawPath(
                        path = slicePath,
                        color = if (isSelected) color else color.copy(alpha = 0.8f)
                    )
                    if (isSelected) {
                        drawPath(
                            path = slicePath,
                            color = Color.White.copy(alpha = 0.3f),
                            style = Stroke(width = 4f)
                        )
                    }
                }

                // Only draw labels for slices > 3% to avoid clutter
                if (sweepAngle > 10f) {
                    // Calculate label position
                    val angleInRadians = (startAngle + sweepAngle / 2) * (Math.PI / 180f)
                    val lineStart = Offset(
                        x = center.x + (radius * 0.9f) * cos(angleInRadians).toFloat(),
                        y = center.y + (radius * 0.9f) * sin(angleInRadians).toFloat()
                    )
                    val lineEnd = Offset(
                        x = center.x + (radius * 1.2f) * cos(angleInRadians).toFloat(),
                        y = center.y + (radius * 1.2f) * sin(angleInRadians).toFloat()
                    )
                    val isRightSide = cos(angleInRadians) > 0
                    val textEnd = Offset(
                        x = lineEnd.x + (if (isRightSide) 20f else -20f),
                        y = lineEnd.y
                    )

                    // Draw connecting line
                    val path = Path().apply {
                        moveTo(lineStart.x, lineStart.y)
                        lineTo(lineEnd.x, lineEnd.y)
                        lineTo(textEnd.x, textEnd.y)
                    }
                    drawPath(
                        path = path,
                        color = color.copy(alpha = 0.5f),
                        style = Stroke(width = 2f)
                    )

                    // Draw text label
                    val label = String.format(Locale.US, "%.0f%%", (sweepAngle / 360f) * 100)
                    val textLayoutResult = textMeasurer.measure(
                        text = label,
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceColor
                        )
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x = textEnd.x + (if (isRightSide) 4f else -textLayoutResult.size.width - 4f),
                            y = textEnd.y - textLayoutResult.size.height / 2f
                        )
                    )
                }

                startAngle += sweepAngle
            }
        }

        // Center Info
        if (selectedIndex != -1) {
            val selected = sortedCategories[selectedIndex]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    selected.categoryName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    CurrencyFormatter.formatAmount(selected.totalAmount, currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    String.format(
                        Locale.US,
                        "%.1f%%",
                        (selected.totalAmount.toFloat() / totalAmount) * 100
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = pieChartColors[selectedIndex % pieChartColors.size],
                    fontWeight = FontWeight.ExtraBold
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "TOTAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    CurrencyFormatter.formatAmount(totalAmount, currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
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
