package com.devbytes.vixplayer.app.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Transient vertical pill slider shown while a vertical drag adjusts brightness/volume
 * (One UI quick-panel style: dark capsule track, white fill rising from the bottom, with the
 * type glyph seated inside the track at the bottom). Side-anchored to the matching gesture
 * zone — brightness on the left, volume on the right. Orthogonal to the persistent control
 * chrome: it fades on its own dismiss timer and never touches [controlsVisible]. 0f..1f.
 */
@Composable
fun GestureHud(
    kind: GestureHudKind?,
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    // Hold the last side so the exit fade stays anchored to the same edge — deriving
    // alignment from the live (nulling) kind made the HUD jump to centre mid-dismiss.
    var lastKind by remember { mutableStateOf(GestureHudKind.BRIGHTNESS) }
    if (kind != null) lastKind = kind
    val alignment = when (lastKind) {
        GestureHudKind.BRIGHTNESS -> Alignment.CenterStart
        GestureHudKind.VOLUME -> Alignment.CenterEnd
    }
    Box(modifier = modifier.fillMaxSize(), contentAlignment = alignment) {
        AnimatedVisibility(visible = kind != null, enter = fadeIn(), exit = fadeOut()) {
            VerticalPillSlider(
                kind = lastKind,
                fraction = fraction.coerceIn(0f, 1f),
                modifier = Modifier.padding(horizontal = 36.dp),
            )
        }
    }
}

@Composable
private fun VerticalPillSlider(
    kind: GestureHudKind,
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val icon = when (kind) {
        GestureHudKind.VOLUME -> if (fraction <= 0f) R.drawable.ic_mute else R.drawable.ic_volume
        GestureHudKind.BRIGHTNESS -> R.drawable.ic_brightness
    }
    // Compact capsule track with the white fill rising bottom-up, and the glyph seated
    // inside at the bottom. The icon's tint flips to stay legible over whichever it sits on —
    // dark over the white fill (the usual case), light over the bare dark track near zero.
    Box(
        modifier = modifier
            .width(46.dp)
            .height(168.dp)
            .clip(RoundedCornerShape(50))
            .background(ScrimStrong),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction)
                .background(OnScrim),
        )
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (fraction > 0.12f) ScrimStrong else OnScrim,
            modifier = Modifier.padding(bottom = 12.dp).size(22.dp),
        )
    }
}

/**
 * Centered transient pill confirming the current [AspectMode] after a cycle.
 * Text-only, matching the HUD pill language; dismisses on its own timer.
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
/**
 * "No audio — this device can't play DTS": the only signal the user gets when a track has
 * no decoder, because ExoPlayer plays on silently rather than erroring. Same transient
 * pill family as the sleep/orientation HUDs; informational, no action.
 */
@Composable
fun UnsupportedCodecHud(
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
                    painter = painterResource(R.drawable.ic_mute),
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
    /** Shown only when the shot saved and there is something to share. */
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
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
                if (actionLabel != null && onAction != null) {
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkAccent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onAction)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}
