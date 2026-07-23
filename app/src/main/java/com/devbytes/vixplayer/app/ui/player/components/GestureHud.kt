package com.devbytes.vixplayer.app.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.ui.AspectRatioFrameLayout
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.ui.theme.DarkAccent
import com.devbytes.vixplayer.app.ui.theme.OnScrim
import com.devbytes.vixplayer.app.ui.theme.ScrimStrong
import com.devbytes.vixplayer.app.ui.theme.TimecodeStyle
import kotlin.math.abs

/** Which system value the transient drag HUD is reporting. */
enum class GestureHudKind { BRIGHTNESS, VOLUME }

/**
 * Video surface scaling, mapped 1:1 to a Media3 [AspectRatioFrameLayout] resize mode.
 * FIT letterboxes (no distortion), CROP fills by cropping (preserves aspect),
 * STRETCH fills by distorting. Cycles in declaration order.
 */
enum class AspectMode(val label: String, val resizeMode: Int) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    CROP("Crop", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    STRETCH("Stretch", AspectRatioFrameLayout.RESIZE_MODE_FILL);

    fun next(): AspectMode = entries[(ordinal + 1) % entries.size]
}

/**
 * User orientation preference for the player. AUTO follows the video's shape
 * (via the sensor, flippable within the axis); LANDSCAPE/PORTRAIT hard-pin the
 * device, overriding the sensor. Cycles in declaration order.
 */
enum class OrientationMode(val label: String) {
    AUTO("Auto"),
    LANDSCAPE("Landscape"),
    PORTRAIT("Portrait");

    fun next(): OrientationMode = entries[(ordinal + 1) % entries.size]
}

/**
 * Centered transient pill shown while a vertical drag adjusts brightness/volume.
 * Orthogonal to the persistent control chrome — it fades on its own dismiss
 * timer and never touches [controlsVisible]. [fraction] is 0f..1f.
 */
@Composable
fun GestureHud(
    kind: GestureHudKind?,
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = kind != null, enter = fadeIn(), exit = fadeOut()) {
            val f = fraction.coerceIn(0f, 1f)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(ScrimStrong)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val icon = when (kind) {
                    GestureHudKind.VOLUME -> if (f <= 0f) R.drawable.ic_mute else R.drawable.ic_volume
                    GestureHudKind.BRIGHTNESS, null -> R.drawable.ic_brightness
                }
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = OnScrim,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(14.dp))
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(OnScrim.copy(alpha = 0.25f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(DarkAccent),
                    )
                }
            }
        }
    }
}

/**
 * Centered transient pill shown while a horizontal drag scrubs the timeline.
 * Preview-only: the seek commits on release. Shows the target timecode and the
 * signed delta from the anchor position.
 */
@Composable
fun SeekScrubHud(
    visible: Boolean,
    targetMs: Long,
    deltaMs: Long,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(ScrimStrong)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatTimeMs(targetMs),
                    style = MaterialTheme.typography.titleMedium,
                    color = OnScrim,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = formatSignedSeconds(deltaMs),
                    style = TimecodeStyle,
                    color = DarkAccent,
                )
            }
        }
    }
}

private fun formatSignedSeconds(ms: Long): String {
    val s = abs(ms) / 1000
    return if (ms < 0) "−${s}s" else "+${s}s"
}

/**
 * Centered transient pill confirming the current [AspectMode] after a cycle.
 * Text-only, matching [SeekScrubHud]'s language; dismisses on its own timer.
 */
@Composable
fun AspectHud(
    visible: Boolean,
    mode: AspectMode,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(ScrimStrong)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnScrim,
                )
            }
        }
    }
}

/**
 * Centered transient pill confirming the current [OrientationMode] after a cycle.
 * Confirms the picked mode even when it triggers no visible rotation (e.g. forcing
 * Landscape on an already-landscape video); dismisses on its own timer.
 */
@Composable
fun OrientationHud(
    visible: Boolean,
    mode: OrientationMode,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(ScrimStrong)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_rotate),
                    contentDescription = null,
                    tint = DarkAccent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnScrim,
                )
            }
        }
    }
}

/**
 * Centered transient pill confirming a sleep-timer change (armed / off) or the
 * pause it triggers. Text is composed by the caller; dismisses on its own timer.
 */
@Composable
fun SleepTimerHud(
    visible: Boolean,
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(ScrimStrong)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_timer),
                    contentDescription = null,
                    tint = DarkAccent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnScrim,
                )
            }
        }
    }
}

/**
 * Centered transient pill confirming a screenshot capture (saved / failed /
 * permission needed). Text is composed by the caller; dismisses on its own timer.
 */
@Composable
fun ScreenshotHud(
    visible: Boolean,
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(ScrimStrong)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_screenshot),
                    contentDescription = null,
                    tint = DarkAccent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnScrim,
                )
            }
        }
    }
}

/**
 * Centered transient pill shown while a long-press speed-hold is active.
 * Reports the live multiplier; dismisses on release.
 */
@Composable
fun SpeedHud(
    visible: Boolean,
    multiplier: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier
                    .padding(top = 72.dp)
                    .clip(RoundedCornerShape(50))
                    .background(ScrimStrong)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_speed),
                    contentDescription = null,
                    tint = DarkAccent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "%.1f×".format(multiplier),
                    style = TimecodeStyle,
                    color = OnScrim,
                )
            }
        }
    }
}
