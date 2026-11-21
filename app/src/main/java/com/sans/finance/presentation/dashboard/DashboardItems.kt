package com.sans.finance.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sans.finance.core.util.DateFormatterUtils
import com.sans.finance.domain.model.CategoryBudgetProgress
import com.sans.finance.domain.model.Expense
import com.sans.finance.presentation.components.CategoryIcon
import com.sans.finance.presentation.components.PrivacyText
import java.util.Date

@Composable
fun UpcomingBillsCard(
    bills: List<Expense>,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean,
    onRecurringExpensesClick: () -> Unit,
    onInstallmentsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (bills.isEmpty()) return
    var showBillsMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Upcoming Obligations",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                CircleShape
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${bills.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp
                        )
                    }
                }

                Box {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { showBillsMenu = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "See All",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showBillsMenu,
                        onDismissRequest = { showBillsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Recurring Payments") },
                            onClick = {
                                showBillsMenu = false
                                onRecurringExpensesClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Installments") },
                            onClick = {
                                showBillsMenu = false
                                onInstallmentsClick()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column {
                bills.forEachIndexed { index, bill ->
                    DashboardBillRowItem(
                        bill = bill,
                        currencyCode = currencyCode,
                        isPrivacyModeEnabled = isPrivacyModeEnabled
                    )
                    if (index < bills.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardBillRowItem(
    bill: Expense,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            CategoryIcon(
                icon = bill.categoryIcon ?: "📄",
                fontSize = 16.sp
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bill.title.ifBlank { bill.categoryName ?: "Upcoming Bill" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            val dueLabel = if (bill.isInstallmentPayment) {
                "Installment ${bill.installmentMonth}/${bill.installmentTotalMonths}"
            } else {
                val dueDate = bill.nextDueDate
                if (dueDate != null) "Due ${DateFormatterUtils.getStandardFormatter().format(Date(dueDate))}"
                else "Recurring Bill"
            }
            Text(
                text = dueLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        PrivacyText(
            amount = bill.amount,
            currencyCode = bill.currency.ifBlank { currencyCode },
            isVisible = !isPrivacyModeEnabled,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun RecentTransactionsCard(
    transactions: List<Expense>,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (transactions.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Activity",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Latest ${transactions.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column {
                transactions.forEachIndexed { index, transaction ->
                    RecentTransactionRowItem(
                        transaction = transaction,
                        currencyCode = currencyCode,
                        isPrivacyModeEnabled = isPrivacyModeEnabled,
                        onClick = { onTransactionClick(transaction.id) }
                    )
                    if (index < transactions.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionRowItem(
    transaction: Expense,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean,
    onClick: () -> Unit
) {
    val isIncome = transaction.type == "INCOME"
    val statusColor = if (isIncome) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    (if (isIncome) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary).copy(
                        alpha = 0.1f
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            CategoryIcon(
                icon = transaction.categoryIcon ?: (if (isIncome) "💰" else "💸"),
                fontSize = 16.sp
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title.ifBlank { transaction.categoryName ?: "Transaction" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            val subtitle = if (transaction.title.isNotBlank()) {
                transaction.details ?: transaction.categoryName ?: ""
            } else {
                transaction.details ?: ""
            }
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            PrivacyText(
                amount = transaction.amount,
                currencyCode = transaction.currency.ifBlank { currencyCode },
                isVisible = !isPrivacyModeEnabled,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
                color = statusColor
            )
            Text(
                text = DateFormatterUtils.getStandardFormatter().format(Date(transaction.date)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun DashboardGoalItem(goal: DashboardGoal, currencyCode: String, isPrivacyModeEnabled: Boolean) {
    val progress = goal.progress
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    goal.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (!isPrivacyModeEnabled) {
                    Text(
                        "${String.format(java.util.Locale.US, "%.1f", progress * 100f)}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        "••%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun CategoryBudgetItem(
    budget: CategoryBudgetProgress,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.shapes.small
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CategoryIcon(icon = budget.categoryIcon ?: "📁", fontSize = 14.sp)
                }
                Text(
                    text = budget.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                PrivacyText(
                    amount = budget.budgetAmount - budget.spentAmount,
                    currencyCode = currencyCode,
                    isVisible = !isPrivacyModeEnabled,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Black,
                    color = if (budget.spentAmount > budget.budgetAmount) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { budget.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (budget.progress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

