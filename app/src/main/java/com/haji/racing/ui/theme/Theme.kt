package com.haji.racing.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = RacingPrimary,
    onPrimary = RacingOnPrimary,
    primaryContainer = RacingPrimaryDark,
    onPrimaryContainer = Color(0xFFFFD9CB),
    secondary = RacingAccent,
    onSecondary = Color(0xFF04262F),
    secondaryContainer = Color(0xFF123A4A),
    onSecondaryContainer = Color(0xFFBEE7F7),
    tertiary = RacingStart,
    onTertiary = Color(0xFF00210F),
    background = RacingBg,
    onBackground = TextPrimary,
    surface = RacingSurface,
    onSurface = TextPrimary,
    surfaceVariant = RacingCard,
    onSurfaceVariant = TextSecondary,
    outline = RacingOutline,
    error = RacingEnd,
    onError = Color.White,
)

/** 强制暗色赛车主题 */
@Composable
fun HajiRacingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = RacingTypography,
        content = content,
    )
}
