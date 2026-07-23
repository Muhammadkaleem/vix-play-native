package com.devbytes.vixplayer.app.player.gesture

/**
 * Semantic gestures the player surface can emit. Recognized in Compose by
 * [Modifier.playerGestures]; applied to the player/system by PlayerScreen.
 */
sealed interface GestureEvent {
    /** Single tap on the surface — flips the control chrome. */
    data object ToggleControls : GestureEvent

    /** Double tap on the left/right half — jumps ±10s. */
    data class DoubleTapSeek(val side: SeekSide) : GestureEvent

    /** Incremental vertical drag on the right half (+up, −down), as a fraction of height. */
    data class VolumeDrag(val deltaFraction: Float) : GestureEvent

    /** Incremental vertical drag on the left half (+up, −down), as a fraction of height. */
    data class BrightnessDrag(val deltaFraction: Float) : GestureEvent

    /** A horizontal scrub began — capture the current position as the anchor. */
    data object SeekScrubStart : GestureEvent

    /**
     * Cumulative horizontal displacement since scrub start, as a fraction of width (+right),
     * plus how far the finger has strayed vertically from where the drag began (absolute
     * fraction of height) — that distance selects the precision tier.
     */
    data class SeekScrub(val totalFraction: Float, val verticalFraction: Float) : GestureEvent

    /** Horizontal scrub released — commit the previewed target. */
    data object SeekCommit : GestureEvent

    /** Long-press engaged — hold playback at a boosted speed until release. */
    data object SpeedHoldStart : GestureEvent

    /** Cumulative vertical displacement since speed-hold engaged (+up = faster). */
    data class SpeedHoldChange(val totalFraction: Float) : GestureEvent

    /** Speed-hold released — restore the prior speed. */
    data object SpeedHoldEnd : GestureEvent

    /** A vertical drag gesture ended — starts the transient HUD's dismiss timer. */
    data object DragEnd : GestureEvent
}

enum class SeekSide { LEFT, RIGHT }

/**
 * Scrub sensitivity, selected by how far the finger has moved vertically from the drag
 * origin — the PRD's "finger's vertical position sets granularity (coarse/fine/frame)".
 *
 * [rangeMs] is the span a full-width drag covers at that tier. Measured from the *drag
 * origin* rather than absolute screen position, so it behaves the same whether the drag
 * started near the top or the bottom of the surface.
 *
 * Named "Precise" rather than the PRD's "frame": ±3s across a drag width is sub-second
 * targeting, which is the actual need, but it isn't literally frame-accurate stepping.
 */
enum class SeekPrecision(val rangeMs: Long, val label: String) {
    COARSE(120_000L, "Coarse"),
    FINE(20_000L, "Fine"),
    PRECISE(3_000L, "Precise");

    companion object {
        fun fromVerticalFraction(fraction: Float): SeekPrecision = when {
            fraction < 0.30f -> COARSE
            fraction < 0.60f -> FINE
            else -> PRECISE
        }
    }
}
