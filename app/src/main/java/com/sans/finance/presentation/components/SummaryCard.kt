package com.sans.finance.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SummaryCard(
    income: Long,
    expense: Long,
    total: Long,
    currencyCode: String,
    avgMonthlyExpense: Long = 0L,
    isPrivacyModeEnabled: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SummaryItem(
                    label = "Income",
                    amount = income,
                    currencyCode = currencyCode,
                    color = MaterialTheme.colorScheme.tertiary,
                    isPrivacyModeEnabled = isPrivacyModeEnabled,
                    modifier = Modifier.weight(1f)
                )

                SummaryItem(
                    label = "Expenses",
                    amount = expense,
                    currencyCode = currencyCode,
                    color = MaterialTheme.colorScheme.error,
                    isPrivacyModeEnabled = isPrivacyModeEnabled,
                    modifier = Modifier.weight(1f)
                )

                SummaryItem(
                    label = "Balance",
                    amount = total,
                    currencyCode = currencyCode,
                    color = MaterialTheme.colorScheme.onSurface,
                    isPrivacyModeEnabled = isPrivacyModeEnabled,
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                )
            }

            if (avgMonthlyExpense > 0) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            CircleShape
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AVG. MONTHLY SPENDING: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    PrivacyText(
                        amount = avgMonthlyExpense,
                        currencyCode = currencyCode,
                        isVisible = !isPrivacyModeEnabled,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    amount: Long,
    currencyCode: String,
    color: Color,
    isPrivacyModeEnabled: Boolean,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        PrivacyText(
            amount = amount,
            currencyCode = currencyCode,
            isVisible = !isPrivacyModeEnabled,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            ),
            color = color,
            modifier = Modifier.fillMaxWidth(),
            textAlign = when (horizontalAlignment) {
                Alignment.Start -> TextAlign.Start
                Alignment.End -> TextAlign.End
                else -> TextAlign.Center
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
