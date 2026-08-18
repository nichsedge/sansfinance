package com.sans.finance.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.sans.finance.core.util.CurrencyFormatter

@Composable
fun PrivacyText(
    amount: Long,
    currencyCode: String,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    isCompact: Boolean = false,
    animate: Boolean = false,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: androidx.compose.ui.text.style.TextOverflow = androidx.compose.ui.text.style.TextOverflow.Clip
) {
    val animatedAmount by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "AmountAnimation"
    )

    val currentAmount = if (animate) animatedAmount.toLong() else amount

    val text = if (isVisible) {
        if (isCompact) {
            CurrencyFormatter.formatAmountCompact(currentAmount, currencyCode)
        } else {
            CurrencyFormatter.formatAmount(currentAmount, currencyCode)
        }
    } else {
        "••••••"
    }

    val combinedStyle = style.copy(
        fontFeatureSettings = if (style.fontFeatureSettings.isNullOrBlank()) "tnum" else style.fontFeatureSettings
    )

    Text(
        text = text,
        modifier = modifier,
        style = combinedStyle,
        fontWeight = fontWeight,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow
    )
}
