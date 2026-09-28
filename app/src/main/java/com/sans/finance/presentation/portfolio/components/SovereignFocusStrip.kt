package com.sans.finance.presentation.portfolio.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sans.finance.data.util.SovereignAdvisorJson

@Composable
fun SovereignFocusStrip(
    advisor: SovereignAdvisorJson,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playbook = advisor.reinvestmentPlaybook
    val hasMaturity = playbook?.hasUpcomingMaturity == true
    val containerColor = if (hasMaturity) {
        Color(0xFFFFF8E1)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    }
    val borderColor = if (hasMaturity) {
        Color(0xFFFFB300).copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    }
    val accentColor = if (hasMaturity) {
        Color(0xFFE65100)
    } else {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hasMaturity) Icons.Default.WarningAmber else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (hasMaturity) "SBN ROLLOVER ALERT" else "ADHD FOCUS ROUTER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = accentColor,
                            letterSpacing = 0.6.sp
                        )
                        if (hasMaturity && playbook != null) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "• ${playbook.daysLeft} hari lagi",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }

                    val subtitleText = when {
                        hasMaturity && playbook != null -> "${playbook.maturingAsset} jatuh tempo. Tap untuk Playbook & Rebalance."
                        advisor.depositRouter != null -> "Prioritas DCA: ${advisor.depositRouter.priorityAssetClass}. Tap untuk arahan 1-langkah."
                        else -> "Tap untuk membuka ADHD Focus Suite & Sovereign Playbook"
                    }

                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f))
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Buka Playbook",
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
