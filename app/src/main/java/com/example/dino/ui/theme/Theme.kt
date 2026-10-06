package com.example.dino.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Earth = Color(0xFF513829)
val Amber = Color(0xFFCA8B32)
val Forest = Earth
val Clay = Color(0xFFB67339)
val Sand = Color(0xFFF5EDDF)
val Ink = Color(0xFF352A23)
val Muted = Color(0xFF8B7967)
val Parchment = Color(0xFFFFF9EF)
val BoneWhite = Color(0xFFE9D6B3)

private val LightColorScheme = lightColorScheme(
    primary = Forest,
    secondary = Amber,
    tertiary = Color(0xFF9B7148),
    primaryContainer = Color(0xFFE8D4B6),
    onPrimaryContainer = Earth,
    secondaryContainer = Color(0xFFF1D9A8),
    onSecondaryContainer = Earth,
    tertiaryContainer = Color(0xFFEDDCCA),
    onTertiaryContainer = Earth,
    onPrimary = Color.White,
    onSecondary = Color.White,
    background = Sand,
    onBackground = Ink,
    surface = Parchment,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEBDFCD),
    onSurfaceVariant = Muted,
    outline = Color(0xFFD6C5AD),
    outlineVariant = Color(0xFFE4D5BF),
    surfaceContainer = Color(0xFFF3E7D6),
    surfaceContainerLow = Color(0xFFF8EFE2),
    surfaceContainerHigh = Color(0xFFEDE0CC)

)

@Composable
fun DinoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
