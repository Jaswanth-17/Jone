package com.example.jone.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme

private val LightColors = lightColorScheme(
    primary = JonePrimary,
    onPrimary = JoneOnPrimary,
    secondary = JoneSecondary,
    background = JoneBackground,
    surface = JoneSurface,
    error = JoneError
)

private val DarkColors = darkColorScheme(
    primary = JonePrimary,
    onPrimary = JoneOnPrimary,
    secondary = JoneSecondary,
    background = JoneDarkBackground,
    surface = JoneDarkSurface,
    error = JoneError
)

@Composable
fun JoneTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
