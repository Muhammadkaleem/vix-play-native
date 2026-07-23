package com.devbytes.vixplayer.app.ui.library

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.data.repository.VideoFile
import com.devbytes.vixplayer.app.ui.library.components.ContinueWatchingRail
import com.devbytes.vixplayer.app.ui.library.components.LibraryEmptyState
import com.devbytes.vixplayer.app.ui.library.components.LibrarySkeleton
import com.devbytes.vixplayer.app.ui.library.components.VideoCard

private val SortOrder.label: String
    get() = when (this) {
        SortOrder.RECENT -> "Recent"
        SortOrder.NAME -> "Name (A–Z)"
        SortOrder.DURATION -> "Longest"
        SortOrder.SIZE -> "Largest"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoLibraryScreen(
    onVideoClick: (Uri) -> Unit,
    onSearchClick: () -> Unit = {},
    onFolderBrowseClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    viewModel: VideoLibraryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var sortMenuOpen by remember { mutableStateOf(false) }

    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val selectionMode = selected.isNotEmpty()
    // Only shown below API 30, where removal is permanent and nothing else confirms it.
    var confirmDelete by remember { mutableStateOf(false) }

    BackHandler(enabled = selectionMode) { viewModel.clearSelection() }

    val deleteConsent = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onDeleteConsentResult(result.resultCode == android.app.Activity.RESULT_OK)
    }

    fun removeSelection() {
        viewModel.deleteSelection(uiState.allVideos) { sender ->
            deleteConsent.launch(
                androidx.activity.result.IntentSenderRequest.Builder(sender).build()
            )
        }
    }

    message?.let { text ->
        LaunchedEffect(text) {
            android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_VIDEO
    else
        Manifest.permission.READ_EXTERNAL_STORAGE

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onPermissionGranted() else viewModel.onPermissionDenied()
    }

    // Splash owns the cold-start permission request (see Routes.kt). The library only
    // reflects the current grant state; if the user later revoked access it shows the
    // passive empty-state, and re-grants via that CTA — it never auto-fires the dialog.
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED
        if (granted) viewModel.onPermissionGranted() else viewModel.onPermissionDenied()
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = { Text("${selected.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = "Clear selection",
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            shareVideos(context, viewModel.selectedVideos(uiState.allVideos))
                            viewModel.clearSelection()
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_share),
                                contentDescription = "Share",
                            )
                        }
                        IconButton(onClick = {
                            if (viewModel.systemConfirmsDelete) removeSelection()
                            else confirmDelete = true
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = if (viewModel.deleteIsRecoverable) {
                                    "Move to trash"
                                } else {
                                    "Delete"
                                },
                            )
                        }
                        IconButton(onClick = { viewModel.selectAll(uiState.allVideos) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = "Select all",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
                return@Scaffold
            }
            TopAppBar(
                title = { Text("VixPlay") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = onFolderBrowseClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_folder),
                            contentDescription = "Browse folders",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (uiState.hasPermission && uiState.allVideos.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { sortMenuOpen = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_sort),
                                    contentDescription = "Sort",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            DropdownMenu(
                                expanded = sortMenuOpen,
                                onDismissRequest = { sortMenuOpen = false },
                            ) {
                                SortOrder.values().forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.label) },
                                        onClick = {
                                            viewModel.setSortOrder(order)
                                            sortMenuOpen = false
                                        },
                                        trailingIcon = if (order == uiState.sortOrder) {
                                            {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_check),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        } else null,
                                    )
                                }
                            }
                        }
                        IconButton(onClick = viewModel::toggleViewMode) {
                            Icon(
                                painter = painterResource(
                                    if (uiState.viewMode == ViewMode.GRID) R.drawable.ic_list else R.drawable.ic_grid,
                                ),
                                contentDescription = "Toggle view",
                                tint = MaterialTheme.colorScheme.onSurface,
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
        when {
            !uiState.hasPermission -> LibraryEmptyState(
                iconRes = R.drawable.ic_lock,
                title = "Storage access needed",
                body = "VixPlay needs permission to find videos on your device. Nothing ever leaves your phone.",
                primaryLabel = "Grant access",
                onPrimary = { permissionLauncher.launch(permission) },
                modifier = Modifier.padding(padding),
            )
            uiState.isLoading -> LibrarySkeleton(
                columns = if (uiState.viewMode == ViewMode.GRID) 2 else 1,
                modifier = Modifier.padding(padding),
            )
            uiState.error != null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { Text("Error: ${uiState.error}") }
            uiState.allVideos.isEmpty() -> LibraryEmptyState(
                iconRes = R.drawable.ic_nav_video,
                title = "No videos yet",
                body = "We couldn't find any videos in your media library. Try browsing a specific folder.",
                primaryLabel = "Browse folders",
                onPrimary = onFolderBrowseClick,
                modifier = Modifier.padding(padding),
            )
            else -> LibraryGrid(
                uiState = uiState,
                onVideoClick = onVideoClick,
                selected = selected,
                selectionMode = selectionMode,
                onToggleSelect = { viewModel.toggleSelection(it) },
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (confirmDelete) {
        ConfirmDeleteDialog(
            count = selected.size,
            onConfirm = { confirmDelete = false; removeSelection() },
            onDismiss = { confirmDelete = false },
        )
    }
}

// Only reachable below API 30: no trash there, so this dialog is the sole thing between
// a tap and permanent deletion.
@Composable
private fun ConfirmDeleteDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete $count videos?") },
        text = { Text("This permanently removes them from this device. It can't be undone.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Hands the selection to the system chooser, which is itself the confirmation step. */
private fun shareVideos(context: android.content.Context, videos: List<VideoFile>) {
    if (videos.isEmpty()) return
    val uris = ArrayList(videos.map { it.uri })
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, uris.first())
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "video/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Share"))
}

@Composable
private fun LibraryGrid(
    uiState: VideoLibraryUiState,
    onVideoClick: (Uri) -> Unit,
    selected: Set<Long>,
    selectionMode: Boolean,
    onToggleSelect: (VideoFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resumeMap = uiState.continueWatching.associate { it.video.mediaStoreId to it.progressFraction }
    val columns = if (uiState.viewMode == ViewMode.GRID) 2 else 1

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        // Hidden while selecting: the rail's cards aren't selection-aware, so leaving it
        // up would mean a tap there opens the player while a tap below toggles a checkbox.
        if (uiState.continueWatching.isNotEmpty() && !selectionMode) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ContinueWatchingRail(
                    items = uiState.continueWatching,
                    onVideoClick = { onVideoClick(it.uri) },
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
        items(uiState.allVideos, key = { it.mediaStoreId }) { video ->
            VideoCard(
                video = video,
                resumeFraction = resumeMap[video.mediaStoreId] ?: 0f,
                isSelected = video.mediaStoreId in selected,
                // In selection mode a tap toggles instead of opening the player.
                onClick = {
                    if (selectionMode) onToggleSelect(video) else onVideoClick(video.uri)
                },
                onLongClick = { onToggleSelect(video) },
            )
        }
    }
}
