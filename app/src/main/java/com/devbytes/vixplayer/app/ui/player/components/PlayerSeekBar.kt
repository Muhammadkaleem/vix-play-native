package com.devbytes.vixplayer.app.ui.player.components

import android.graphics.Bitmap
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.devbytes.vixplayer.app.ui.theme.OnScrim
import androidx.compose.ui.graphics.Color
import com.devbytes.vixplayer.app.ui.theme.ScrimStrong
import com.devbytes.vixplayer.app.ui.theme.TimecodeStyle

@Composable
fun PlayerSeekBar(
    positionMs: Long,
    bufferedMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    onScrubbingChange: (Boolean) -> Unit = {},
    frameLoader: (suspend (Long) -> Bitmap?)? = null,
    // Over video by default; a surface host (the audio player) passes its own content color.
    timecodeColor: Color = OnScrim.copy(alpha = 0.8f),
    /**
     * Seeks by a signed amount. When set, the bar takes remote / D-pad focus and ←/→ step it by
     * [KEY_STEP_MS] (held keys repeat).
     */
    onStep: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (durationMs <= 0L) return

    val fraction = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    val bufferedFraction = (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f)
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(fraction) }
    val displayFraction = if (isDragging) dragFraction else fraction
    val accent = MaterialTheme.colorScheme.primary
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val active = isDragging || focused

    val previewMs = (dragFraction * durationMs).toLong()
    var previewFrame by remember { mutableStateOf<Bitmap?>(null) }
    // Same 5s bucketing as the surface scrub; cancellation gives latest-wins.
    LaunchedEffect(isDragging, previewMs / 5_000L, frameLoader) {
        previewFrame = if (isDragging && frameLoader != null) frameLoader(previewMs) else null
    }

    Column(modifier = modifier) {
        // Preview rides above the thumb, clamped so it never runs off either edge.
        if (isDragging && frameLoader != null) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cardWidth = 172.dp
                val maxOffset = (maxWidth - cardWidth).coerceAtLeast(0.dp)
                val offset = (maxWidth * displayFraction - cardWidth / 2)
                    .coerceIn(0.dp, maxOffset)
                ScrubPreview(
                    frame = previewFrame,
                    targetMs = previewMs,
                    modifier = Modifier
                        .offset(x = offset)
                        .padding(bottom = 8.dp),
                )
            }
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                // Comfortable scrub target; the drawn track stays 4dp, centered.
                .height(32.dp)
                .then(
                    if (onStep == null) Modifier
                    else Modifier
                        .onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                            when (event.key) {
                                Key.DirectionLeft -> { onStep(-KEY_STEP_MS); true }
                                Key.DirectionRight -> { onStep(KEY_STEP_MS); true }
                                else -> false
                            }
                        }
                        .focusable(interactionSource = interaction),
                )
                // One handler for both tap and drag: on press we jump to the touch's
                // absolute x (so a tap seeks to that point), track the finger while held,
                // and commit the seek on release. Absolute-position (not accumulated
                // delta) is what makes tap-to-seek land where you tapped.
                .pointerInput(durationMs) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isDragging = true
                        onScrubbingChange(true)
                        dragFraction = (down.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            dragFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            change.consume()
                            if (!change.pressed) break
                        }
                        isDragging = false
                        onScrubbingChange(false)
                        onSeek((dragFraction * durationMs).toLong())
                    }
                },
        ) {
            val trackY = size.height / 2f
            // Slight capsule at rest; thickens while actively seeking or focused.
            val trackH = (if (active) 9.dp else 5.dp).toPx()
            val r = CornerRadius(trackH / 2f)

            // Background capsule track (dark, like the gesture pill track)
            drawRoundRect(
                color = ScrimStrong,
                topLeft = Offset(0f, trackY - trackH / 2f),
                size = Size(size.width, trackH),
                cornerRadius = r,
            )
            // Buffered
            drawRoundRect(
                color = OnScrim.copy(alpha = 0.3f),
                topLeft = Offset(0f, trackY - trackH / 2f),
                size = Size(size.width * bufferedFraction, trackH),
                cornerRadius = r,
            )
            // Progress — white fill, matching the pill sliders
            drawRoundRect(
                color = OnScrim,
                topLeft = Offset(0f, trackY - trackH / 2f),
                size = Size(size.width * displayFraction, trackH),
                cornerRadius = r,
            )
            // Thumb — accent, legible on both the white fill and the dark track
            val thumb = Offset(size.width * displayFraction, trackY)
            // Focused: a white ring marks the thumb as the remote's target.
            if (focused) drawCircle(color = OnScrim, radius = 11.dp.toPx(), center = thumb)
            drawCircle(
                color = accent,
                radius = if (active) 8.dp.toPx() else 6.dp.toPx(),
                center = thumb,
            )
        }

        Spacer(Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
            Text(
                text = formatTimeMs(if (isDragging) (dragFraction * durationMs).toLong() else positionMs),
                style = TimecodeStyle,
                color = timecodeColor,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = formatTimeMs(durationMs),
                style = TimecodeStyle,
                color = timecodeColor,
            )
        }
    }
}

/** How far one ←/→ press moves a focused seek bar. */
const val KEY_STEP_MS = 10_000L

internal fun formatTimeMs(ms: Long): String {
    val s = ms / 1000
    val m = s / 60
    val h = m / 60
    return if (h > 0) "%d:%02d:%02d".format(h, m % 60, s % 60)
    else "%d:%02d".format(m, s % 60)
}
