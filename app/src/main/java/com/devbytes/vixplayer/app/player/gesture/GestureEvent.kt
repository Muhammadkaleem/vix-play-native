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

    /** Cumulative horizontal displacement since scrub start, as a fraction of width (+right). */
    data class SeekScrub(val totalFraction: Float) : GestureEvent

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
