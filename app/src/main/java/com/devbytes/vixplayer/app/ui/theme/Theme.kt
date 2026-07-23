package com.devbytes.vixplayer.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VixPlayDarkColors = darkColorScheme(
    primary = DarkAccent,
    onPrimary = OnAccent,
    background = DarkBackground,
    onBackground = DarkContentPrimary,
    surface = DarkSurface,
    onSurface = DarkContentPrimary,
    surfaceVariant = DarkSheet,
    onSurfaceVariant = DarkContentSecondary,
    error = Error,
    onError = OnAccent,
)

@Composable
fun VixPlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VixPlayDarkColors,
        typography = PlayerTypography,
        content = content,
    )
}
