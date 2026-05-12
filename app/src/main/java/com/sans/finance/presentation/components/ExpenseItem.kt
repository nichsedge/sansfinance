package com.sans.finance.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.sans.finance.R
import com.sans.finance.domain.model.Expense

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpenseItem(
    expense: Expense,
    categoryName: String?,
    categoryIcon: String,
    accountName: String? = null,
    showNextDueDate: Boolean = false,
    isPrivacyModeEnabled: Boolean = false,
    overrideAmount: Long? = null,
    overrideLabel: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = if (expense.isInstallmentPayment) MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = 0.5f
        ) else MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.weight(0.35f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryIcon(
                        icon = categoryIcon,
                        fontSize = 16.sp
                    )
                    Text(
                        categoryName ?: stringResource(R.string.uncategorized),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Column(modifier = Modifier.weight(0.65f)) {
                    Text(
                        expense.note.ifBlank { expense.description ?: "" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (expense.isInstallmentPayment) MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.7f
                        ) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            accountName ?: "Unknown",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        if (expense.isRecurring && expense.recurrenceInterval != null) {
                            Text(
                                " • ${
                                    expense.recurrenceInterval.lowercase()
                                        .replaceFirstChar { it.uppercase() }
                                }",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (expense.isInstallmentPayment && expense.installmentTotalMonths > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    "${expense.installmentMonth} / ${expense.installmentTotalMonths}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (expense.status == "Pending") {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    "Pending",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    if (showNextDueDate && expense.nextDueDate != null) {
                        val dateStr =
                            com.sans.finance.core.util.DateFormatterUtils.getStandardFormatter()
                                .format(java.util.Date(expense.nextDueDate))
                        Text(
                            "Next: $dateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    val displayAmount = overrideAmount
                        ?: if (expense.isInstallment && expense.monthlyPayment > 0) expense.monthlyPayment else expense.amount
                    val amountColor = when (expense.type) {
                        "INCOME" -> Color(0xFF4CAF50)
                        "EXPENSE" -> Color(0xFFE53935)
                        "TRANSFER" -> Color(0xFF2196F3)
                        else -> Color(0xFFE53935)
                    }
                    PrivacyText(
                        amount = if (expense.type == "INCOME") displayAmount else -displayAmount,
                        currencyCode = expense.currency,
                        isVisible = !isPrivacyModeEnabled,
                        style = MaterialTheme.typography.bodyMedium,
                        color = amountColor
                    )
                    if (overrideLabel != null) {
                        Text(
                            text = overrideLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            androidx.compose.material3.HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )
        }
    }
}
