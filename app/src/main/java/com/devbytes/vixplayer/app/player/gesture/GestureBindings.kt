package com.devbytes.vixplayer.app.player.gesture

/**
 * What a vertical drag on one half of the surface does. Seek is deliberately absent —
 * it's a horizontal gesture, and mapping a vertical drag to it would be incoherent.
 */
enum class DragAction(val label: String) {
    BRIGHTNESS("Brightness"),
    VOLUME("Volume"),
    NONE("None"),
}

/** What a double-tap on one side does. The side already sets the direction. */
enum class TapAction(val label: String, val skipMs: Long) {
    SKIP_10("Skip 10s", 10_000L),
    SKIP_30("Skip 30s", 30_000L),
    NONE("None", 0L),
}

/**
 * User-remappable gesture assignments for the four configurable slots. The horizontal
 * scrub (seek), long-press speed-hold and pinch-zoom are fixed and not represented here.
 *
 * Defaults reproduce the app's original hardcoded behaviour, so an untouched install
 * behaves exactly as before remapping existed.
 */
data class GestureBindings(
    val leftDrag: DragAction = DragAction.BRIGHTNESS,
    val rightDrag: DragAction = DragAction.VOLUME,
    val leftTap: TapAction = TapAction.SKIP_10,
    val rightTap: TapAction = TapAction.SKIP_10,
) {
    fun dragFor(side: SeekSide): DragAction =
        if (side == SeekSide.LEFT) leftDrag else rightDrag

    fun tapFor(side: SeekSide): TapAction =
        if (side == SeekSide.LEFT) leftTap else rightTap

    companion object {
        val DEFAULT = GestureBindings()
    }
}
