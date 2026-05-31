package com.example.procrastination_detection.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = TextLight,
    onSurface = TextLight,
    error = ProcrastinatingLight,
    tertiary = WarningLight,
    surfaceVariant = Color(0xFFF1F5F9), // Slate-100 for light card panels
    onSurfaceVariant = Color(0xFF475569) // Slate-600 for light secondary text
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = TextDark,
    onSurface = TextDark,
    error = ProcrastinatingDark,
    tertiary = WarningDark,
    surfaceVariant = Color(0xFF1E293B), // Slate-800 for dark card panels
    onSurfaceVariant = Color(0xFF94A3B8) // Slate-400 for dark secondary text
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Automatically detects OS setting!
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}