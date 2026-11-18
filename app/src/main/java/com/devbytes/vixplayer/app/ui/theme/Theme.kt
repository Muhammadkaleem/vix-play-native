package com.devbytes.vixplayer.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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

private val VixPlayAmoledColors = VixPlayDarkColors.copy(
    background = AmoledBackground,
    surface = AmoledBackground,
    surfaceVariant = AmoledSheet,
)

private val VixPlayLightColors = lightColorScheme(
    primary = LightAccent,
    onPrimary = OnAccent,
    background = LightBackground,
    onBackground = LightContentPrimary,
    surface = LightSurface,
    onSurface = LightContentPrimary,
    surfaceVariant = LightSheet,
    onSurfaceVariant = LightContentSecondary,
    error = Error,
    onError = OnAccent,
)

/**
 * Resolves the active [ColorScheme] from the user's [mode] and [accent].
 *
 * DYNAMIC uses Material You on API 31+ (following the system light/dark), falling back to
 * the static schemes below that. An explicit [accent] overrides `primary` in whatever
 * scheme is chosen — including Dynamic's wallpaper primary, per the theming rule.
 */
@Composable
private fun resolveColorScheme(mode: ThemeMode, accent: AccentChoice): ColorScheme {
    val systemDark = isSystemInDarkTheme()
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current

    val base = when (mode) {
        ThemeMode.LIGHT -> VixPlayLightColors
        ThemeMode.DARK -> VixPlayDarkColors
        ThemeMode.AMOLED -> VixPlayAmoledColors
        ThemeMode.SYSTEM -> if (systemDark) VixPlayDarkColors else VixPlayLightColors
        ThemeMode.DYNAMIC -> when {
            dynamicAvailable && systemDark -> dynamicDarkColorScheme(context)
            dynamicAvailable -> dynamicLightColorScheme(context)
            systemDark -> VixPlayDarkColors
            else -> VixPlayLightColors
        }
    }

    return accent.color?.let { base.copy(primary = it, onPrimary = Color.White) } ?: base
}

/** True when the resolved scheme is dark, so system-bar icons can be set accordingly. */
@Composable
private fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK, ThemeMode.AMOLED -> true
    ThemeMode.SYSTEM, ThemeMode.DYNAMIC -> isSystemInDarkTheme()
}

@Composable
fun VixPlayTheme(
    mode: ThemeMode = ThemeMode.LIGHT,
    accent: AccentChoice = AccentChoice.DEFAULT,
    content: @Composable () -> Unit,
) {
    val colorScheme = resolveColorScheme(mode, accent)
    val dark = mode.isDark()

    // Flip status/nav-bar icon appearance so they stay legible in light mode. Guarded to
    // the real window (skipped in @Preview).
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = !dark
            insets.isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PlayerTypography,
        content = content,
    )
}
