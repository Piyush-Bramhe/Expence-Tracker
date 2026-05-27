package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ElegantPurple,
    onPrimary = ElegantDeepPurple,
    primaryContainer = ElegantContainerPurple,
    onPrimaryContainer = Color(0xFFE8DDFF),
    secondary = ElegantPurple,
    background = DarkBg,
    surface = DarkSurface,
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6),
    outline = DarkBorder,
    error = ExpenseCoral
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldLightContainer,
    secondary = AccentOrange,
    background = LightBg,
    surface = LightSurface,
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827),
    outline = LightBorder,
    error = ExpenseCoral
)

@Composable
fun SpendWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
