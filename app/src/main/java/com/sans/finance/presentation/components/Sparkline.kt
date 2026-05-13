package com.sans.finance.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun Sparkline(
    data: List<Long>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    lineWidth: Float = 4f,
    showFill: Boolean = true,
    onValueSelected: ((Int) -> Unit)? = null
) {
    if (data.size < 2) return

    Canvas(
        modifier = modifier
            .then(
                if (onValueSelected != null) {
                    Modifier.pointerInput(data) {
                        detectTapGestures { offset ->
                            val width = size.width
                            val stepX = width / (data.size - 1)
                            val index = (offset.x / stepX).toInt().coerceIn(0, data.size - 1)
                            onValueSelected(index)
                        }
                    }
                } else Modifier
            )
    ) {
        val width = size.width
        val height = size.height

        val minData = data.minOrNull() ?: 0L
        val maxData = data.maxOrNull() ?: 1L
        val range = (maxData - minData).coerceAtLeast(1L)

        val stepX = width / (data.size - 1)
        val path = Path()

        data.forEachIndexed { index, value ->
            val x = index * stepX
            val fractionY = (value - minData).toFloat() / range.toFloat()
            val y = height - (fractionY * height)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        if (showFill) {
            val fillPath = Path().apply {
                addPath(path)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.2f), Color.Transparent)
                )
            )
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = lineWidth)
        )
    }
}
