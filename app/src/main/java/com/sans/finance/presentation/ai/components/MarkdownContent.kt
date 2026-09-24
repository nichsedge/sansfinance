package com.sans.finance.presentation.ai.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Lightweight, high-performance Markdown renderer for Jetpack Compose.
 * Handles bold (**text**), italic (*text*), inline code (`code`), headings (###),
 * and clean bullet lists (- / *) with comfortable typography line-height.
 */
@Composable
fun MarkdownContent(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isStreaming: Boolean = false
) {
    val lines = remember(text) { text.split("\n") }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        var inBulletList = false

        for ((index, rawLine) in lines.withIndex() ) {
            val line = rawLine.trimEnd()

            if (line.isBlank()) {
                if (inBulletList) {
                    inBulletList = false
                }
                Spacer(modifier = Modifier.height(6.dp))
                continue
            }

            val trimmed = line.trimStart()

            when {
                // Headings
                trimmed.startsWith("### ") -> {
                    inBulletList = false
                    val content = trimmed.removePrefix("### ").trim()
                    Text(
                        text = parseMarkdownToAnnotatedString(content, accentColor),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }

                trimmed.startsWith("## ") -> {
                    inBulletList = false
                    val content = trimmed.removePrefix("## ").trim()
                    Text(
                        text = parseMarkdownToAnnotatedString(content, accentColor),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }

                trimmed.startsWith("# ") -> {
                    inBulletList = false
                    val content = trimmed.removePrefix("# ").trim()
                    Text(
                        text = parseMarkdownToAnnotatedString(content, accentColor),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }

                // Bullet list item (- or *)
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    inBulletList = true
                    val content = trimmed.substring(2).trim()
                    val isLastLine = index == lines.lastIndex
                    val displayContent = if (isStreaming && isLastLine) "$content▍" else content

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = parseMarkdownToAnnotatedString(displayContent, accentColor),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                letterSpacing = 0.15.sp
                            ),
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Numbered list item (e.g. 1. , 2. )
                trimmed.matches(Regex("""^\d+\.\s+.*""")) -> {
                    inBulletList = true
                    val dotIdx = trimmed.indexOf('.')
                    val numberPrefix = trimmed.substring(0, dotIdx + 1)
                    val content = trimmed.substring(dotIdx + 1).trim()
                    val isLastLine = index == lines.lastIndex
                    val displayContent = if (isStreaming && isLastLine) "$content▍" else content

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = numberPrefix,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = parseMarkdownToAnnotatedString(displayContent, accentColor),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                letterSpacing = 0.15.sp
                            ),
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Regular Paragraph
                else -> {
                    inBulletList = false
                    val isLastLine = index == lines.lastIndex
                    val displayContent = if (isStreaming && isLastLine) "$line▍" else line

                    Text(
                        text = parseMarkdownToAnnotatedString(displayContent, accentColor),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            letterSpacing = 0.15.sp
                        ),
                        color = textColor,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Parses inline markdown: **bold**, *italic*, and `inline code`.
 */
fun parseMarkdownToAnnotatedString(
    text: String,
    accentColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length
        while (i < len) {
            // Bold **text**
            if (i + 1 < len && text[i] == '*' && text[i + 1] == '*') {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                } else {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2))
                    }
                    break
                }
            }

            // Inline code `code`
            if (text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = accentColor.copy(alpha = 0.12f),
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                } else {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = accentColor.copy(alpha = 0.12f)
                        )
                    ) {
                        append(text.substring(i + 1))
                    }
                    break
                }
            }

            // Italic *text*
            if (text[i] == '*' && (i + 1 == len || text[i + 1] != '*')) {
                val end = text.indexOf('*', i + 1)
                if (end != -1 && (end + 1 == len || text[end + 1] != '*')) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }

            append(text[i])
            i++
        }
    }
}
