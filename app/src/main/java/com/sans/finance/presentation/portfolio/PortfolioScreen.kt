package com.sans.finance.presentation.portfolio

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    onBack: () -> Unit = {},
    viewModel: PortfolioViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

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
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Text(
                        "Portfolio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sync from Cloud") },
                                onClick = {
                                    showMenu = false
                                    viewModel.syncFromGcs()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Sync, contentDescription = null)
                                }
                            )
                            if (state.snapshotDates.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Analyze with AI") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.analyzePortfolioWithAi()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Prune Monthly (Keep Latest/Mo)") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.pruneMonthlySnapshots()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.PieChart, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.snapshotDates.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                            "Sync snapshots from Cloud Storage to start tracking",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.syncFromGcs() },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sync from Cloud", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    PrimaryTabRow(
                        selectedTabIndex = state.selectedTab,
                        containerColor = Color.Transparent,
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
                            if (state.snapshotDates.size > 1) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = viewModel::onPreviousSnapshot,
                                            modifier = Modifier.size(36.dp),
                                            enabled = state.selectedDateIndex < state.snapshotDates.size - 1
                                        ) {
                                            Icon(
                                                Icons.Default.ChevronLeft,
                                                contentDescription = "Previous Snapshot",
                                                tint = if (state.selectedDateIndex < state.snapshotDates.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                            )
                                        }

                                        Card(
                                            shape = MaterialTheme.shapes.extraLarge,
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                            )
                                        ) {
                                            Text(
                                                text = state.selectedDate?.let { dateFormat.format(Date(it)) } ?: "",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = viewModel::onNextSnapshot,
                                            modifier = Modifier.size(36.dp),
                                            enabled = state.selectedDateIndex > 0
                                        ) {
                                            Icon(
                                                Icons.Default.ChevronRight,
                                                contentDescription = "Next Snapshot",
                                                tint = if (state.selectedDateIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                PortfolioHeader(state, onForecastingClick)
                            }

                            if (state.valueHistory.size >= 2 || state.netWorthHistory.size >= 2) {
                                item {
                                    val activeHistory = if (state.chartMode == 0 && state.netWorthHistory.isNotEmpty()) state.netWorthHistory else state.valueHistory
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.extraLarge,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        if (state.chartMode == 0) "Net Worth Trend" else "Investments Trend",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Black,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        if (state.chartMode == 0) "Includes liquid cash & accounts" else "Market holdings only",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }

                                                Row(
                                                    modifier = Modifier
                                                        .background(
                                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                                            CircleShape
                                                        )
                                                        .padding(3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (state.chartMode == 0) MaterialTheme.colorScheme.primary else Color.Transparent
                                                            )
                                                            .clickable { viewModel.setChartMode(0) }
                                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            "Total",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = if (state.chartMode == 0) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (state.chartMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (state.chartMode == 1) MaterialTheme.colorScheme.primary else Color.Transparent
                                                            )
                                                            .clickable { viewModel.setChartMode(1) }
                                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            "Investments",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = if (state.chartMode == 1) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (state.chartMode == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.height(16.dp))
                                            NetWorthTrendChart(
                                                history = activeHistory,
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
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                "Asset Allocation",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurface
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
                                        isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                                        accountAliases = state.accountAliases
                                    )
                                }
                            }

                            item { Spacer(modifier = Modifier.height(32.dp)) }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            state.aiAnalysis?.let { analysis ->
                                item {
                                    PortfolioAiInsightCard(
                                        analysis = analysis,
                                        onClear = viewModel::clearAiAnalysis
                                    )
                                }
                            }

                            item {
                                PortfolioHealthView(
                                    healthList = state.healthList,
                                    rebalanceSuggestions = state.rebalanceSuggestions,
                                    isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                                    currentCurrency = state.currentCurrency,
                                    onTargetClick = { editingTarget = it }
                                )
                            }
                        }
                    }
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
        title = { Text("Edit Target for ${target.assetClass}", fontWeight = FontWeight.Black) },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = percentageText,
                    onValueChange = { percentageText = it },
                    label = { Text("Target Percentage (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                percentageText.toDoubleOrNull()?.let { onConfirm(it) }
            }, shape = MaterialTheme.shapes.large) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}

@Composable
fun AssetCategoryGroup(
    category: String,
    total: Double,
    holdings: List<PortfolioHoldingEntity>,
    currentCurrency: String,
    isPrivacyModeEnabled: Boolean,
    accountAliases: Map<String, String>
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
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
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
                HoldingItem(
                    holding = holding,
                    isPrivacyModeEnabled = isPrivacyModeEnabled,
                    currentCurrency = currentCurrency,
                    accountAliases = accountAliases
                )
                if (index < holdings.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
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
        alpha = 0.12f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Consolidated Portfolio Value",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            PrivacyText(
                amount = (state.totalValueIdr * 100).toLong(),
                currencyCode = state.currentCurrency,
                isVisible = !state.isPrivacyModeEnabled,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (state.includedAccountCashIdr > 0.0) {
                Text(
                    "Includes account cash",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            state.xirr?.let { xirrValue ->
                Text(
                    text = "ANNUALIZED RETURN (XIRR): ${String.format("%.2f%%", xirrValue * 100)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 4.dp),
                    letterSpacing = 0.5.sp
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
                        .padding(top = 10.dp)
                        .background(color.copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
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
                        }% vs last snapshot",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
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
    currentCurrency: String,
    accountAliases: Map<String, String>
) {
    val displayAccountName = accountAliases[holding.accountKey]
        ?: holding.accountName?.takeIf { it.isNotBlank() }
        ?: holding.account

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
            Text(
                displayAccountName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
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
@Composable
fun PortfolioAiInsightCard(
    analysis: com.sans.finance.data.ai.PortfolioAnalysisResult,
    onClear: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        alpha = 0.15f
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "AI STRATEGIST",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }
                IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                analysis.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(16.dp))
            analysis.insights.forEach { insight ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val dotColor = when (insight.importance) {
                                "HIGH" -> MaterialTheme.colorScheme.error
                                "MEDIUM" -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.secondary
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(dotColor, CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                insight.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            insight.observation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                insight.suggestion,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
