package com.devbytes.vixplayer.app.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.media3.exoplayer.ExoPlayer
import com.devbytes.vixplayer.app.R
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
    onSubtitlesClick: () -> Unit = {},
    onAudioClick: () -> Unit = {},
    onEnterPip: () -> Unit = {},
    onSleepTimerClick: () -> Unit = {},
    onScreenshotClick: () -> Unit = {},
    onFileInfoClick: () -> Unit = {},
    onLockClick: () -> Unit = {},
    onMenuOpenChange: (Boolean) -> Unit = {},
    onScrubbingChange: (Boolean) -> Unit = {},
    /** Supplies scrub-preview frames; null disables the seek-bar thumbnail. */
    frameLoader: (suspend (Long) -> android.graphics.Bitmap?)? = null,
    modifier: Modifier = Modifier,
) {
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var menuExpanded by remember { mutableStateOf(false) }
    // Report menu-open upward so the parent can freeze the controls auto-hide timer.
    LaunchedEffect(menuExpanded) { onMenuOpenChange(menuExpanded) }
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
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
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
                // Priority controls, promoted from the overflow to direct top-bar
                // buttons (most-used first): Subtitles, Audio track, Speed, then Lock.
                IconButton(onClick = onSubtitlesClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_subtitles),
                        contentDescription = "Subtitles",
                        tint = OnScrim,
                    )
                }
                // Only shown when the media carries a real audio choice (>= 2 tracks).
                if (showAudioTrack) {
                    IconButton(onClick = onAudioClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_audio_track),
                            contentDescription = "Audio track",
                            tint = OnScrim,
                        )
                    }
                }
                IconButton(onClick = onSpeedClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_speed),
                        contentDescription = "Playback speed",
                        tint = OnScrim,
                    )
                }
                IconButton(onClick = onLockClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock),
                        contentDescription = "Lock controls",
                        tint = OnScrim,
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more_vert),
                            contentDescription = "More",
                            tint = OnScrim,
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        // Only surfaces on devices that support Picture-in-Picture.
                        if (showPip) {
                            DropdownMenuItem(
                                text = { Text("Picture-in-picture") },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_pip),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onEnterPip()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Screenshot") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_screenshot),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onScreenshotClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("File info") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_info),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onFileInfoClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(sleepTimerLabel) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_timer),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onSleepTimerClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Aspect: ${aspectMode.label}") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_aspect),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onCycleAspect()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Orientation: $orientationLabel") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_rotate),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onCycleOrientation()
                            },
                        )
                    }
                }
            }

            // Bottom controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                PlayerSeekBar(
                    positionMs = positionMs,
                    bufferedMs = bufferedMs,
                    durationMs = durationMs,
                    onSeek = { player.seekTo(it) },
                    onScrubbingChange = onScrubbingChange,
                    frameLoader = frameLoader,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { player.seekBack() }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_rewind),
                            contentDescription = "Rewind",
                            tint = OnScrim,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    IconButton(
                        onClick = { if (player.isPlaying) player.pause() else player.play() },
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            painter = painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = OnScrim,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    IconButton(onClick = { player.seekForward() }, modifier = Modifier.size(48.dp)) {
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
