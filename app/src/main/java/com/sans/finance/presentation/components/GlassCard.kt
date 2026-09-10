package com.sans.finance.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

import androidx.compose.ui.graphics.luminance

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    alpha: Float = 0.5f,
    borderAlpha: Float = 0.1f,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val backgroundBrush = remember(containerColor, alpha) {
        Brush.verticalGradient(
            colors = listOf(
                containerColor.copy(alpha = alpha),
                containerColor.copy(alpha = alpha * 0.7f)
            )
        )
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val borderBrush = remember(borderAlpha, isDark, outlineVariant) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = borderAlpha * 2f),
                    Color.White.copy(alpha = borderAlpha)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    outlineVariant.copy(alpha = (borderAlpha * 4f).coerceIn(0.2f, 0.7f)),
                    outlineVariant.copy(alpha = (borderAlpha * 2f).coerceIn(0.1f, 0.4f))
                )
            )
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                this.shape = shape
                this.clip = true
            }
            .background(backgroundBrush)
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = shape
            )
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}
