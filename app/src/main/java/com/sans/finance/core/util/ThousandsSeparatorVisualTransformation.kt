package com.sans.finance.core.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class ThousandsSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        // Find decimal separator if it exists
        val decimalIndex = originalText.indexOfFirst { it == '.' || it == ',' }

        val intPart: String
        val decPart: String
        if (decimalIndex != -1) {
            intPart = originalText.substring(0, decimalIndex)
            decPart = originalText.substring(decimalIndex)
        } else {
            intPart = originalText
            decPart = ""
        }

        val formattedIntPart = buildString {
            for (i in intPart.indices) {
                append(intPart[i])
                val remaining = intPart.length - 1 - i
                if (remaining > 0 && remaining % 3 == 0) {
                    append(' ')
                }
            }
        }

        val formattedText = formattedIntPart + decPart

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= intPart.length) {
                    var separators = 0
                    for (i in 0 until offset) {
                        val remaining = intPart.length - 1 - i
                        if (remaining > 0 && remaining % 3 == 0) {
                            separators++
                        }
                    }
                    return offset + separators
                } else {
                    val totalSeparators = if (intPart.isEmpty()) 0 else (intPart.length - 1) / 3
                    return offset + totalSeparators
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= formattedIntPart.length) {
                    var separators = 0
                    for (i in 0 until offset) {
                        if (formattedIntPart[i] == ' ') {
                            separators++
                        }
                    }
                    return offset - separators
                } else {
                    val totalSeparators = if (intPart.isEmpty()) 0 else (intPart.length - 1) / 3
                    return offset - totalSeparators
                }
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
