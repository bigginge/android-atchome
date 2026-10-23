package com.atchome.atc.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun AtcHomeTheme(themeMode: String = "SYSTEM", content: @Composable () -> Unit) {
    val useDark = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }
    val palette = if (useDark) DarkAtcPalette else LightAtcPalette
    val scheme = if (useDark) {
        darkColorScheme(
            primary = palette.highlight, onPrimary = palette.radarBackground,
            secondary = palette.textSecondary, onSecondary = palette.textPrimary,
            background = palette.radarBackground, onBackground = palette.textPrimary,
            surface = palette.infoBackground, onSurface = palette.textPrimary
        )
    } else {
        lightColorScheme(
            primary = palette.highlight, onPrimary = palette.cardBackground,
            secondary = palette.textSecondary, onSecondary = palette.textPrimary,
            background = palette.radarBackground, onBackground = palette.textPrimary,
            surface = palette.infoBackground, onSurface = palette.textPrimary
        )
    }
    CompositionLocalProvider(LocalAtcPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
    }
}
