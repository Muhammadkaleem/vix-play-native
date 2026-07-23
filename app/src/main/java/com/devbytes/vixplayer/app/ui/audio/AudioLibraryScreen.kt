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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Audio tab root — Tracks / Albums / Artists / Folders / Playlists.
 *
 * The three groupings are derived in memory from one MediaStore query, so they can't
 * disagree with each other. Drilling into a group happens **in place** (same pattern as
 * `FolderBrowserScreen`) rather than through new routes.
 *
 * Playlists joined the tab set once it had a real data model behind it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioLibraryScreen(
    onAudioPlayerClick: (Long) -> Unit = {},
    onPlaylistsClick: () -> Unit = {},
    onEqualizerClick: () -> Unit = {},
    onOpenPlaylist: (Long) -> Unit = {},
    viewModel: AudioLibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val tab by viewModel.tab.collectAsState()
    val openGroup by viewModel.openGroup.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    // Track awaiting a playlist choice; non-null shows the picker.
    var pendingTrack by remember { mutableStateOf<AudioTrack?>(null) }

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
                            onAddToPlaylist = { pendingTrack = it },
                        )
                        // Tracks tab: the whole library is the context.
                        tab == AudioTab.TRACKS -> TrackList(
                            tracks = s.tracks,
                            onPlay = { track ->
                                viewModel.play(track, s.tracks)
                                onAudioPlayerClick(track.mediaStoreId)
                            },
                            onAddToPlaylist = { pendingTrack = it },
                        )

                        tab == AudioTab.PLAYLISTS -> {
                            if (playlists.isEmpty()) {
                                LibraryEmptyState(
                                    iconRes = R.drawable.ic_playlist,
                                    title = "No playlists yet",
                                    body = "Use the menu on any track to start one.",
                                )
                            } else {
                                LazyColumn {
                                    items(playlists, key = { it.id }) { playlist ->
                                        GroupRow(
                                            group = AudioGroup(
                                                name = playlist.name,
                                                trackCount = 0,
                                                artUri = "",
                                                tracks = emptyList(),
                                            ),
                                            showCount = false,
                                            onClick = { onOpenPlaylist(playlist.id) },
                                        )
                                    }
                                }
                            }
                        }

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

    // Playlist picker for the pending track, with an inline "new playlist" path so the
    // first playlist can be created without leaving the library.
    pendingTrack?.let { track ->
        var newName by remember { mutableStateOf("") }
        var creating by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { pendingTrack = null },
            title = { Text("Add to playlist") },
            text = {
                Column {
                    if (creating) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            singleLine = true,
                            label = { Text("Playlist name") },
                        )
                    } else {
                        if (playlists.isEmpty()) {
                            Text(
                                text = "No playlists yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        playlists.forEach { playlist ->
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addToPlaylist(playlist.id, track)
                                        pendingTrack = null
                                    }
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (creating) {
                    TextButton(
                        onClick = {
                            viewModel.createPlaylistWith(newName, track)
                            pendingTrack = null
                        },
                        enabled = newName.isNotBlank(),
                    ) { Text("Create") }
                } else {
                    TextButton(onClick = { creating = true }) { Text("New playlist") }
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingTrack = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun TrackList(
    tracks: List<AudioTrack>,
    onPlay: (AudioTrack) -> Unit,
    onAddToPlaylist: (AudioTrack) -> Unit,
) {
    LazyColumn {
        items(tracks, key = { it.mediaStoreId }) { track ->
            TrackRow(
                title = track.title,
                artist = track.artist,
                durationMs = track.durationMs,
                albumArtUri = track.albumArtUri.toString(),
                onClick = { onPlay(track) },
                onAddToPlaylist = { onAddToPlaylist(track) },
            )
        }
    }
}

@Composable
private fun GroupRow(
    group: AudioGroup,
    onClick: () -> Unit,
    showCount: Boolean = true,
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
            if (showCount) {
                Text(
                    text = if (group.trackCount == 1) "1 track" else "${group.trackCount} tracks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
    onAddToPlaylist: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
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
        // Overflow rather than long-press: the PRD reserves long-press for multi-select.
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Add to playlist") },
                    onClick = { menu = false; onAddToPlaylist() },
                )
            }
        }
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
