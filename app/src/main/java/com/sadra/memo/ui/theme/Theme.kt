package com.sadra.memo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B2A5C),
    onPrimary = Color.White,
    secondary = Color(0xFF8E7CC3),
    onSecondary = Color.White,
    tertiary = Color(0xFF2979FF),
    onTertiary = Color.White,
    background = Color(0xFFF5F6FA),
    onBackground = Color(0xFF12172B),
    surface = Color.White,
    onSurface = Color(0xFF12172B),
    surfaceVariant = Color(0xFFE8EAF3),
    onSurfaceVariant = Color(0xFF454B66)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FA8FF),
    onPrimary = Color(0xFF0B1020),
    secondary = Color(0xFFB7A8E6),
    onSecondary = Color(0xFF0B1020),
    tertiary = Color(0xFF6FA0FF),
    onTertiary = Color(0xFF0B1020),
    background = Color(0xFF0B1020),
    onBackground = Color(0xFFE6E9F5),
    surface = Color(0xFF131A30),
    onSurface = Color(0xFFE6E9F5),
    surfaceVariant = Color(0xFF1E2744),
    onSurfaceVariant = Color(0xFFB4BAD4)
)

@Composable
fun MemoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
