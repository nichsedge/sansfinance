package com.sans.finance.presentation.portfolio

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.presentation.components.GlassCard
import com.sans.finance.presentation.components.PrivacyText
import com.sans.finance.presentation.portfolio.components.AllocationDonutChart
import com.sans.finance.presentation.portfolio.components.NetWorthTrendChart
import com.sans.finance.presentation.portfolio.components.PortfolioHealthView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    onDashboardClick: () -> Unit,
    onForecastingClick: () -> Unit,
    viewModel: PortfolioViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    val snackbarHostState = remember { SnackbarHostState() }
    var editingTarget by remember { mutableStateOf<com.sans.finance.domain.model.AssetClassHealth?>(null) }

    LaunchedEffect(state.importMessage) {
        state.importMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearImportMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (state.snapshotDates.isNotEmpty()) {
                        IconButton(onClick = viewModel::onPreviousSnapshot) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "Previous",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                title = {
                    if (state.snapshotDates.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                state.selectedDate?.let { dateFormat.format(Date(it)) } ?: "",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            IconButton(onClick = viewModel::onNextSnapshot) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Next",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        Text(
                            "Portfolio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Dashboard") },
                            onClick = {
                                showMenu = false
                                onDashboardClick()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Dashboard,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Portfolio") },
                            onClick = { showMenu = false },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.PieChart,
                                    contentDescription = null
                                )
                            }
                        )

                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.snapshotDates.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "No portfolio data",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Import a CSV or JSON snapshot to start",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                PrimaryTabRow(
                    selectedTabIndex = state.selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    Tab(
                        selected = state.selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        text = {
                            Text(
                                "Overview",
                                fontWeight = if (state.selectedTab == 0) FontWeight.Black else FontWeight.Medium
                            )
                        }
                    )
                    Tab(
                        selected = state.selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        text = {
                            Text(
                                "Health",
                                fontWeight = if (state.selectedTab == 1) FontWeight.Black else FontWeight.Medium
                            )
                        }
                    )
                }

                if (state.selectedTab == 0) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            PortfolioHeader(state, onForecastingClick)
                        }

                        if (state.valueHistory.size >= 2) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.extraLarge,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "Net Worth Trend",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        NetWorthTrendChart(
                                            history = state.valueHistory,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp),
                                            isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                                            currencyCode = state.currentCurrency
                                        )
                                    }
                                }
                            }
                        }

                        if (state.categoryTotals.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.extraLarge,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "Asset Allocation",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        AllocationDonutChart(
                                            categories = state.categoryTotals,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }



                        state.holdingsByCategory.forEach { (category, holdings) ->
                            item {
                                val categoryTotal = holdings.sumOf { it.valueIdr }
                                AssetCategoryGroup(
                                    category = category,
                                    total = categoryTotal,
                                    holdings = holdings,
                                    currentCurrency = state.currentCurrency,
                                    isPrivacyModeEnabled = state.isPrivacyModeEnabled
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                } else {
                    PortfolioHealthView(
                        healthList = state.healthList,
                        isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                        onTargetClick = { editingTarget = it },
                        modifier = Modifier
                            .padding(16.dp)
                    )
                }
            }
        }

        editingTarget?.let { target ->
            TargetEditDialog(
                target = target,
                onDismiss = { editingTarget = null },
                onConfirm = { newPercentage ->
                    viewModel.updateTarget(target.assetClass, newPercentage)
                    editingTarget = null
                }
            )
        }
    }
}

@Composable
fun TargetEditDialog(
    target: com.sans.finance.domain.model.AssetClassHealth,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var percentageText by remember { mutableStateOf(target.targetPercentage.toString()) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Target for ${target.assetClass}") },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = percentageText,
                    onValueChange = { percentageText = it },
                    label = { Text("Target Percentage (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                percentageText.toDoubleOrNull()?.let { onConfirm(it) }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}



@Composable
fun AssetCategoryGroup(
    category: String,
    total: Double,
    holdings: List<PortfolioHoldingEntity>,
    currentCurrency: String,
    isPrivacyModeEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    category,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                PrivacyText(
                    amount = (total * 100).toLong(),
                    currencyCode = currentCurrency,
                    isVisible = !isPrivacyModeEnabled,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            holdings.forEachIndexed { index, holding ->
                HoldingItem(holding, isPrivacyModeEnabled, currentCurrency)
                if (index < holdings.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
fun PortfolioHeader(state: PortfolioScreenState, onForecastingClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primary,
        alpha = 0.15f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Total Portfolio Value",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            PrivacyText(
                amount = (state.totalValueIdr * 100).toLong(),
                currencyCode = state.currentCurrency,
                isVisible = !state.isPrivacyModeEnabled,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            state.xirr?.let { xirrValue ->
                Text(
                    text = "XIRR: ${String.format("%.2f%%", xirrValue * 100)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            state.previousTotalIdr?.let { prev ->
                val diff = state.totalValueIdr - prev
                val percent = if (prev != 0.0) (diff / prev) * 100 else 0.0
                val color =
                    if (diff >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(color.copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (diff >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${if (diff >= 0) "+" else ""}${
                            String.format(
                                "%.2f",
                                percent
                            )
                        }% vs last",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onForecastingClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.large,
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("View Wealth Trajectory", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HoldingItem(
    holding: PortfolioHoldingEntity,
    isPrivacyModeEnabled: Boolean,
    currentCurrency: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                holding.asset,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "${holding.source} • ${holding.assetClass}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            PrivacyText(
                amount = (holding.valueIdr * 100).toLong(),
                currencyCode = currentCurrency,
                isVisible = !isPrivacyModeEnabled,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (holding.quantity > 0 && holding.price != null) {
                val quantity = holding.quantity
                val price = holding.price

                val quantityFormatted = when {
                    quantity >= 1_000_000 -> String.format(
                        Locale.US,
                        "%,.2fM",
                        quantity / 1_000_000.0
                    )

                    quantity >= 1_000 -> String.format(Locale.US, "%,.0f", quantity)
                    quantity >= 1 -> String.format(Locale.US, "%,.4f", quantity)
                    else -> String.format(Locale.US, "%.8f", quantity).trimEnd('0').trimEnd('.')
                }

                val displayValue = if (isPrivacyModeEnabled) "••••" else {
                    val priceNonNull = price!!
                    val priceFormatted = when {
                        priceNonNull >= 1_000_000 -> String.format(
                            Locale.US,
                            "%,.1fM",
                            priceNonNull / 1_000_000.0
                        )

                        priceNonNull >= 1_000 -> String.format(Locale.US, "%,.0f", priceNonNull)
                        priceNonNull >= 1 -> String.format(Locale.US, "%,.2f", priceNonNull)
                        else -> String.format(Locale.US, "%,.4f", priceNonNull)
                    }
                    "$quantityFormatted @ $priceFormatted ${holding.currency}"
                }

                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
