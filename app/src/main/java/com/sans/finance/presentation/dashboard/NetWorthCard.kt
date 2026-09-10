package com.sans.finance.presentation.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sans.finance.presentation.components.GlassCard
import com.sans.finance.presentation.components.PrivacyText

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NetWorthCard(
    netWorth: Long,
    assets: Long,
    liabilities: Long,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean,
    onTogglePrivacyMode: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTogglePrivacyMode()
                },
                onDoubleClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTogglePrivacyMode()
                }
            ),
        containerColor = MaterialTheme.colorScheme.primary,
        alpha = 0.15f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Net Worth",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            PrivacyText(
                amount = netWorth,
                currencyCode = currencyCode,
                isVisible = !isPrivacyModeEnabled,
                animate = true,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BreakdownItem(
                    "Assets",
                    assets,
                    MaterialTheme.colorScheme.tertiary,
                    currencyCode,
                    isPrivacyModeEnabled
                )
                VerticalDivider(
                    modifier = Modifier.height(32.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                BreakdownItem(
                    "Liabilities",
                    liabilities,
                    MaterialTheme.colorScheme.error,
                    currencyCode,
                    isPrivacyModeEnabled
                )
            }
        }
    }
}

@Composable
fun BreakdownItem(
    label: String,
    amount: Long,
    color: Color,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold
        )
        PrivacyText(
            amount = amount,
            currencyCode = currencyCode,
            isVisible = !isPrivacyModeEnabled,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

