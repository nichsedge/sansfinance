package com.sans.finance.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    alpha: Float = 0.5f,
    borderAlpha: Float = 0.1f,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        containerColor.copy(alpha = alpha),
                        containerColor.copy(alpha = alpha * 0.7f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderAlpha * 2),
                        Color.White.copy(alpha = borderAlpha)
                    )
                ),
                shape = MaterialTheme.shapes.extraLarge
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}
