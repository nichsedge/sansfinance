package com.sans.finance.presentation.transaction_stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.collectAsState
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
    viewModel: TransactionStatsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.statistics), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = if (state.selectedCategory != null) {
                            { viewModel.onCategorySelected(null) }
                        } else onBack) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    onDateClick = {
                        if (state.selectedPeriodType == TransactionStatsPeriodType.CUSTOM) showDatePicker =
                            true
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
                    onBack = { viewModel.onCategorySelected(null) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        CustomDateRangePickerDialog(
            onDismiss = { showDatePicker = false },
            onRangeSelected = { start, end ->
                viewModel.onCustomDateRangeSelected(start, end)
                showDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodTypeSelector(
    selectedType: TransactionStatsPeriodType,
    onTypeSelected: (TransactionStatsPeriodType) -> Unit
) {
    val types = TransactionStatsPeriodType.values()
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
    onDateClick: () -> Unit
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
            onClick = onDateClick,
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            enabled = state.selectedPeriodType == TransactionStatsPeriodType.CUSTOM
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .padding(horizontal = 12.dp, vertical = 8.dp),
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
fun CategoryDetailView(
    state: TransactionStatsState,
    onBack: () -> Unit
) {
    val category = state.selectedCategory ?: return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Detail Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = category.categoryName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = CurrencyFormatter.formatAmount(category.totalAmount, state.currentCurrency),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Category Trend
        TrendChart(
            title = stringResource(R.string.spending_trend),
            trendData = state.categoryTrend,
            period = state.selectedPeriodType,
            currencyCode = state.currentCurrency
        )

        // Transaction Log
        SectionTitle(stringResource(R.string.transactions), icon = Icons.AutoMirrored.Filled.List)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.3f
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (state.categoryTransactions.isEmpty()) {
                    Text(
                        stringResource(R.string.no_data_available),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    state.categoryTransactions.forEachIndexed { index, transaction ->
                        TransactionItem(transaction)
                        if (index < state.categoryTransactions.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Expense) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = DateFormatterUtils.getStandardFormatter().format(Date(transaction.date)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val desc = transaction.details
            if (!desc.isNullOrBlank()) {
                Text(
                    text = desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        Text(
            text = CurrencyFormatter.formatAmount(transaction.amount, transaction.currency),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Black,
            color = if (transaction.type == "INCOME") Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
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
fun TrendChart(
    title: String,
    trendData: List<DaySpent>,
    period: TransactionStatsPeriodType,
    currencyCode: String = "USD"
) {
    SectionTitle(title, icon = Icons.Default.Insights)

    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(220.dp)
        ) {
            if (trendData.isEmpty()) {
                Text(
                    stringResource(R.string.no_data_available),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val sortedSpending = remember(trendData) { trendData.sortedBy { it.day } }
                val dateFormat = remember { DateFormatterUtils.getMonthYearFormatter() }
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                val textMeasurer = rememberTextMeasurer()
                val labelStyle = MaterialTheme.typography.labelSmall.copy(color = onSurfaceColor)

                var selectedIndex by remember { mutableStateOf<Int?>(null) }
                val tooltipStyle = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
                val tooltipDateStyle = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(sortedSpending.size) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val yAxisLabelWidth = textMeasurer.measure(
                                        CurrencyFormatter.formatAmountCompact(
                                            sortedSpending.maxOf { it.amount },
                                            currencyCode
                                        ), style = labelStyle
                                    ).size.width.toFloat() + 16f
                                    val chartLeft = yAxisLabelWidth
                                    val chartRight = size.width
                                    val chartWidth = chartRight - chartLeft
                                    val stepX =
                                        chartWidth / (sortedSpending.size - 1).coerceAtLeast(1)
                                    val index = ((offset.x - chartLeft) / stepX).toInt()
                                        .coerceIn(0, sortedSpending.size - 1)
                                    selectedIndex = index
                                },
                                onDrag = { change, _ ->
                                    val yAxisLabelWidth = textMeasurer.measure(
                                        CurrencyFormatter.formatAmountCompact(
                                            sortedSpending.maxOf { it.amount },
                                            currencyCode
                                        ), style = labelStyle
                                    ).size.width.toFloat() + 16f
                                    val chartLeft = yAxisLabelWidth
                                    val chartRight = size.width
                                    val chartWidth = chartRight - chartLeft
                                    val stepX =
                                        chartWidth / (sortedSpending.size - 1).coerceAtLeast(1)
                                    val index = ((change.position.x - chartLeft) / stepX).toInt()
                                        .coerceIn(0, sortedSpending.size - 1)
                                    selectedIndex = index
                                },
                                onDragEnd = { selectedIndex = null },
                                onDragCancel = { selectedIndex = null }
                            )
                        }
                        .pointerInput(sortedSpending.size) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val yAxisLabelWidth = textMeasurer.measure(
                                        CurrencyFormatter.formatAmountCompact(
                                            sortedSpending.maxOf { it.amount },
                                            currencyCode
                                        ), style = labelStyle
                                    ).size.width.toFloat() + 16f
                                    val chartLeft = yAxisLabelWidth
                                    val chartRight = size.width
                                    val chartWidth = chartRight - chartLeft
                                    val stepX =
                                        chartWidth / (sortedSpending.size - 1).coerceAtLeast(1)
                                    val index = ((offset.x - chartLeft) / stepX).toInt()
                                        .coerceIn(0, sortedSpending.size - 1)
                                    selectedIndex = index
                                    tryAwaitRelease()
                                    selectedIndex = null
                                }
                            )
                        }
                ) {
                    val maxAmount = sortedSpending.maxOfOrNull { it.amount } ?: 1L
                    val minAmount = 0L
                    val amountRange = (maxAmount - minAmount).coerceAtLeast(1L)

                    val textLayoutResults = sortedSpending.map {
                        textMeasurer.measure(dateFormat.format(Date(it.day)), style = labelStyle)
                    }
                    val bottomPadding =
                        textLayoutResults.maxOfOrNull { it.size.height }?.toFloat() ?: 40f
                    val yAxisLabels = 5
                    val yAxisLabelWidth = textMeasurer.measure(
                        CurrencyFormatter.formatAmountCompact(maxAmount, currencyCode),
                        style = labelStyle
                    ).size.width.toFloat() + 16f

                    val chartLeft = yAxisLabelWidth
                    val chartRight = size.width
                    val chartTop = 16f
                    val chartBottom = size.height - bottomPadding - 16f

                    val chartWidth = chartRight - chartLeft
                    val chartHeight = chartBottom - chartTop

                    // Grid and Y-axis
                    for (i in 0 until yAxisLabels) {
                        val fraction = i.toFloat() / (yAxisLabels - 1)
                        val y = chartBottom - (fraction * chartHeight)
                        val value = minAmount + (amountRange * fraction).toLong()

                        drawLine(
                            color = gridColor,
                            start = Offset(chartLeft, y),
                            end = Offset(chartRight, y),
                            strokeWidth = 1f
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

                    // Curve
                    if (sortedSpending.size > 1) {
                        val path = Path()
                        val points = mutableListOf<Offset>()
                        val stepX = chartWidth / (sortedSpending.size - 1).coerceAtLeast(1)

                        sortedSpending.forEachIndexed { index, data ->
                            val x = chartLeft + index * stepX
                            val fractionY = (data.amount - minAmount).toFloat() / amountRange
                            val y = chartBottom - (fractionY * chartHeight)
                            points.add(Offset(x, y))
                        }

                        path.moveTo(points.first().x, points.first().y)
                        for (i in 0 until points.size - 1) {
                            val p1 = points[i]
                            val p2 = points[i + 1]
                            val controlPoint1 = Offset(p1.x + (p2.x - p1.x) / 2f, p1.y)
                            val controlPoint2 = Offset(p1.x + (p2.x - p1.x) / 2f, p2.y)
                            path.cubicTo(
                                controlPoint1.x,
                                controlPoint1.y,
                                controlPoint2.x,
                                controlPoint2.y,
                                p2.x,
                                p2.y
                            )
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
                                    primaryColor.copy(alpha = 0.3f),
                                    Color.Transparent
                                ), startY = chartTop, endY = chartBottom
                            )
                        )
                        drawPath(path = path, color = primaryColor, style = Stroke(width = 6f))

                        // Tooltip
                        selectedIndex?.let { index ->
                            val point = points[index]
                            val data = sortedSpending[index]

                            drawLine(
                                color = primaryColor.copy(alpha = 0.5f),
                                start = Offset(point.x, chartTop),
                                end = Offset(point.x, chartBottom),
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )

                            drawCircle(color = primaryColor, radius = 12f, center = point)
                            drawCircle(color = Color.White, radius = 6f, center = point)

                            val valStr =
                                CurrencyFormatter.formatAmountCompact(data.amount, currencyCode)
                            val dateStr = SimpleDateFormat(
                                "dd MMM",
                                Locale.getDefault()
                            ).format(Date(data.day))

                            val valLayout = textMeasurer.measure(valStr, tooltipStyle)
                            val dateLayout = textMeasurer.measure(dateStr, tooltipDateStyle)

                            val tWidth = maxOf(valLayout.size.width, dateLayout.size.width) + 48f
                            val tHeight = valLayout.size.height + dateLayout.size.height + 32f

                            var tX = point.x - tWidth / 2f
                            if (tX < chartLeft) tX = chartLeft + 16f
                            if (tX + tWidth > chartRight) tX = chartRight - tWidth - 16f

                            val tY = (point.y - tHeight - 32f).coerceAtLeast(chartTop + 16f)

                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(tX, tY),
                                size = Size(tWidth, tHeight),
                                cornerRadius = CornerRadius(16f, 16f)
                            )

                            drawText(
                                textLayoutResult = dateLayout,
                                topLeft = Offset(tX + 24f, tY + 16f)
                            )
                            drawText(
                                textLayoutResult = valLayout,
                                topLeft = Offset(tX + 24f, tY + 16f + dateLayout.size.height)
                            )
                        }

                        // X-axis labels
                        val labelsToDraw = Math.min(sortedSpending.size, 5)
                        if (labelsToDraw > 0) {
                            val step = Math.max(
                                1,
                                (sortedSpending.size - 1) / (labelsToDraw - 1).coerceAtLeast(1)
                            )
                            for (i in sortedSpending.indices step step) {
                                val x = chartLeft + i * stepX
                                val textLayoutResult = textLayoutResults[i]
                                drawText(
                                    textLayoutResult = textLayoutResult,
                                    topLeft = Offset(
                                        x - textLayoutResult.size.width / 2f,
                                        chartBottom + 8f
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
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
