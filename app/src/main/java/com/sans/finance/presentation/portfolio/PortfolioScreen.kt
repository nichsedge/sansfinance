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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.sans.finance.presentation.ai.components.MarkdownContent
import com.sans.finance.presentation.components.AppTopBar
import com.sans.finance.presentation.components.GlassCard
import com.sans.finance.presentation.components.PrivacyText
import com.sans.finance.presentation.portfolio.components.AllocationDonutChart
import com.sans.finance.presentation.portfolio.components.EnhancedHoldingItem
import com.sans.finance.presentation.portfolio.components.ExpandableCategoryGroup
import com.sans.finance.presentation.portfolio.components.HoldingDetailBottomSheet
import com.sans.finance.presentation.portfolio.components.NetWorthTrendChart
import com.sans.finance.presentation.portfolio.components.PortfolioFilterBar
import com.sans.finance.presentation.portfolio.components.PortfolioGuideBottomSheet
import com.sans.finance.presentation.portfolio.components.PortfolioHealthView
import com.sans.finance.presentation.portfolio.components.PortfolioSortOption
import com.sans.finance.presentation.portfolio.components.PortfolioViewMode
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
    val sovereignAdvisor by viewModel.sovereignAdvisor.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val snackbarHostState = remember { SnackbarHostState() }
    var showGuideSheet by rememberSaveable { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importFile(it) }
    }

    var editingTarget by remember { mutableStateOf<com.sans.finance.domain.model.AssetClassHealth?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedSource by rememberSaveable { mutableStateOf<String?>(null) }
    var sortOption by rememberSaveable { mutableStateOf(PortfolioSortOption.VALUE_DESC) }
    var viewMode by rememberSaveable { mutableStateOf(PortfolioViewMode.GROUPED) }
    var collapsedCategories by rememberSaveable { mutableStateOf(setOf<String>()) }
    var selectedHoldingForDetail by remember {
        mutableStateOf<Pair<PortfolioHoldingEntity, com.sans.finance.domain.model.ValuedHolding?>?>(null)
    }

    val sourceCounts = remember(state.holdings) {
        state.holdings.groupingBy { it.source }.eachCount().toSortedMap()
    }

    val valuedMap = remember(state.valuedHoldings) {
        state.valuedHoldings.associateBy {
            if (it.holding.id != 0L) it.holding.id.toString() else "${it.holding.source}_${it.holding.category}_${it.holding.asset}_${it.holding.accountKey}"
        }
    }

    fun getValued(holding: PortfolioHoldingEntity): com.sans.finance.domain.model.ValuedHolding? {
        val key = if (holding.id != 0L) holding.id.toString() else "${holding.source}_${holding.category}_${holding.asset}_${holding.accountKey}"
        return valuedMap[key]
    }

    // Comprehensive cross-portfolio filtering and sorting
    val filteredHoldings = remember(
        state.holdings,
        state.valuedHoldings,
        state.accountAliases,
        searchQuery,
        selectedSource,
        sortOption
    ) {
        val query = searchQuery.trim().lowercase()
        state.holdings.filter { holding ->
            val matchesSearch = query.isEmpty() ||
                holding.asset.lowercase().contains(query) ||
                holding.account.lowercase().contains(query) ||
                (state.accountAliases[holding.accountKey]?.lowercase()?.contains(query) == true) ||
                (holding.accountName?.lowercase()?.contains(query) == true) ||
                holding.assetClass.lowercase().contains(query) ||
                holding.category.lowercase().contains(query) ||
                holding.source.lowercase().contains(query)

            val matchesSource = selectedSource == null || holding.source.equals(selectedSource, ignoreCase = true)

            matchesSearch && matchesSource
        }.let { list ->
            when (sortOption) {
                PortfolioSortOption.VALUE_DESC -> list.sortedByDescending { h ->
                    getValued(h)?.currentValueInBase ?: h.valueIdr
                }
                PortfolioSortOption.VALUE_ASC -> list.sortedBy { h ->
                    getValued(h)?.currentValueInBase ?: h.valueIdr
                }
                PortfolioSortOption.GAIN_DESC -> list.sortedWith(
                    compareByDescending<PortfolioHoldingEntity> { h ->
                        getValued(h)?.hasCostBasis == true
                    }.thenByDescending { h ->
                        getValued(h)?.totalGainPercentage ?: Double.NEGATIVE_INFINITY
                    }.thenByDescending { h ->
                        getValued(h)?.priceGainInBase ?: 0.0
                    }
                )
                PortfolioSortOption.GAIN_ASC -> list.sortedWith(
                    compareByDescending<PortfolioHoldingEntity> { h ->
                        getValued(h)?.hasCostBasis == true
                    }.thenBy { h ->
                        getValued(h)?.totalGainPercentage ?: Double.POSITIVE_INFINITY
                    }.thenBy { h ->
                        getValued(h)?.priceGainInBase ?: 0.0
                    }
                )
                PortfolioSortOption.YIELD_DESC -> list.sortedByDescending { h ->
                    h.yieldRate ?: 0.0
                }
                PortfolioSortOption.NAME_ASC -> list.sortedBy { h ->
                    h.asset.lowercase()
                }
            }
        }
    }

    val totalFilteredValue = remember(filteredHoldings, valuedMap) {
        filteredHoldings.sumOf { getValued(it)?.currentValueInBase ?: it.valueIdr }
    }

    // Grouping by category with categories sorted dynamically by sort criteria
    val filteredHoldingsByCategory = remember(filteredHoldings, sortOption, valuedMap) {
        val grouped = filteredHoldings.groupBy { it.category }
        when (sortOption) {
            PortfolioSortOption.VALUE_DESC -> grouped.entries.sortedByDescending { (_, list) ->
                list.sumOf { getValued(it)?.currentValueInBase ?: it.valueIdr }
            }
            PortfolioSortOption.VALUE_ASC -> grouped.entries.sortedBy { (_, list) ->
                list.sumOf { getValued(it)?.currentValueInBase ?: it.valueIdr }
            }
            PortfolioSortOption.GAIN_DESC -> grouped.entries.sortedByDescending { (_, list) ->
                list.maxOfOrNull { getValued(it)?.totalGainPercentage ?: Double.NEGATIVE_INFINITY } ?: Double.NEGATIVE_INFINITY
            }
            PortfolioSortOption.GAIN_ASC -> grouped.entries.sortedBy { (_, list) ->
                list.minOfOrNull { getValued(it)?.totalGainPercentage ?: Double.POSITIVE_INFINITY } ?: Double.POSITIVE_INFINITY
            }
            PortfolioSortOption.YIELD_DESC -> grouped.entries.sortedByDescending { (_, list) ->
                list.maxOfOrNull { it.yieldRate ?: 0.0 } ?: 0.0
            }
            PortfolioSortOption.NAME_ASC -> grouped.entries.sortedBy { it.key.lowercase() }
        }.associate { it.key to it.value }
    }

    LaunchedEffect(state.importMessage) {
        state.importMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearImportMessage()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Portfolio",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showGuideSheet = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Panduan & Format Portofolio",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
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
                            DropdownMenuItem(
                                text = { Text("Import File (CSV / JSON)") },
                                onClick = {
                                    showMenu = false
                                    importLauncher.launch(arrayOf("text/*", "application/json", "*/*"))
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.FileOpen, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Panduan Data Portofolio") },
                                onClick = {
                                    showMenu = false
                                    showGuideSheet = true
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null)
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
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
                            "Sync snapshots dari Cloud Storage atau import file CSV / JSON",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { viewModel.syncFromGcs() },
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Sync Cloud", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { importLauncher.launch(arrayOf("text/*", "application/json", "*/*")) },
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Import File")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { showGuideSheet = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Panduan Pemula & Expert")
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
                        Tab(
                            selected = state.selectedTab == 2,
                            onClick = { viewModel.selectTab(2) },
                            text = {
                                Text(
                                    "Yield",
                                    fontWeight = if (state.selectedTab == 2) FontWeight.Black else FontWeight.Medium
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

                            if (sovereignAdvisor != null) {
                                item {
                                    com.sans.finance.presentation.portfolio.components.SovereignAdvisorCard(
                                        advisor = sovereignAdvisor,
                                        isPrivacyModeEnabled = state.isPrivacyModeEnabled
                                    )
                                }
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

                            if (state.currencyBreakdowns.size > 1) {
                                item {
                                    MultiCurrencyBreakdownCard(
                                        summaries = state.currencyBreakdowns,
                                        baseCurrency = state.currentCurrency,
                                        isPrivacyModeEnabled = state.isPrivacyModeEnabled
                                    )
                                }
                            }

                            item {
                                PortfolioFilterBar(
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { searchQuery = it },
                                    totalHoldingCount = state.holdings.size,
                                    sourceCounts = sourceCounts,
                                    selectedSource = selectedSource,
                                    onSourceSelect = { selectedSource = it },
                                    sortOption = sortOption,
                                    onSortOptionSelect = { sortOption = it },
                                    viewMode = viewMode,
                                    onToggleViewMode = {
                                        viewMode = if (viewMode == PortfolioViewMode.GROUPED) PortfolioViewMode.FLAT else PortfolioViewMode.GROUPED
                                    },
                                    isAllExpanded = collapsedCategories.isEmpty(),
                                    onToggleExpandAll = {
                                        collapsedCategories = if (collapsedCategories.isEmpty()) {
                                            filteredHoldingsByCategory.keys.toSet()
                                        } else {
                                            emptySet()
                                        }
                                    },
                                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                )
                            }

                            if (filteredHoldings.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        shape = MaterialTheme.shapes.extraLarge,
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(28.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = null,
                                                modifier = Modifier.size(36.dp),
                                                tint = MaterialTheme.colorScheme.outline
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                "No holdings match your search or filter",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            } else if (viewMode == PortfolioViewMode.FLAT) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.extraLarge,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Column {
                                            filteredHoldings.forEachIndexed { index, holding ->
                                                val valued = getValued(holding)
                                                EnhancedHoldingItem(
                                                    holding = holding,
                                                    valuedHolding = valued,
                                                    categoryTotal = totalFilteredValue,
                                                    currentCurrency = state.currentCurrency,
                                                    isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                                                    accountAliases = state.accountAliases,
                                                    onClick = { selectedHoldingForDetail = holding to valued }
                                                )
                                                if (index < filteredHoldings.size - 1) {
                                                    HorizontalDivider(
                                                        modifier = Modifier.padding(horizontal = 16.dp),
                                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                filteredHoldingsByCategory.forEach { (category, holdings) ->
                                    item(key = category) {
                                        val categoryTotal = holdings.sumOf { getValued(it)?.currentValueInBase ?: it.valueIdr }
                                        val categoryWeightPct = if (totalFilteredValue > 0.0) {
                                            (categoryTotal / totalFilteredValue) * 100.0
                                        } else 0.0

                                        ExpandableCategoryGroup(
                                            category = category,
                                            total = categoryTotal,
                                            categoryWeightPct = categoryWeightPct,
                                            holdings = holdings,
                                            valuedHoldings = state.valuedHoldings,
                                            currentCurrency = state.currentCurrency,
                                            isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                                            accountAliases = state.accountAliases,
                                            isExpanded = category !in collapsedCategories,
                                            onToggleExpand = {
                                                collapsedCategories = if (category in collapsedCategories) {
                                                    collapsedCategories - category
                                                } else {
                                                    collapsedCategories + category
                                                }
                                            },
                                            onHoldingClick = { h, v ->
                                                selectedHoldingForDetail = h to v
                                            }
                                        )
                                    }
                                }
                            }

                            item { Spacer(modifier = Modifier.height(32.dp)) }
                        }
                    } else if (state.selectedTab == 1) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (state.isAiAnalyzing || state.aiStreamingText.isNotEmpty() || state.aiAnalysis != null) {
                                item {
                                    PortfolioAiInsightCard(
                                        analysis = state.aiAnalysis,
                                        streamingText = state.aiStreamingText,
                                        isAnalyzing = state.isAiAnalyzing,
                                        onStop = viewModel::stopAiAnalysis,
                                        onClear = viewModel::clearAiAnalysis
                                    )
                                }
                            } else if (state.holdings.isNotEmpty()) {
                                item {
                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        alpha = 0.35f
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    "AI Portfolio Strategist",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    "Dapatkan evaluasi diversifikasi, risiko, dan rebalancing portofolio otomatis.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Button(
                                                onClick = viewModel::analyzePortfolioWithAi,
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Icon(
                                                    Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text("Analisis", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                PortfolioHealthView(
                                    healthList = state.healthList,
                                    rebalanceSuggestions = state.rebalanceSuggestions,
                                    currencyBreakdowns = state.currencyBreakdowns,
                                    isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                                    currentCurrency = state.currentCurrency,
                                    comparison = state.benchmarkComparison,
                                    selectedBenchmark = state.selectedBenchmark,
                                    onSelectBenchmark = viewModel::selectBenchmark,
                                    onTargetClick = { editingTarget = it }
                                )
                            }
                        }
                    } else if (state.selectedTab == 2) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            com.sans.finance.presentation.portfolio.components.DividendYieldView(
                                summary = state.dividendSummary,
                                isPrivacyModeEnabled = state.isPrivacyModeEnabled
                            )
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

        selectedHoldingForDetail?.let { (holding, valuedHolding) ->
            HoldingDetailBottomSheet(
                holding = holding,
                valuedHolding = valuedHolding,
                currentCurrency = state.currentCurrency,
                isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                accountAliases = state.accountAliases,
                onDismiss = { selectedHoldingForDetail = null }
            )
        }

        if (showGuideSheet) {
            PortfolioGuideBottomSheet(
                onDismiss = { showGuideSheet = false },
                onImportFile = {
                    showGuideSheet = false
                    importLauncher.launch(arrayOf("application/json", "text/*"))
                },
                snackbarHostState = snackbarHostState
            )
        }
    }
}

@Composable
fun MultiCurrencyBreakdownCard(
    summaries: List<com.sans.finance.domain.model.CurrencyValuationSummary>,
    baseCurrency: String,
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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Multi-Currency FX Valuation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Base: $baseCurrency",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            summaries.forEachIndexed { index, summary ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    summary.currency,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${summary.count} holding${if (summary.count > 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (summary.currency != baseCurrency) {
                            Text(
                                "FX: 1 ${summary.currency} = ${String.format(Locale.US, "%,.2f", summary.currentFxRate)} $baseCurrency",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        PrivacyText(
                            amount = (summary.totalInBaseCurrency * 100).toLong(),
                            currencyCode = baseCurrency,
                            isVisible = !isPrivacyModeEnabled,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (summary.currency != baseCurrency && summary.fxGainInBase != 0.0) {
                            val fxColor = if (summary.fxGainInBase >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                            val fxSign = if (summary.fxGainInBase >= 0) "+" else ""
                            Text(
                                text = "FX: $fxSign${com.sans.finance.core.util.CurrencyFormatter.formatAmountCompact((summary.fxGainInBase * 100).toLong(), baseCurrency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = fxColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (index < summaries.size - 1) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
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
                "Portfolio Value",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            val displayValue = if (state.totalValueInBase > 0) state.totalValueInBase else state.totalValueIdr
            PrivacyText(
                amount = (displayValue * 100).toLong(),
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

            // Annualized Return (XIRR)
            state.xirr?.let { xirrValue ->
                Text(
                    text = "ANNUALIZED RETURN (XIRR): ${String.format(Locale.US, "%.2f%%", xirrValue * 100)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 4.dp),
                    letterSpacing = 0.5.sp
                )
            }

            // Gain Breakdown (Cost-basis Total Return if available, else Snapshot-over-Snapshot Trajectory)
            if (state.hasCostBasis && (state.totalGainInBase != 0.0 || state.totalPriceGainInBase != 0.0)) {
                val gainColor = if (state.totalGainInBase >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                val gainSign = if (state.totalGainInBase >= 0) "+" else ""

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(gainColor.copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (state.totalGainInBase >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = gainColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Total Return: $gainSign${com.sans.finance.core.util.CurrencyFormatter.formatAmountCompact((state.totalGainInBase * 100).toLong(), state.currentCurrency)} (${String.format(Locale.US, "%+.2f%%", state.totalGainPercentage)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = gainColor,
                        fontWeight = FontWeight.Black
                    )
                }

                if (state.totalFxGainInBase != 0.0 || state.totalPriceGainInBase != 0.0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        val fxColor = if (state.totalFxGainInBase >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        val fxSign = if (state.totalFxGainInBase >= 0) "+" else ""
                        Text(
                            text = "FX Movement: $fxSign${com.sans.finance.core.util.CurrencyFormatter.formatAmountCompact((state.totalFxGainInBase * 100).toLong(), state.currentCurrency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = fxColor,
                            fontWeight = FontWeight.Bold
                        )

                        if (state.totalPriceGainInBase != 0.0) {
                            Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            val priceColor = if (state.totalPriceGainInBase >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            val priceSign = if (state.totalPriceGainInBase >= 0) "+" else ""
                            Text(
                                text = "Price: $priceSign${com.sans.finance.core.util.CurrencyFormatter.formatAmountCompact((state.totalPriceGainInBase * 100).toLong(), state.currentCurrency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = priceColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else if (state.previousTotalIdr != null) {
                val currentVal = if (state.totalValueInBase > 0) state.totalValueInBase else state.totalValueIdr
                val prev = state.previousTotalIdr
                val diff = currentVal - prev
                val percent = if (prev != 0.0) (diff / prev) * 100 else 0.0
                val color = if (diff >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                val sign = if (diff >= 0) "+" else ""

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 8.dp)
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
                        text = "$sign${com.sans.finance.core.util.CurrencyFormatter.formatAmountCompact((diff * 100).toLong(), state.currentCurrency)} (${String.format(Locale.US, "%+.2f%%", percent)}) vs last snapshot",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Black
                    )
                }

                if (state.totalFxGainInBase != 0.0) {
                    val fxColor = if (state.totalFxGainInBase >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    val fxSign = if (state.totalFxGainInBase >= 0) "+" else ""
                    Text(
                        text = "FX Movement: $fxSign${com.sans.finance.core.util.CurrencyFormatter.formatAmountCompact((state.totalFxGainInBase * 100).toLong(), state.currentCurrency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = fxColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else if (state.totalFxGainInBase != 0.0) {
                val fxColor = if (state.totalFxGainInBase >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                val fxSign = if (state.totalFxGainInBase >= 0) "+" else ""
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(fxColor.copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "FX Movement: $fxSign${com.sans.finance.core.util.CurrencyFormatter.formatAmountCompact((state.totalFxGainInBase * 100).toLong(), state.currentCurrency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = fxColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Initial snapshot • Sync over time to track returns",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
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
fun PortfolioAiInsightCard(
    analysis: com.sans.finance.data.ai.PortfolioAnalysisResult?,
    streamingText: String,
    isAnalyzing: Boolean,
    onStop: () -> Unit,
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
                        if (isAnalyzing) "AI STRATEGIST • STREAMING" else "AI STRATEGIST",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }
                if (isAnalyzing) {
                    IconButton(onClick = onStop, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop Analysis",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (isAnalyzing && streamingText.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Menghubungkan ke AI Strategist & menganalisis portofolio...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (streamingText.isNotEmpty()) {
                MarkdownContent(
                    text = streamingText,
                    isStreaming = isAnalyzing
                )
            } else if (analysis?.rawText != null) {
                MarkdownContent(
                    text = analysis.rawText,
                    isStreaming = false
                )
            } else if (analysis != null) {
                Text(
                    analysis.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )

                if (analysis.insights.isNotEmpty()) {
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
    }
}
