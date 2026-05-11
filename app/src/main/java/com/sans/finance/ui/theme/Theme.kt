package com.sans.finance.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Slate50, // Slate focus
    onPrimary = Slate900,
    primaryContainer = Slate700,
    onPrimaryContainer = Slate50,
    secondary = Slate500,
    secondaryContainer = Slate800,
    onSecondaryContainer = Slate100,
    tertiary = Emerald500,
    tertiaryContainer = Slate900,
    onTertiaryContainer = Slate300,
    error = Rose500,
    background = Slate900,
    surface = Slate800,
    onBackground = Slate50,
    onSurface = Slate50,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate400
)

private val LightColorScheme = lightColorScheme(
    primary = Slate900, // Slate focus
    onPrimary = Color.White,
    primaryContainer = Slate100,
    onPrimaryContainer = Slate900,
    secondary = Slate500,
    secondaryContainer = Slate200,
    onSecondaryContainer = Slate800,
    tertiary = Emerald500,
    tertiaryContainer = Slate50,
    onTertiaryContainer = Slate700,
    error = Rose500,
    background = Slate50,
    surface = Color.White,
    onBackground = Slate900,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500
)

@Composable
fun SansFinanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Strictly follow Slate-based Sans Finance brand
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}