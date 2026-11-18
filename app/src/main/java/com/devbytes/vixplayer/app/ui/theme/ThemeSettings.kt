package com.devbytes.vixplayer.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Base theme selection. Flat set (AMOLED is its own mode, not a modifier) so the picker
 * and its persistence stay a single choice. [DYNAMIC] is Material You — offered only on
 * API 31+, falling back to static Light/Dark below that.
 */
enum class ThemeMode(val label: String, val description: String) {
    SYSTEM("System", "Follow the device light/dark setting"),
    LIGHT("Light", "Bright surfaces"),
    DARK("Dark", "The default dark theme"),
    AMOLED("AMOLED", "Pure black — saves power on OLED screens"),
    DYNAMIC("Material You", "Colors from your wallpaper"),
}

/**
 * Accent applied to `primary`. [DEFAULT] means "let the theme decide" — the wallpaper
 * primary in [ThemeMode.DYNAMIC], the brand violet otherwise. Any explicit choice
 * overrides even Dynamic's derived primary (the PRD rule). Each swatch is picked to stay
 * legible with white text on both light and dark surfaces.
 */
enum class AccentChoice(val label: String, val color: Color?) {
    DEFAULT("Default", null),
    VIOLET("Violet", Color(0xFF8B5BFF)),
    INDIGO("Indigo", Color(0xFF5B7CFF)),
    TEAL("Teal", Color(0xFF13A594)),
    GREEN("Green", Color(0xFF3F9E5E)),
    AMBER("Amber", Color(0xFFE08A2B)),
    ROSE("Rose", Color(0xFFE0466E)),
}

/** The two persisted theming choices, read together so the theme resolves in one place. */
data class ThemeState(
    val mode: ThemeMode = ThemeMode.LIGHT,
    val accent: AccentChoice = AccentChoice.DEFAULT,
)
