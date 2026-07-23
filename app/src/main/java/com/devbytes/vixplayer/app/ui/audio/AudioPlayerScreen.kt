package com.devbytes.vixplayer.app.ui.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.ui.player.components.PlayerSeekBar
import kotlinx.coroutines.delay

/**
 * Full-screen audio player over the shared app-scoped player.
 *
 * Background playback, notification and lock-screen controls need nothing here:
 * `PlaybackService` is already hosting a `MediaSession` over this same player instance.
 *
 * Shuffle/repeat/next/prev are the engine's own playlist semantics rather than a queue
 * kept alongside it, so the UI can't drift out of sync with what is actually playing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    mediaStoreId: Long = -1L,
    onBack: () -> Unit = {},
    onEqualizerClick: () -> Unit = {},
    viewModel: AudioPlayerViewModel = hiltViewModel(),
) {
    val player = viewModel.player
    val tracksByUri by viewModel.tracksByUri.collectAsState()

    // No-op when the library already queued; covers deep-link / notification entry.
    LaunchedEffect(mediaStoreId) { viewModel.ensureQueued(mediaStoreId) }

    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var shuffle by remember { mutableStateOf(player.shuffleModeEnabled) }
    var repeatMode by remember { mutableIntStateOf(player.repeatMode) }
    var currentUri by remember {
        mutableStateOf(player.currentMediaItem?.localConfiguration?.uri?.toString())
    }
    var positionMs by remember { mutableLongStateOf(player.currentPosition) }
    var durationMs by remember { mutableLongStateOf(player.duration.coerceAtLeast(0L)) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                currentUri = item?.localConfiguration?.uri?.toString()
                durationMs = player.duration.coerceAtLeast(0L)
            }

            override fun onShuffleModeEnabledChanged(enabled: Boolean) {
                shuffle = enabled
            }

            override fun onRepeatModeChanged(mode: Int) {
                repeatMode = mode
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Position drives the seek bar; duration only resolves once the item is prepared.
    LaunchedEffect(Unit) {
        while (true) {
            positionMs = player.currentPosition
            durationMs = player.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    // The queue holds URIs; MediaStore owns the metadata, so resolve the display fields
    // from whatever the player transitioned to.
    val track = currentUri?.let { tracksByUri[it] }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now playing") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AlbumArt(uri = track?.albumArtUri?.toString().orEmpty(), size = 260.dp)

            Spacer(Modifier.height(28.dp))

            Text(
                text = track?.title ?: "Not playing",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = listOfNotNull(
                    track?.artist,
                    track?.album?.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(24.dp))

            PlayerSeekBar(
                positionMs = positionMs,
                bufferedMs = player.bufferedPosition,
                durationMs = durationMs,
                onSeek = { player.seekTo(it) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TransportButton(
                    iconRes = R.drawable.ic_shuffle,
                    description = "Shuffle",
                    active = shuffle,
                    onClick = { player.shuffleModeEnabled = !player.shuffleModeEnabled },
                )
                TransportButton(
                    iconRes = R.drawable.ic_previous,
                    description = "Previous",
                    onClick = { player.seekToPreviousMediaItem() },
                )
                TransportButton(
                    iconRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                    description = if (isPlaying) "Pause" else "Play",
                    size = 64,
                    onClick = { if (player.isPlaying) player.pause() else player.play() },
                )
                TransportButton(
                    iconRes = R.drawable.ic_next,
                    description = "Next",
                    onClick = { player.seekToNextMediaItem() },
                )
                TransportButton(
                    iconRes = if (repeatMode == Player.REPEAT_MODE_ONE) {
                        R.drawable.ic_repeat_one
                    } else {
                        R.drawable.ic_repeat
                    },
                    description = "Repeat",
                    active = repeatMode != Player.REPEAT_MODE_OFF,
                    onClick = { player.repeatMode = nextRepeatMode(player.repeatMode) },
                )
            }
        }
    }
}

/** OFF → ALL → ONE → OFF, the conventional cycle. */
private fun nextRepeatMode(current: Int): Int = when (current) {
    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
    else -> Player.REPEAT_MODE_OFF
}

@Composable
private fun TransportButton(
    iconRes: Int,
    description: String,
    onClick: () -> Unit,
    active: Boolean = false,
    size: Int = 44,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(size.dp)) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.size((size * 0.6f).dp),
        )
    }
}
