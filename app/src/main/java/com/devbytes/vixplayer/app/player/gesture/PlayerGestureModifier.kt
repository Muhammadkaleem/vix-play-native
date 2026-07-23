package com.devbytes.vixplayer.app.player.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private enum class DragAxis { HORIZONTAL, VERTICAL }

/** Drags that start within this inset from the left/right edge are ignored so
 *  horizontal scrubs don't collide with the system back-gesture. */
private val EDGE_EXCLUSION = 24.dp

/**
 * Single owner of all player-surface pointer input.
 *
 * Tap/double-tap/long-press ride the canonical [detectTapGestures] detector.
 * Long-press flips [speedActive]; the parallel drag loop reads that flag so a
 * hold-then-drag scrubs playback speed, while a quick drag (which cancels the
 * long-press before it fires) axis-locks to volume/brightness/seek instead.
 * The drag loop consumes moves, so a committed drag never double-fires a tap.
 */
@Composable
fun Modifier.playerGestures(
    enabled: Boolean,
    onEvent: (GestureEvent) -> Unit,
): Modifier {
    val speedActive = remember { mutableStateOf(false) }
    return this
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onTap = { onEvent(GestureEvent.ToggleControls) },
                onDoubleTap = { offset ->
                    val side = if (offset.x < size.width / 2f) SeekSide.LEFT else SeekSide.RIGHT
                    onEvent(GestureEvent.DoubleTapSeek(side))
                },
                onLongPress = {
                    speedActive.value = true
                    onEvent(GestureEvent.SpeedHoldStart)
                },
            )
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            val edgePx = EDGE_EXCLUSION.toPx()
            val slop = viewConfiguration.touchSlop
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var axis: DragAxis? = null
                var last = down.position
                var scrubTotal = 0f
                var speedTotal = 0f
                var dragged = false
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break

                    // Speed-hold takes over the pointer once long-press engages —
                    // vertical movement varies the multiplier, no axis lock.
                    if (speedActive.value) {
                        val delta = change.position - last
                        last = change.position
                        speedTotal += -delta.y / size.height
                        onEvent(GestureEvent.SpeedHoldChange(speedTotal))
                        dragged = true
                        change.consume()
                        continue
                    }

                    if (axis == null) {
                        val dx = change.position.x - down.position.x
                        val dy = change.position.y - down.position.y
                        if (abs(dx) > slop || abs(dy) > slop) {
                            val horizontal = abs(dx) > abs(dy)
                            // Abandon horizontal drags that begin in the edge gutter.
                            if (horizontal &&
                                (down.position.x < edgePx || down.position.x > size.width - edgePx)
                            ) break
                            axis = if (horizontal) DragAxis.HORIZONTAL else DragAxis.VERTICAL
                            last = change.position
                            if (axis == DragAxis.HORIZONTAL) onEvent(GestureEvent.SeekScrubStart)
                        }
                    } else {
                        val delta = change.position - last
                        last = change.position
                        when (axis) {
                            DragAxis.VERTICAL -> {
                                val frac = -delta.y / size.height
                                if (down.position.x < size.width / 2f) {
                                    onEvent(GestureEvent.BrightnessDrag(frac))
                                } else {
                                    onEvent(GestureEvent.VolumeDrag(frac))
                                }
                            }
                            DragAxis.HORIZONTAL -> {
                                scrubTotal += delta.x / size.width
                                onEvent(GestureEvent.SeekScrub(scrubTotal))
                            }
                        }
                        dragged = true
                        change.consume()
                    }
                }
                when {
                    speedActive.value -> {
                        onEvent(GestureEvent.SpeedHoldEnd)
                        speedActive.value = false
                    }
                    dragged && axis == DragAxis.HORIZONTAL -> onEvent(GestureEvent.SeekCommit)
                    dragged -> onEvent(GestureEvent.DragEnd)
                }
            }
        }
}
