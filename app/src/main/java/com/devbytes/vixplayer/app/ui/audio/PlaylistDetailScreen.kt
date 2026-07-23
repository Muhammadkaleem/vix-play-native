package com.devbytes.vixplayer.app.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.data.repository.PlaylistRow
import com.devbytes.vixplayer.app.ui.library.components.LibraryEmptyState

/**
 * Playlist contents: play all / shuffle, drag to reorder, remove.
 *
 * Items whose source file is gone are shown greyed and labelled rather than dropped, per
 * the PRD, and are skipped when the playlist is played.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onBack: () -> Unit = {},
    onOpenPlayer: () -> Unit = {},
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val rows by viewModel.rows.collectAsState()
    val name by viewModel.name.collectAsState()

    LaunchedEffect(playlistId) { viewModel.load(playlistId) }

    val listState = rememberLazyListState()
    val reorder = rememberReorderState(
        listState = listState,
        onMove = { from, to -> viewModel.move(from, to) },
        onDrop = { viewModel.persistOrder() },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name.ifBlank { "Playlist" }) },
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
        if (rows.isEmpty()) {
            LibraryEmptyState(
                iconRes = R.drawable.ic_playlist,
                title = "Nothing here yet",
                body = "Add tracks from the library using the menu on any track.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { viewModel.play(0); onOpenPlayer() },
                    modifier = Modifier.weight(1f),
                ) { Text("Play all") }
                OutlinedButton(
                    onClick = { viewModel.play(0, shuffle = true); onOpenPlayer() },
                    modifier = Modifier.weight(1f),
                ) { Text("Shuffle") }
            }

            LazyColumn(state = listState) {
                itemsIndexed(rows, key = { _, row -> row.item.id }) { index, row ->
                    ItemRow(
                        row = row,
                        dragging = reorder.draggingIndex == index,
                        dragOffset = if (reorder.draggingIndex == index) reorder.dragOffset else 0f,
                        modifier = Modifier.reorderable(reorder, index),
                        onPlay = {
                            if (row.available) {
                                viewModel.play(index)
                                onOpenPlayer()
                            }
                        },
                        onRemove = { viewModel.remove(row.item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemRow(
    row: PlaylistRow,
    dragging: Boolean,
    dragOffset: Float,
    modifier: Modifier = Modifier,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = dragOffset }
            .background(
                if (dragging) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.background
                }
            )
            .clickable(enabled = row.available, onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(uri = row.albumArtUri.orEmpty(), size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                // Unavailable rows stay visible but read as inactive.
                .alpha(if (row.available) 1f else 0.45f),
        ) {
            Text(
                text = row.item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (row.available) row.item.artist else "Unavailable — file missing",
                style = MaterialTheme.typography.bodySmall,
                color = if (row.available) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = "Remove",
            )
        }
    }
}
