package com.devbytes.vixplayer.app.ui.theme

import androidx.compose.ui.graphics.Color

// Brand gradient
val BrandStart = Color(0xFF6D5BFF)
val BrandEnd   = Color(0xFFB24BFF)

// Dark (default)
val DarkBackground       = Color(0xFF121316)
val DarkSurface          = Color(0xFF16171C)
val DarkSheet            = Color(0xFF1C1D22)
val DarkOverlay          = Color(0xFF1A1B1F)
val DarkContentPrimary   = Color(0xFFF2F3F5)
val DarkContentSecondary = Color(0xFFA0A3AD)
val DarkContentDisabled  = Color(0xFF565863)
val DarkAccent           = Color(0xFF8B5BFF)
val DarkAccentPressed    = Color(0xFF6D5BFF)
val OnAccent             = Color(0xFFFFFFFF)

// AMOLED overrides
val AmoledBackground = Color(0xFF000000)
val AmoledSheet      = Color(0xFF0A0A0A)

// Light
val LightBackground       = Color(0xFFF7F7F8)
val LightSurface          = Color(0xFFFFFFFF)
val LightSheet            = Color(0xFFEDEDF2)
val LightContentPrimary   = Color(0xFF14151A)
val LightContentSecondary = Color(0xFF5B5E6B)
val LightAccent           = Color(0xFF7A4BFF)

// State
val Success = Color(0xFF3FBE72)
val Warning = Color(0xFFF0B455)
val Error   = Color(0xFFFF6369)

// Overlay / scrim — used over thumbnail imagery (theme-independent by design)
val ScrimStrong   = Color(0xB8000000) // black 72% — duration badge / label backing
val OnScrim       = Color(0xFFFFFFFF) // text/icons on a scrim
val TrackInactive = Color(0x40FFFFFF) // inactive resume/progress track

// Speed-scale — a diverging data-viz palette for the over-video speed picker dots.
// Cool (slow) → neutral white at 1× → warm (fast), so you read "slow vs fast" by colour
// temperature. Like the scrim tokens above, it's theme-independent by design: the dots
// always render on the dark player scrim, so the hues are fixed, not light/dark variants.
object SpeedScale {
    private val stops: List<Pair<Float, Color>> = listOf(
        0.25f to Color(0xFF4F9DFF), // deep blue   — slowest
        0.5f  to Color(0xFF52C4FF), // sky blue
        1.0f  to OnScrim,           // neutral white — normal speed
        1.5f  to Color(0xFFB24BFF), // violet (brand end)
        2.0f  to Color(0xFFFF5BA3), // pink / magenta
        4.0f  to Color(0xFFFF5B5B), // red         — fastest
    )

    /** The scale colour for [speed], matched to the nearest known stop. */
    fun colorFor(speed: Float): Color =
        stops.minByOrNull { kotlin.math.abs(it.first - speed) }!!.second
}
