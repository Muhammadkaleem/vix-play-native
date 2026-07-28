package com.devbytes.vixplayer.app.player.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
 *
 * When [zoomed] is true (pinch-zoom active), a one-finger drag instead pans the
 * viewport as free 2D [GestureEvent.Pan] — volume/brightness/seek and speed-hold
 * are suspended until zoom returns to 1x (double-tap resets it). [zoomed] is read
 * live via [rememberUpdatedState], so crossing 1x never restarts the detectors.
 */
@Composable
fun Modifier.playerGestures(
    enabled: Boolean,
    zoomed: Boolean,
    onEvent: (GestureEvent) -> Unit,
): Modifier {
    val speedActive = remember { mutableStateOf(false) }
    val zoomedState = rememberUpdatedState(zoomed)
    return this
        // Pinch-zoom. Placed first, and only acts while two or more fingers are down —
        // it consumes nothing at one finger, so the verified single-finger loop below is
        // never starved and the two gestures can't contend.
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                var prevDistance = 0f
                while (true) {
                    val event = awaitPointerEvent()
                    val pts = event.changes.filter { it.pressed }
                    if (pts.size < 2) {
                        prevDistance = 0f
                        if (pts.isEmpty()) break else continue
                    }
                    val (a, b) = pts
                    val dist = (a.position - b.position).getDistance()
                    if (prevDistance > 0f && dist > 0f) {
                        onEvent(GestureEvent.Zoom(dist / prevDistance))
                    }
                    prevDistance = dist
                    // Consume so the single-finger loop doesn't also interpret these.
                    pts.forEach { it.consume() }
                }
            }
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onTap = { onEvent(GestureEvent.ToggleControls) },
                onDoubleTap = { offset ->
                    val side = if (offset.x < size.width / 2f) SeekSide.LEFT else SeekSide.RIGHT
                    onEvent(GestureEvent.DoubleTapSeek(side))
                },
                onLongPress = {
                    // Speed-hold is a drag gesture, so it's suspended while zoomed (the
                    // pointer belongs to panning); a long-press then does nothing.
                    if (!zoomedState.value) {
                        speedActive.value = true
                        onEvent(GestureEvent.SpeedHoldStart)
                    }
                },
            )
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            val edgePx = EDGE_EXCLUSION.toPx()
            val slop = viewConfiguration.touchSlop
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)

                // Zoomed: one finger pans the viewport (free 2D, no axis lock). Decided at
                // touch-down — a single-finger drag can't change the zoom state mid-gesture.
                if (zoomedState.value) {
                    var last = down.position
                    var panned = false
                    while (true) {
                        val event = awaitPointerEvent()
                        // A second finger means a pinch — let the pinch detector own it.
                        if (event.changes.count { it.pressed } >= 2) break
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val delta = change.position - last
                        last = change.position
                        if (delta != Offset.Zero) {
                            onEvent(GestureEvent.Pan(delta.x, delta.y))
                            panned = true
                            change.consume()
                        }
                    }
                    if (panned) onEvent(GestureEvent.DragEnd)
                    return@awaitEachGesture
                }

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
                                // Report which side; PlayerScreen resolves it to an action
                                // via the user's bindings. The detection stays binding-blind.
                                val side = if (down.position.x < size.width / 2f) {
                                    SeekSide.LEFT
                                } else {
                                    SeekSide.RIGHT
                                }
                                onEvent(GestureEvent.VerticalDrag(side, frac))
                            }
                            DragAxis.HORIZONTAL -> {
                                scrubTotal += delta.x / size.width
                                // Vertical stray from the drag origin picks the precision tier.
                                val vertical =
                                    abs(change.position.y - down.position.y) / size.height
                                onEvent(GestureEvent.SeekScrub(scrubTotal, vertical))
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
