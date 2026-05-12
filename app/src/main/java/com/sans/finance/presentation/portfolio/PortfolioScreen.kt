package com.sans.finance.presentation.portfolio

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.presentation.components.PrivacyText
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.presentation.portfolio.components.AllocationDonutChart
import com.sans.finance.presentation.portfolio.components.NetWorthTrendChart
import com.sans.finance.presentation.portfolio.components.PortfolioHealthView
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    onDashboardClick: () -> Unit,
    onForecastingClick: () -> Unit,
    viewModel: PortfolioViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showViewMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importFile(it) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(state.importMessage) {
        state.importMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearImportMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showViewMenu = true }
                        ) {
                            Text("Portfolio", fontWeight = FontWeight.Bold)
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Switch View",
                                modifier = Modifier.size(20.dp)
                            )

                            DropdownMenu(
                                expanded = showViewMenu,
                                onDismissRequest = { showViewMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Dashboard") },
                                    onClick = {
                                        showViewMenu = false
                                        onDashboardClick()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Dashboard, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Portfolio") },
                                    onClick = { showViewMenu = false },
                                    leadingIcon = { Icon(Icons.Default.PieChart, contentDescription = null) }
                                )
                            }
                        }
                        if (state.snapshotDates.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.togglePrivacyMode() }) {
                                    Icon(
                                        imageVector = if (state.isPrivacyModeEnabled) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (state.isPrivacyModeEnabled) "Show balances" else "Hide balances"
                                    )
                                }
                                IconButton(onClick = viewModel::onPreviousSnapshot) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                                }
                                Text(
                                    state.selectedDate?.let { dateFormat.format(Date(it)) } ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = viewModel::onNextSnapshot) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                                }
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { launcher.launch("*/*") }) {
                Icon(Icons.Default.UploadFile, contentDescription = "Import Snapshot")
            }
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.snapshotDates.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.QueryStats, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text("No portfolio data", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Import a CSV or JSON snapshot to start", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                TabRow(
                    selectedTabIndex = state.selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    Tab(
                        selected = state.selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        text = { Text("Overview") }
                    )
                    Tab(
                        selected = state.selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        text = { Text("Health") }
                    )
                }

                if (state.selectedTab == 0) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        item {
                            PortfolioHeader(state, onForecastingClick)
                        }

                        if (state.valueHistory.size >= 2) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    shape = MaterialTheme.shapes.extraLarge,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Text(
                                            "Net Worth Trend",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        NetWorthTrendChart(
                                            history = state.valueHistory,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp),
                                            isPrivacyModeEnabled = state.isPrivacyModeEnabled
                                        )
                                    }
                                }
                            }
                        }

                        if (state.categoryTotals.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    shape = MaterialTheme.shapes.extraLarge,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Text(
                                            "Asset Allocation",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
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
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(category, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    PrivacyText(
                                        amount = (categoryTotal * 100).toLong(),
                                        currencyCode = "IDR",
                                        isVisible = !state.isPrivacyModeEnabled,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            items(holdings) { holding ->
                                HoldingItem(holding, state.isPrivacyModeEnabled)
                                Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                            }
                        }
                    }
                } else {
                    PortfolioHealthView(
                        healthList = state.healthList,
                        isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                        modifier = Modifier.padding(bottom = 80.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PortfolioHeader(state: PortfolioScreenState, onForecastingClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Total Portfolio Value",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PrivacyText(
            amount = (state.totalValueIdr * 100).toLong(),
            currencyCode = "IDR",
            isVisible = !state.isPrivacyModeEnabled,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        
        state.previousTotalIdr?.let { prev ->
            val diff = state.totalValueIdr - prev
            val percent = if (prev != 0.0) (diff / prev) * 100 else 0.0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (diff >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = if (diff >= 0) Color(0xFF4CAF50) else Color(0xFFE57373),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${if (diff >= 0) "+" else ""}${String.format("%.2f", percent)}% vs last",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (diff >= 0) Color(0xFF4CAF50) else Color(0xFFE57373),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        Text(
            text = "≈ ${if (state.isPrivacyModeEnabled) "••••" else String.format("%,.2f", state.totalValueUsd)} USD",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onForecastingClick,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.TrendingUp, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Wealth Trajectory")
        }
    }
}

@Composable
fun HoldingItem(holding: PortfolioHoldingEntity, isPrivacyModeEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(holding.asset, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("${holding.source} • ${holding.assetClass}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        Column(horizontalAlignment = Alignment.End) {
            PrivacyText(
                amount = (holding.valueIdr * 100).toLong(),
                currencyCode = "IDR",
                isVisible = !isPrivacyModeEnabled,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (holding.amount > 0) {
                Text(
                    text = "${if (isPrivacyModeEnabled) "••••" else String.format("%.4f", holding.amount)} ${holding.currency}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
