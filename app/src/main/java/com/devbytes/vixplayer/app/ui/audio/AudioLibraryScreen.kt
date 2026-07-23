package com.devbytes.vixplayer.app.ui.audio

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import com.devbytes.vixplayer.app.data.repository.AudioTrack
import com.devbytes.vixplayer.app.ui.library.components.LibraryEmptyState
import com.devbytes.vixplayer.app.ui.library.components.LibrarySkeleton

/**
 * Audio tab root — Tracks / Albums / Artists / Folders.
 *
 * All four groupings are derived in memory from one MediaStore query, so they can't
 * disagree with each other. Drilling into a group happens **in place** (same pattern as
 * `FolderBrowserScreen`) rather than through new routes.
 *
 * Playlists is not a tab here: it is P1 with its own route and no data model yet, so a
 * fifth tab would be selectable and permanently empty.
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
    val tab by viewModel.tab.collectAsState()
    val openGroup by viewModel.openGroup.collectAsState()

    // System back leaves the drill before it leaves the screen.
    BackHandler(enabled = openGroup != null) { viewModel.closeGroup() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(openGroup?.name ?: "Audio") },
                navigationIcon = {
                    if (openGroup != null) {
                        IconButton(onClick = { viewModel.closeGroup() }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_back),
                                contentDescription = "Back",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Tabs stay hidden while drilled in — the title and back arrow own that level.
            if (openGroup == null) {
                ScrollableTabRow(
                    selectedTabIndex = tab.ordinal,
                    containerColor = MaterialTheme.colorScheme.background,
                    edgePadding = 12.dp,
                ) {
                    AudioTab.entries.forEach { entry ->
                        Tab(
                            selected = entry == tab,
                            onClick = { viewModel.selectTab(entry) },
                            text = { Text(entry.label) },
                        )
                    }
                }
            }

            when (val s = state) {
                is AudioLibraryUiState.Loading -> LibrarySkeleton(columns = 1)

                is AudioLibraryUiState.Empty -> LibraryEmptyState(
                    iconRes = R.drawable.ic_nav_audio,
                    title = "No music yet",
                    body = "Audio files on this device will show up here.",
                )

                is AudioLibraryUiState.Loaded -> {
                    val drilled = openGroup
                    when {
                        // Inside a group: its tracks, queued as their own context.
                        drilled != null -> TrackList(
                            tracks = drilled.tracks,
                            onPlay = { track ->
                                viewModel.play(track, drilled.tracks)
                                onAudioPlayerClick(track.mediaStoreId)
                            },
                        )
                        // Tracks tab: the whole library is the context.
                        tab == AudioTab.TRACKS -> TrackList(
                            tracks = s.tracks,
                            onPlay = { track ->
                                viewModel.play(track, s.tracks)
                                onAudioPlayerClick(track.mediaStoreId)
                            },
                        )

                        else -> {
                            val groups = viewModel.groupsFor(tab, s.tracks)
                            LazyColumn {
                                items(groups, key = { it.name }) { group ->
                                    GroupRow(
                                        group = group,
                                        onClick = { viewModel.openGroup(group) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackList(
    tracks: List<AudioTrack>,
    onPlay: (AudioTrack) -> Unit,
) {
    LazyColumn {
        items(tracks, key = { it.mediaStoreId }) { track ->
            TrackRow(
                title = track.title,
                artist = track.artist,
                durationMs = track.durationMs,
                albumArtUri = track.albumArtUri.toString(),
                onClick = { onPlay(track) },
            )
        }
    }
}

@Composable
private fun GroupRow(
    group: AudioGroup,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(uri = group.artUri, size = 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (group.trackCount == 1) "1 track" else "${group.trackCount} tracks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
