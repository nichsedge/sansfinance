package com.sans.finance.presentation.installments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sans.finance.R
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.core.util.DateFormatterUtils
import com.sans.finance.domain.model.Installment
import com.sans.finance.domain.model.InstallmentItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentDetailBottomSheet(
    context: InstallmentDetailContext,
    onDismiss: () -> Unit,
    onToggleStatus: (itemId: Long, currentStatus: String) -> Unit,
    onEditExpense: (expenseId: Long) -> Unit,
    onDeletePlan: (installment: Installment) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val installment = context.installment
    val items = context.items
    val currencyCode = context.currencyCode

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InstallmentHeader(
                installment = installment,
                onDeleteClick = { showDeleteDialog = true }
            )

            val paidCount = items.count { it.status == "Paid" }
            InstallmentSummaryCard(
                installment = installment,
                paidCount = paidCount,
                currencyCode = currencyCode
            )

            Text(
                text = "PAYMENT SCHEDULE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            InstallmentScheduleCard(
                items = items,
                currencyCode = currencyCode,
                onToggleStatus = onToggleStatus
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onEditExpense(installment.expenseId)
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Transaction")
                }
            }
        }
    }

    if (showDeleteDialog) {
        InstallmentDeleteDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirmDelete = {
                showDeleteDialog = false
                onDeletePlan(installment)
                onDismiss()
            }
        )
    }
}

@Composable
private fun InstallmentHeader(
    installment: Installment,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = installment.expenseName ?: "Installment Plan #${installment.id}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            installment.expenseDate?.let {
                Text(
                    text = "Started: ${DateFormatterUtils.formatStandardDate(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onDeleteClick) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete Plan",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun InstallmentSummaryCard(
    installment: Installment,
    paidCount: Int,
    currencyCode: String
) {
    val formattedMonthly = CurrencyFormatter.formatAmount(installment.monthlyPayment, currencyCode)
    val formattedRemaining = CurrencyFormatter.formatAmount(installment.remainingBalance, currencyCode)
    val progress = if (installment.durationMonths > 0) {
        paidCount.toFloat() / installment.durationMonths
    } else {
        0f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Payment",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$formattedMonthly/mo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Remaining Debt",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formattedRemaining,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$paidCount of ${installment.durationMonths} months paid",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${(progress * 100).toInt()}% completed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InstallmentScheduleCard(
    items: List<InstallmentItem>,
    currencyCode: String,
    onToggleStatus: (itemId: Long, currentStatus: String) -> Unit
) {
    val now = System.currentTimeMillis()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (items.isEmpty()) {
                Text(
                    text = "No payments generated",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                items.forEachIndexed { index, monthlyItem ->
                    InstallmentScheduleRow(
                        monthlyItem = monthlyItem,
                        currencyCode = currencyCode,
                        now = now,
                        onToggleStatus = onToggleStatus
                    )
                    if (index < items.size - 1) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallmentScheduleRow(
    monthlyItem: InstallmentItem,
    currencyCode: String,
    now: Long,
    onToggleStatus: (itemId: Long, currentStatus: String) -> Unit
) {
    val monthYearFormatter = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }
    val isOverdue = monthlyItem.status == "Pending" && monthlyItem.dueDate < now
    val formattedAmount = CurrencyFormatter.formatAmount(monthlyItem.amount, currencyCode)
    val formattedDate = DateFormatterUtils.formatStandardDate(monthlyItem.dueDate)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${monthYearFormatter.format(Date(monthlyItem.dueDate))} · Month ${monthlyItem.monthNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                if (isOverdue) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Overdue",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = "$formattedDate · $formattedAmount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            val statusColor = when {
                monthlyItem.status == "Paid" -> MaterialTheme.colorScheme.primary
                isOverdue -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val statusLabel = when {
                monthlyItem.status == "Paid" -> stringResource(R.string.paid)
                isOverdue -> "Overdue"
                else -> stringResource(R.string.pending)
            }
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = { onToggleStatus(monthlyItem.id, monthlyItem.status) },
                modifier = Modifier.size(32.dp)
            ) {
                val icon = if (monthlyItem.status == "Paid") {
                    Icons.Default.CheckCircle
                } else {
                    Icons.Outlined.Circle
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Toggle Status",
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun InstallmentDeleteDialog(
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Installment Plan") },
        text = {
            Text("Are you sure you want to delete this entire installment plan and its generated schedule?")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

