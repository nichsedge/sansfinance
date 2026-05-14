package com.sans.finance.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    searchQuery: String = "",
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = if (expense.isInstallmentPayment) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = if (expense.isInstallmentPayment) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category Icon
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    icon = categoryIcon,
                    fontSize = 14.sp
                )
            }

            // Details
            Column(modifier = Modifier.weight(1f)) {
                val noteText = expense.title.ifBlank { expense.details ?: "" }
                val annotatedNote = remember(noteText, searchQuery) {
                    if (searchQuery.isEmpty()) {
                        androidx.compose.ui.text.AnnotatedString(noteText)
                    } else {
                        val builder = androidx.compose.ui.text.AnnotatedString.Builder(noteText)
                        val lowerNote = noteText.lowercase()
                        val lowerQuery = searchQuery.lowercase()
                        var start = 0
                        while (start < lowerNote.length) {
                            val index = lowerNote.indexOf(lowerQuery, start)
                            if (index == -1) break
                            builder.addStyle(
                                style = androidx.compose.ui.text.SpanStyle(
                                    background = Color.Yellow.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Bold
                                ),
                                start = index,
                                end = index + lowerQuery.length
                            )
                            start = index + lowerQuery.length
                        }
                        builder.toAnnotatedString()
                    }
                }

                Text(
                    text = annotatedNote,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        categoryName ?: stringResource(R.string.uncategorized),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                    Text(
                        "•",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        accountName ?: "Unknown",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    val interval = expense.recurrenceInterval
                    if (expense.isRecurring && interval != null) {
                        Text(
                            "• ${interval.lowercase().take(3)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (expense.isInstallmentPayment && expense.installmentTotalMonths > 0) {
                        Text(
                            "• ${expense.installmentMonth}/${expense.installmentTotalMonths}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            // Amount
            Column(horizontalAlignment = Alignment.End) {
                val displayAmount = overrideAmount
                    ?: if (expense.isInstallment && expense.monthlyPayment > 0) expense.monthlyPayment else expense.amount
                val amountColor = when (expense.type) {
                    "INCOME" -> Color(0xFF4CAF50)
                    "EXPENSE" -> Color(0xFFE53935)
                    "TRANSFER" -> Color(0xFF2196F3)
                    else -> Color(0xFFE53935)
                }
                val prefix = when (expense.type) {
                    "INCOME" -> "+"
                    "EXPENSE" -> "-"
                    else -> ""
                }

                PrivacyText(
                    amount = if (expense.type == "INCOME") displayAmount else -displayAmount,
                    currencyCode = expense.currency,
                    isVisible = !isPrivacyModeEnabled,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = amountColor
                )

                if (expense.status == "Pending") {
                    Text(
                        "Pending",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                } else if (overrideLabel != null) {
                    Text(
                        text = overrideLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
