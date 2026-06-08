package com.haji.racing.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RacingRed = Color(0xFFD32F2F)
private val RacingRedLight = Color(0xFFFF6659)
private val RacingRedDark = Color(0xFF9A0007)

private val RacingDark = Color(0xFF1A1A2E)
private val RacingDarkSurface = Color(0xFF16213E)
private val RacingDarkCard = Color(0xFF0F3460)

private val LightColorScheme = lightColorScheme(
    primary = RacingRed,
    onPrimary = Color.White,
    primaryContainer = RacingRedLight,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF424242),
    onSecondary = Color.White,
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF49454F),
)

private val DarkColorScheme = darkColorScheme(
    primary = RacingRed,
    onPrimary = Color.White,
    primaryContainer = RacingRedDark,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFBB86FC),
    onSecondary = Color.Black,
    background = RacingDark,
    onBackground = Color.White,
    surface = RacingDarkSurface,
    onSurface = Color.White,
    surfaceVariant = RacingDarkCard,
    onSurfaceVariant = Color(0xFFCAC4D0),
)

@Composable
fun HajiRacingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
