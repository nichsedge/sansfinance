package com.sans.finance.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    isCompact: Boolean = false
) {
    val text = if (isVisible) {
        if (isCompact) {
            CurrencyFormatter.formatAmountCompact(amount, currencyCode)
        } else {
            CurrencyFormatter.formatAmount(amount, currencyCode)
        }
    } else {
        "••••••"
    }

    Text(
        text = text,
        modifier = modifier,
        style = style,
        fontWeight = fontWeight,
        color = color
    )
}
