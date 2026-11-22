package com.devbytes.vixplayer.app.ui.player.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import android.content.res.Configuration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devbytes.vixplayer.app.ui.theme.OnScrim
import com.devbytes.vixplayer.app.ui.theme.ScrimStrong
import com.devbytes.vixplayer.app.ui.theme.SpeedScale
import kotlinx.coroutines.delay
import kotlin.math.abs

/** The selectable playback-speed presets shown as dots on the inline pill. */
val SPEED_PRESETS: List<Float> = listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f, 4.0f)

/**
 * How many preset steps a full screen-width horizontal speed-slide spans. Higher = more
 * sensitive (shorter drag to switch dots). At 15, the whole 6-preset range is covered by
 * dragging ~1/3 of the screen width, and one dot is ~7% of the width.
 */
const val SPEED_DRAG_STEPS_PER_WIDTH = 15f

/** "1×" / "1.5×" — trailing-zero-trimmed, suffixed with the multiplier glyph. */
fun formatSpeed(speed: Float): String {
    val s = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()
    return "${s}×"
}

/** Snaps an arbitrary (e.g. gesture-derived) speed to the nearest preset dot. */
fun nearestSpeedPreset(speed: Float): Float =
    SPEED_PRESETS.minByOrNull { abs(it - speed) } ?: 1f

private const val IDLE_DISMISS_MS = 4000L

/**
 * The inline, over-video speed picker: a compact, centred pill of six dots joined by a track
 * line (cool = slow, warm = fast). The active dot zooms slightly and becomes a coloured
 * "window" showing its speed value inside it; 1× stays neutral. Tap a dot or drag across the
 * track — both apply live. Dismisses on an outside tap or after [IDLE_DISMISS_MS] idle.
 */
@Composable
fun SpeedControlPill(
    visible: Boolean,
    currentSpeed: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Wider in portrait (narrow screen), tighter in landscape (wide screen).
    val widthFraction =
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) 0.65f
        else 0.45f

    // Restart the idle-dismiss timer on open, on each dot tap, and on every speed change
    // (so a gesture slide driving currentSpeed keeps the pill open while the finger moves).
    var interactionKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(visible, interactionKey, currentSpeed) {
        if (visible) {
            delay(IDLE_DISMISS_MS)
            onDismiss()
        }
    }

    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize()) {
                // Tap-through scrim: a tap anywhere outside the pill dismisses.
                Box(Modifier.fillMaxSize().pointerInputTap { onDismiss() })
                PillBody(
                    currentSpeed = currentSpeed,
                    onSelect = { interactionKey++; onSelect(it) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        // Clear the status bar/cutout and the player's top control bar so the
                        // pill never collides with it — the overlap is worst in portrait.
                        .statusBarsPadding()
                        .padding(top = 64.dp)
                        .fillMaxWidth(widthFraction)
                        // Swallow taps on the pill chrome (not a dot) so they don't dismiss.
                        .pointerInputTap { }
                        // Remote / D-pad: ←/→ step through the presets, OK closes.
                        .onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                            val step = when (event.key) {
                                Key.DirectionLeft -> -1
                                Key.DirectionRight -> 1
                                Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { onDismiss(); return@onKeyEvent true }
                                else -> return@onKeyEvent false
                            }
                            val index = SPEED_PRESETS.indexOf(nearestSpeedPreset(currentSpeed))
                            interactionKey++
                            onSelect(SPEED_PRESETS[(index + step).coerceIn(0, SPEED_PRESETS.lastIndex)])
                            true
                        }
                        .focusable(),
                )
            }
        }
    }
}

@Composable
private fun PillBody(
    currentSpeed: Float,
    onSelect: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var rowWidthPx by remember { mutableIntStateOf(1) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(ScrimStrong)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Track line joining the dots, behind them. A gradient of the dot colours so each
            // dot meets the line in its own colour; spans first→last dot centre (each dot sits
            // in a 1/6 cell centred at (i+0.5)/6, so the span is 1/12..11/12 = 10/12) so the
            // line never overflows the edge dots.
            Box(
                Modifier
                    .fillMaxWidth(10f / 12f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(SPEED_PRESETS.map { SpeedScale.colorFor(it) })),
            )
            // Dots — one gesture handler owns both tap and drag-scrub.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { rowWidthPx = it.width.coerceAtLeast(1) }
                    .pointerInput(rowWidthPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            fun pickAt(x: Float) {
                                val idx = ((x / rowWidthPx) * SPEED_PRESETS.size).toInt()
                                    .coerceIn(0, SPEED_PRESETS.lastIndex)
                                onSelect(SPEED_PRESETS[idx])
                            }
                            pickAt(down.position.x)
                            down.consume()
                            drag(down.id) { change -> pickAt(change.position.x); change.consume() }
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SPEED_PRESETS.forEach { preset ->
                    DotCell(
                        preset = preset,
                        selected = abs(preset - currentSpeed) < 0.001f,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Numbers under each dot (kept as-is); the active dot also shows its value inside.
        Row(Modifier.fillMaxWidth()) {
            SPEED_PRESETS.forEach { preset ->
                val selected = abs(preset - currentSpeed) < 0.001f
                Text(
                    text = formatSpeed(preset),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) OnScrim else OnScrim.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DotCell(preset: Float, selected: Boolean, modifier: Modifier = Modifier) {
    val color = SpeedScale.colorFor(preset)
    Box(modifier.height(30.dp), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                (scaleIn(tween(200), initialScale = 0.7f) + fadeIn(tween(200))) togetherWith
                    (scaleOut(tween(160), targetScale = 0.7f) + fadeOut(tween(160)))
            },
            label = "dotState",
        ) { isSelected ->
            if (isSelected) {
                // Active: a slightly-zoomed target — ring + inner dot, both in the dot's own
                // colour so its border matches the connecting line (no accent). The gap reads
                // as the selected state.
                Box(
                    Modifier
                        .size(22.dp)
                        .border(2.5.dp, color, CircleShape)
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            } else {
                Box(Modifier.size(11.dp).clip(CircleShape).background(color))
            }
        }
    }
}

/** A tap-only pointer handler that re-arms per gesture (used for scrim + swallow). */
private fun Modifier.pointerInputTap(onTap: () -> Unit): Modifier =
    this.pointerInput(Unit) { detectTapGestures { onTap() } }
