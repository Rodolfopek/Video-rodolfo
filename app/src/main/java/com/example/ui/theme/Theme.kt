package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = Color.Black,
    primaryContainer = CyanAccentGlow,
    onPrimaryContainer = Color.White,
    secondary = VioletAccent,
    onSecondary = Color.Black,
    secondaryContainer = PlayerSurfaceVariant,
    onSecondaryContainer = Color.White,
    tertiary = EmeraldAccent,
    background = PlayerDarkBg,
    onBackground = TextPrimary,
    surface = PlayerSurface,
    onSurface = TextPrimary,
    surfaceVariant = PlayerSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder
)

@Composable
fun NovaPlayerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
