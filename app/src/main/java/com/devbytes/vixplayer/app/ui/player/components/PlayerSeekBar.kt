package com.devbytes.vixplayer.app.ui.player.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.devbytes.vixplayer.app.ui.theme.OnScrim
import com.devbytes.vixplayer.app.ui.theme.TimecodeStyle
import com.devbytes.vixplayer.app.ui.theme.TrackInactive

@Composable
fun PlayerSeekBar(
    positionMs: Long,
    bufferedMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    onScrubbingChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (durationMs <= 0L) return

    val fraction = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    val bufferedFraction = (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f)
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(fraction) }
    val displayFraction = if (isDragging) dragFraction else fraction
    val accent = MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                // Comfortable scrub target; the drawn track stays 4dp, centered.
                .height(32.dp)
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            onScrubbingChange(true)
                            dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            isDragging = false
                            onScrubbingChange(false)
                            onSeek((dragFraction * durationMs).toLong())
                        },
                        onDragCancel = {
                            isDragging = false
                            onScrubbingChange(false)
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragFraction = (dragFraction + dragAmount / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                    )
                },
        ) {
            val trackY = size.height / 2f
            val trackH = 4.dp.toPx()
            val r = CornerRadius(trackH / 2f)

            // Background track
            drawRoundRect(
                color = TrackInactive,
                topLeft = Offset(0f, trackY - trackH / 2f),
                size = Size(size.width, trackH),
                cornerRadius = r,
            )
            // Buffered
            drawRoundRect(
                color = OnScrim.copy(alpha = 0.35f),
                topLeft = Offset(0f, trackY - trackH / 2f),
                size = Size(size.width * bufferedFraction, trackH),
                cornerRadius = r,
            )
            // Progress
            drawRoundRect(
                color = accent,
                topLeft = Offset(0f, trackY - trackH / 2f),
                size = Size(size.width * displayFraction, trackH),
                cornerRadius = r,
            )
            // Thumb
            drawCircle(
                color = OnScrim,
                radius = if (isDragging) 8.dp.toPx() else 6.dp.toPx(),
                center = Offset(size.width * displayFraction, trackY),
            )
        }

        Spacer(Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
            Text(
                text = formatTimeMs(if (isDragging) (dragFraction * durationMs).toLong() else positionMs),
                style = TimecodeStyle,
                color = OnScrim.copy(alpha = 0.8f),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = formatTimeMs(durationMs),
                style = TimecodeStyle,
                color = OnScrim.copy(alpha = 0.8f),
            )
        }
    }
}

internal fun formatTimeMs(ms: Long): String {
    val s = ms / 1000
    val m = s / 60
    val h = m / 60
    return if (h > 0) "%d:%02d:%02d".format(h, m % 60, s % 60)
    else "%d:%02d".format(m, s % 60)
}
