package com.devbytes.vixplayer.app.ui.player.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.res.Configuration
import com.devbytes.vixplayer.app.R

/** Live values shown on the option tiles (sub-labels, toggle/visibility state). */
data class PlayerOptionsValues(
    val speedLabel: String,
    val audioLabel: String?,   // null hides the Audio tile (mono source)
    val subtitlesLabel: String,
    val aspectLabel: String,
    val repeatLabel: String,
    val sleepLabel: String,
    val orientationLabel: String,
    val backgroundOn: Boolean,
    val isFavorite: Boolean,
    val canFavorite: Boolean,
    val hasNext: Boolean,
    val supportsPip: Boolean,
    /** The marked intro ("0:42–1:52" / "Not set"); null hides the tile — not a show's episode. */
    val introLabel: String? = null,
)

// Reference palette: bright white content over a frosted, translucent dark panel.
private val HeaderColor = Color(0xE6FFFFFF)   // 90% white — section titles
private val IconColor = Color(0xF2FFFFFF)     // 95% white — tile icons/labels
private val LabelColor = Color(0xF2FFFFFF)
private val ValueColor = Color(0x8CFFFFFF)    // 55% white — value sub-label
private val HandleColor = Color(0x59FFFFFF)

/**
 * Slide-out player options panel, matched to the reference design: a right-anchored
 * frosted-glass sheet (a blurred frame of the current video under a translucent dark tint),
 * grouped icon tiles five-per-row. Tiles that pick a value open the existing bottom sheets;
 * cycle/toggle tiles act in place; the rest trigger + close. Only real actions are shown.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerOptionsPanel(
    visible: Boolean,
    values: PlayerOptionsValues,
    onDismiss: () -> Unit,
    onSpeed: () -> Unit,
    onAudio: () -> Unit,
    onSubtitles: () -> Unit,
    onAspect: () -> Unit,
    onRepeat: () -> Unit,
    onSleepTimer: () -> Unit,
    onLock: () -> Unit,
    onBackgroundPlay: () -> Unit,
    onOrientation: () -> Unit,
    onScreenshot: () -> Unit,
    onVideoInfo: () -> Unit,
    onEqualizer: () -> Unit,
    onPip: () -> Unit,
    onFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onShare: () -> Unit,
    onIntro: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    // The panel composable persists across open/close, so reset the scroll to the top (handle)
    // each time it opens rather than reopening wherever it was last left.
    val scrollState = rememberScrollState()
    LaunchedEffect(visible) { if (visible) scrollState.scrollTo(0) }

    Box(modifier = modifier.fillMaxSize()) {
        // Light tap-catcher — the reference keeps the video at full brightness, so barely dim.
        AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.12f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }

        BoxWithConstraints(Modifier.align(Alignment.CenterEnd)) {
            // Narrow right sheet in landscape (as in the reference), wider in portrait.
            val panelWidth = if (isPortrait) (maxWidth * 0.88f).coerceAtMost(460.dp)
            else (maxWidth * 0.42f).coerceIn(300.dp, 470.dp)
            val hPad = 12.dp
            val gap = 4.dp
            val tileWidth = (panelWidth - hPad * 2 - gap * 4) / 5

            AnimatedVisibility(
                visible = visible,
                // Wipe: reveal/hide from the right edge (width grows/shrinks, content clipped)
                // rather than sliding the whole sheet in as a block.
                enter = expandHorizontally(tween(280), expandFrom = Alignment.End) + fadeIn(tween(220)),
                exit = shrinkHorizontally(tween(220), shrinkTowards = Alignment.End) + fadeOut(tween(180)),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(panelWidth),
                ) {
                    // Soft cinematic shadow (no blur, no box): a horizontal gradient that fades
                    // from transparent at the left edge (blending into the video) to a semi-dark
                    // backing toward the right screen edge — no hard border or rounded container.
                    Box(
                        Modifier.matchParentSize().background(
                            Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color(0x0008090C), // left edge — fully transparent
                                    0.40f to Color(0x2E08090C), // rgba(8,9,12,0.18)
                                    0.70f to Color(0x6608090C), // rgba(8,9,12,0.40)
                                    1.0f to Color(0xA608090C), // right edge — rgba(8,9,12,0.65)
                                ),
                            ),
                        ),
                    )

                    Column(
                        Modifier
                            .fillMaxHeight()
                            .safeDrawingPadding()
                            .verticalScroll(scrollState)
                            .padding(horizontal = hPad, vertical = 8.dp),
                    ) {
                        Box(
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 6.dp)
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(HandleColor),
                        )

                        Section("Playback", gap) {
                            Tile(R.drawable.ic_speed, "Speed", values.speedLabel, tileWidth) { close(onDismiss, onSpeed) }
                            if (values.audioLabel != null) {
                                Tile(R.drawable.ic_audio_track, "Audio", values.audioLabel, tileWidth) { close(onDismiss, onAudio) }
                            }
                            Tile(R.drawable.ic_subtitles, "Subtitles", values.subtitlesLabel, tileWidth) { close(onDismiss, onSubtitles) }
                            Tile(R.drawable.ic_aspect, "Aspect Ratio", values.aspectLabel, tileWidth, onClick = onAspect)
                        }

                        Section("Playback Options", gap) {
                            Tile(R.drawable.ic_repeat, "Repeat Mode", values.repeatLabel, tileWidth, onClick = onRepeat)
                            Tile(R.drawable.ic_timer, "Sleep Timer", values.sleepLabel, tileWidth) { close(onDismiss, onSleepTimer) }
                            if (values.introLabel != null) {
                                Tile(R.drawable.ic_skip_next, "Skip Intro", values.introLabel, tileWidth) { close(onDismiss, onIntro) }
                            }
                            Tile(R.drawable.ic_lock, "Lock Screen", tileWidth = tileWidth) { close(onDismiss, onLock) }
                            Tile(R.drawable.ic_background, "Background Play", if (values.backgroundOn) "On" else "Off", tileWidth, onClick = onBackgroundPlay)
                            Tile(R.drawable.ic_rotate, "Orientation", values.orientationLabel, tileWidth, onClick = onOrientation)
                        }

                        Section("Tools", gap) {
                            Tile(R.drawable.ic_screenshot, "Screenshot", tileWidth = tileWidth) { close(onDismiss, onScreenshot) }
                            Tile(R.drawable.ic_info, "Video Info", tileWidth = tileWidth) { close(onDismiss, onVideoInfo) }
                            Tile(R.drawable.ic_equalizer, "Equalizer", tileWidth = tileWidth, iconTint = accent) { close(onDismiss, onEqualizer) }
                            if (values.supportsPip) {
                                // Spaces, not hyphens: the line breaker split "Picture-in-Pictu/re" mid-word in a narrow tile.
                                Tile(R.drawable.ic_pip, "Picture in Picture", tileWidth = tileWidth) { close(onDismiss, onPip) }
                            }
                        }

                        Section("Playlist & Share", gap) {
                            if (values.canFavorite) {
                                Tile(R.drawable.ic_favorite, "Add to Favorites", tileWidth = tileWidth, onClick = onFavorite)
                            }
                            Tile(R.drawable.ic_skip_next, "Play Next", tileWidth = tileWidth, enabled = values.hasNext) { close(onDismiss, onPlayNext) }
                            Tile(R.drawable.ic_share, "Share", tileWidth = tileWidth) { close(onDismiss, onShare) }
                        }

                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

private fun close(onDismiss: () -> Unit, action: () -> Unit) {
    onDismiss()
    action()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Section(title: String, gap: Dp, content: @Composable FlowRowScope.() -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        color = HeaderColor,
        modifier = Modifier.padding(start = 2.dp, top = 12.dp, bottom = 6.dp),
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        maxItemsInEachRow = 5,
        content = content,
    )
}

@Composable
private fun Tile(
    @DrawableRes iconRes: Int,
    label: String,
    value: String? = null,
    tileWidth: Dp,
    enabled: Boolean = true,
    iconTint: Color = IconColor,
    onClick: () -> Unit,
) {
    val a = if (enabled) 1f else 0.35f
    Column(
        modifier = Modifier
            .width(tileWidth)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = iconTint.copy(alpha = iconTint.alpha * a),
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            color = LabelColor.copy(alpha = LabelColor.alpha * a),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (value != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = ValueColor.copy(alpha = ValueColor.alpha * a),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
