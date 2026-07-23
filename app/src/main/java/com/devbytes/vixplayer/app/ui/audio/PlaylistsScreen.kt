package com.devbytes.vixplayer.app.ui.audio

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.data.db.entity.Playlist
import com.devbytes.vixplayer.app.ui.library.components.LibraryEmptyState

/** Playlist list with create / rename / delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    onBack: () -> Unit = {},
    onOpenPlaylist: (Long) -> Unit = {},
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsState()
    val counts by viewModel.counts.collectAsState()
    var createDialog by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Playlist?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playlists") },
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
        floatingActionButton = {
            FloatingActionButton(onClick = { createDialog = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = "New playlist",
                )
            }
        },
    ) { padding ->
        if (playlists.isEmpty()) {
            LibraryEmptyState(
                iconRes = R.drawable.ic_playlist,
                title = "No playlists yet",
                body = "Create one, then add tracks from the library.",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistRowItem(
                        playlist = playlist,
                        count = counts[playlist.id] ?: 0,
                        onClick = { onOpenPlaylist(playlist.id) },
                        onRename = { renaming = playlist },
                        onDelete = { viewModel.delete(playlist) },
                    )
                }
            }
        }
    }

    if (createDialog) {
        NameDialog(
            title = "New playlist",
            initial = "",
            confirmLabel = "Create",
            onConfirm = { viewModel.create(it); createDialog = false },
            onDismiss = { createDialog = false },
        )
    }

    renaming?.let { playlist ->
        NameDialog(
            title = "Rename playlist",
            initial = playlist.name,
            confirmLabel = "Rename",
            onConfirm = { viewModel.rename(playlist, it); renaming = null },
            onDismiss = { renaming = null },
        )
    }
}

@Composable
private fun PlaylistRowItem(
    playlist: Playlist,
    count: Int,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_playlist),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (count == 1) "1 track" else "$count tracks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = "More",
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = { menu = false; onRename() },
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { menu = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Name") },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
