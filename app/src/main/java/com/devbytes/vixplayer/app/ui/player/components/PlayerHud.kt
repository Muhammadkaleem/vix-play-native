package com.devbytes.vixplayer.app.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.media3.exoplayer.ExoPlayer
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.ui.common.LocalIsTv
import com.devbytes.vixplayer.app.ui.common.TvSafeHorizontal
import com.devbytes.vixplayer.app.ui.common.TvSafeVertical
import com.devbytes.vixplayer.app.ui.player.PlayerSkin
import com.devbytes.vixplayer.app.ui.theme.AmoledBackground
import com.devbytes.vixplayer.app.ui.theme.OnScrim
import kotlinx.coroutines.delay

@Composable
fun PlayerHud(
    player: ExoPlayer,
    visible: Boolean,
    title: String,
    skin: PlayerSkin,
    onBack: () -> Unit,
    aspectMode: AspectMode,
    orientationLabel: String,
    sleepTimerLabel: String,
    showAudioTrack: Boolean = false,
    showPip: Boolean = false,
    onCycleAspect: () -> Unit = {},
    onCycleOrientation: () -> Unit = {},
    onSpeedClick: () -> Unit = {},
    /** Current speed shown in the HUD when it isn't 1× (null hides it, showing the icon). */
    speedLabel: String? = null,
    onSubtitlesClick: () -> Unit = {},
    onAudioClick: () -> Unit = {},
    onEnterPip: () -> Unit = {},
    onSleepTimerClick: () -> Unit = {},
    onScreenshotClick: () -> Unit = {},
    onFileInfoClick: () -> Unit = {},
    onLockClick: () -> Unit = {},
    onMenuOpenChange: (Boolean) -> Unit = {},
    /** Opens the slide-out options panel (replaces the old overflow dropdown). */
    onMoreClick: () -> Unit = {},
    onScrubbingChange: (Boolean) -> Unit = {},
    /** Supplies scrub-preview frames; null disables the seek-bar thumbnail. */
    frameLoader: (suspend (Long) -> android.graphics.Bitmap?)? = null,
    /** Lock is a touch control (it blocks gestures); a TV remote has none to block. */
    showLock: Boolean = true,
    /** Attached to Play/Pause, so a remote lands there when the controls appear. */
    playFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    // TV overscan: controls keep clear of edges a TV may crop; the scrim still reaches them.
    val safeArea = if (LocalIsTv.current) {
        Modifier.padding(horizontal = TvSafeHorizontal, vertical = TvSafeVertical)
    } else {
        Modifier
    }
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var positionMs by remember { mutableLongStateOf(player.currentPosition) }
    var durationMs by remember { mutableLongStateOf(player.duration.coerceAtLeast(0L)) }
    var bufferedMs by remember { mutableLongStateOf(player.bufferedPosition) }

    LaunchedEffect(player) {
        while (true) {
            isPlaying = player.isPlaying
            positionMs = player.currentPosition
            durationMs = player.duration.coerceAtLeast(0L)
            bufferedMs = player.bufferedPosition
            delay(500)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to AmoledBackground.copy(alpha = 0.65f),
                            0.25f to AmoledBackground.copy(alpha = 0f),
                            0.72f to AmoledBackground.copy(alpha = 0f),
                            1f to AmoledBackground.copy(alpha = 0.85f),
                        )
                    ),
            )

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Clear the status bar + any display cutout (notch/punch-hole), so the
                    // top controls never sit under a cutout in immersive mode or landscape.
                    .statusBarsPadding()
                    .displayCutoutPadding()
                    .then(safeArea)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HudButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = "Back",
                        tint = OnScrim,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = OnScrim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                // Priority top-bar controls (most-used first): Audio track, Subtitles, Speed,
                // then Lock. Audio track sits first — the clip's audio switcher lives here.
                // Only shown when the media carries a real audio choice (>= 2 tracks).
                if (showAudioTrack) {
                    HudButton(onClick = onAudioClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_audio_track),
                            contentDescription = "Audio track",
                            tint = OnScrim,
                        )
                    }
                }
                HudButton(onClick = onSubtitlesClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_subtitles),
                        contentDescription = "Subtitles",
                        tint = OnScrim,
                    )
                }
                HudButton(onClick = onSpeedClick) {
                    if (speedLabel != null) {
                        // Non-1× — surface the value loudly in the accent colour.
                        Text(
                            text = speedLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_speed),
                            contentDescription = "Playback speed",
                            tint = OnScrim,
                        )
                    }
                }
                if (showLock) {
                    HudButton(onClick = onLockClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lock),
                            contentDescription = "Lock controls",
                            tint = OnScrim,
                        )
                    }
                }
                HudButton(onClick = onMoreClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = "More options",
                        tint = OnScrim,
                    )
                }
            }

            // Bottom controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    // Clear the nav bar + any display cutout so the seek bar / transport
                    // controls aren't clipped by a cutout (esp. in landscape).
                    .navigationBarsPadding()
                    .displayCutoutPadding()
                    .then(safeArea)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                PlayerSeekBar(
                    positionMs = positionMs,
                    bufferedMs = bufferedMs,
                    durationMs = durationMs,
                    onSeek = { player.seekTo(it) },
                    onScrubbingChange = onScrubbingChange,
                    frameLoader = frameLoader,
                    onStep = { delta ->
                        val max = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                        player.seekTo((player.currentPosition + delta).coerceIn(0L, max))
                        positionMs = player.currentPosition
                    },
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HudButton(onClick = { player.seekBack() }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_rewind),
                            contentDescription = "Rewind",
                            tint = OnScrim,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    HudButton(
                        onClick = { if (player.isPlaying) player.pause() else player.play() },
                        modifier = Modifier
                            .size(56.dp)
                            .then(playFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
                    ) {
                        Icon(
                            painter = painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = OnScrim,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    HudButton(onClick = { player.seekForward() }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_forward),
                            contentDescription = "Forward",
                            tint = OnScrim,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * An over-video control. Focus (TV remote, D-pad) shows as a white ring with a light fill:
 * Material's own focus layer is too faint against a dark frame to follow from across a room.
 */
@Composable
private fun HudButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    IconButton(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier.then(
            if (focused) Modifier
                .background(OnScrim.copy(alpha = 0.18f), CircleShape)
                .border(2.dp, OnScrim, CircleShape)
            else Modifier,
        ),
        content = content,
    )
}
