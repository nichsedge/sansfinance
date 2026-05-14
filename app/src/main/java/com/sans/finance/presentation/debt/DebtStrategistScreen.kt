package com.sans.finance.presentation.debt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.core.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtStrategistScreen(
    onBack: () -> Unit,
    viewModel: DebtStrategistViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debt Strategist", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.loans.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No active loans or credit card debts found.")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    "Compare strategies to pay off your ${state.loans.size} debts faster.",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Strategy Comparison
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    state.snowballResult?.let { StrategyCard(it, Modifier.weight(1f), MaterialTheme.colorScheme.primary) }
                    state.avalancheResult?.let { StrategyCard(it, Modifier.weight(1f), MaterialTheme.colorScheme.tertiary) }
                }

                Text("ACTIVE DEBTS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                state.loans.forEach { loan ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(loan.name, fontWeight = FontWeight.Bold)
                                Text("${loan.interestRate}% Interest", style = MaterialTheme.typography.labelSmall)
                            }
                            Text(
                                CurrencyFormatter.formatAmount(loan.balance, loan.currency),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StrategyCard(result: DebtStrategyResult, modifier: Modifier, accentColor: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(result.strategyName.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = accentColor)
            Spacer(Modifier.height(8.dp))
            Text("${result.monthsToDebtFree} Months", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text("to debt free", style = MaterialTheme.typography.labelSmall)
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = accentColor.copy(alpha = 0.2f))
            Text("Interest: ${String.format("IDR %,.0f", result.totalInterestPaid)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}
