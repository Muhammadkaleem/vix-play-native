package com.devbytes.vixplayer.app.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.ui.library.components.LibraryEmptyState
import com.devbytes.vixplayer.app.ui.library.components.LibrarySkeleton

/**
 * Audio tab root — Tracks listing.
 *
 * The other four PRD groupings (Albums / Artists / Folders / Playlists) are additive:
 * they are different queries feeding this same row and player, so each lands as its own
 * pass rather than shipping four empty tabs now.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioLibraryScreen(
    onAudioPlayerClick: (Long) -> Unit = {},
    onPlaylistsClick: () -> Unit = {},
    onEqualizerClick: () -> Unit = {},
    viewModel: AudioLibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        when (val s = state) {
            is AudioLibraryUiState.Loading -> LibrarySkeleton(
                columns = 1,
                modifier = Modifier.padding(padding),
            )

            is AudioLibraryUiState.Empty -> LibraryEmptyState(
                iconRes = R.drawable.ic_nav_audio,
                title = "No music yet",
                body = "Audio files on this device will show up here.",
                modifier = Modifier.padding(padding),
            )

            is AudioLibraryUiState.Tracks -> LazyColumn(modifier = Modifier.padding(padding)) {
                items(s.tracks, key = { it.mediaStoreId }) { track ->
                    TrackRow(
                        title = track.title,
                        artist = track.artist,
                        durationMs = track.durationMs,
                        albumArtUri = track.albumArtUri.toString(),
                        onClick = {
                            viewModel.play(track)
                            onAudioPlayerClick(track.mediaStoreId)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    title: String,
    artist: String,
    durationMs: Long,
    albumArtUri: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(uri = albumArtUri, size = 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = formatTrackDuration(durationMs),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Album art from the MediaStore album-art URI. A tinted note icon sits behind it as the
 * fallback for albums with no art — the PRD's "missing art → fallback art" — so a failed
 * or empty load leaves the placeholder visible rather than a blank square.
 */
@Composable
fun AlbumArt(
    uri: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_nav_audio),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size / 2),
        )
        AsyncImage(
            model = ImageRequest.Builder(context).data(uri).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size),
        )
    }
}

internal fun formatTrackDuration(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "$m:${s.toString().padStart(2, '0')}"
}
