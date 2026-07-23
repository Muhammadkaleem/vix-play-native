package com.devbytes.vixplayer.app.ui.library

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.data.repository.FolderEntry
import com.devbytes.vixplayer.app.data.repository.VideoFile
import com.devbytes.vixplayer.app.ui.library.components.FolderListSkeleton
import com.devbytes.vixplayer.app.ui.library.components.LibraryEmptyState
import com.devbytes.vixplayer.app.ui.library.components.LibrarySkeleton
import com.devbytes.vixplayer.app.ui.library.components.VideoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserScreen(
    onVideoClick: (Uri) -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: FolderBrowserViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isInFolder = state is FolderBrowserUiState.VideoList

    // Which shape the pending Loading state will resolve to, so the skeleton
    // matches the incoming content (folder rows vs. video grid) with no layout
    // shift. Defaults to folders (cold start + navigate-up); flipped on descend.
    var pendingGrid by remember { mutableStateOf(false) }

    val goUp: () -> Unit = { pendingGrid = false; viewModel.navigateUp() }

    val context = LocalContext.current
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val selectionMode = selected.isNotEmpty()
    var confirmDelete by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var selectionMenu by remember { mutableStateOf(false) }
    // Non-null while choosing a destination; the flag says move vs copy.
    var choosingDestination by remember { mutableStateOf<Boolean?>(null) }
    val destinations by viewModel.destinations.collectAsStateWithLifecycle()
    val transfer by viewModel.transfer.collectAsStateWithLifecycle()

    val visibleVideos = (state as? FolderBrowserUiState.VideoList)?.videos.orEmpty()

    // Back unwinds selection first, then leaves the folder.
    BackHandler(enabled = selectionMode) { viewModel.clearSelection() }
    BackHandler(enabled = !selectionMode && isInFolder, onBack = goUp)

    val deleteConsent = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onDeleteConsentResult(result.resultCode == android.app.Activity.RESULT_OK)
    }
    val renameConsent = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onRenameConsentResult(result.resultCode == android.app.Activity.RESULT_OK)
    }

    fun removeSelection() {
        viewModel.deleteSelection { sender ->
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

    val title = if (state is FolderBrowserUiState.VideoList) {
        (state as FolderBrowserUiState.VideoList).folderName
    } else {
        "Browse Folders"
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
                            shareFolderVideos(context, viewModel.selectedVideos(visibleVideos))
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
                        IconButton(onClick = { viewModel.selectAll(visibleVideos) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = "Select all",
                            )
                        }
                        // Move/copy/rename live behind the overflow with text labels:
                        // the design set has no copy or move icon, and six icons in one
                        // bar is past the point of being readable anyway.
                        Box {
                            IconButton(onClick = { selectionMenu = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_more_vert),
                                    contentDescription = "More actions",
                                )
                            }
                            DropdownMenu(
                                expanded = selectionMenu,
                                onDismissRequest = { selectionMenu = false },
                            ) {
                                // Rename is single-item only — no sensible bulk meaning.
                                if (selected.size == 1) {
                                    DropdownMenuItem(
                                        text = { Text("Rename") },
                                        onClick = { selectionMenu = false; renaming = true },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Copy to folder") },
                                    onClick = {
                                        selectionMenu = false
                                        viewModel.loadDestinations()
                                        choosingDestination = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Move to folder") },
                                    onClick = {
                                        selectionMenu = false
                                        viewModel.loadDestinations()
                                        choosingDestination = true
                                    },
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
                return@Scaffold
            }
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = if (isInFolder) goUp else onBack) {
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
        when (val s = state) {
            is FolderBrowserUiState.Loading ->
                if (pendingGrid) {
                    LibrarySkeleton(columns = 2, modifier = Modifier.padding(padding))
                } else {
                    FolderListSkeleton(modifier = Modifier.padding(padding))
                }

            is FolderBrowserUiState.FolderList -> FolderList(
                folders = s.folders,
                onFolderClick = { folder ->
                    pendingGrid = true
                    viewModel.enterFolder(folder.bucketId, folder.name)
                },
                modifier = Modifier.padding(padding),
            )

            is FolderBrowserUiState.VideoList -> VideoGrid(
                videos = s.videos,
                onVideoClick = onVideoClick,
                selected = selected,
                selectionMode = selectionMode,
                onToggleSelect = { viewModel.toggleSelection(it) },
                modifier = Modifier.padding(padding),
            )

            is FolderBrowserUiState.Error -> LibraryEmptyState(
                iconRes = R.drawable.ic_info,
                title = "Something went wrong",
                body = s.message,
                primaryLabel = "Retry",
                onPrimary = goUp,
                modifier = Modifier.padding(padding),
            )
        }
    }

    choosingDestination?.let { isMove ->
        AlertDialog(
            onDismissRequest = { choosingDestination = null },
            title = { Text(if (isMove) "Move to…" else "Copy to…") },
            text = {
                if (destinations.isEmpty()) {
                    Text(
                        text = "No other folders contain videos yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        destinations.forEach { folder ->
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        choosingDestination = null
                                        viewModel.transferSelection(folder, isMove)
                                    }
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { choosingDestination = null }) { Text("Cancel") }
            },
        )
    }

    // Modal while transferring: letting the user mutate the same folder under a running
    // copy invites inconsistency, and they explicitly asked for this operation.
    transfer?.let { progress ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("${progress.currentIndex} of ${progress.total}") },
            text = {
                Column {
                    Text(
                        text = progress.currentName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress.fileFraction },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.cancelTransfer() }) { Text("Cancel") }
            },
        )
    }

    // Only reachable below API 30, where there is no trash and nothing else confirms.
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${selected.size} videos?") },
            text = { Text("This permanently removes them from this device. It can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; removeSelection() }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }

    if (renaming) {
        val target = viewModel.selectedVideos(visibleVideos).singleOrNull()
        // Seeded without the extension, which is reapplied on save, so the user edits the
        // name rather than having to retype ".mp4" correctly.
        var newName by remember(target) {
            mutableStateOf(target?.name?.substringBeforeLast('.').orEmpty())
        }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("Name") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        renaming = false
                        viewModel.renameSelected(newName) { sender ->
                            renameConsent.launch(
                                androidx.activity.result.IntentSenderRequest
                                    .Builder(sender).build()
                            )
                        }
                    },
                    enabled = newName.isNotBlank(),
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { renaming = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun FolderList(
    folders: List<FolderEntry>,
    onFolderClick: (FolderEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (folders.isEmpty()) {
        LibraryEmptyState(
            iconRes = R.drawable.ic_nav_video,
            title = "No folders yet",
            body = "We couldn't find any folders containing videos on this device.",
            modifier = modifier,
        )
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(folders, key = { it.bucketId }) { folder ->
            FolderRow(folder = folder, onClick = { onFolderClick(folder) })
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun FolderRow(
    folder: FolderEntry,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(folder.sampleUri)
                        .decoderFactory(VideoFrameDecoder.Factory())
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(width = 96.dp, height = 54.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${folder.videoCount} video${if (folder.videoCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Hands the selection to the system chooser, which is itself the confirmation step. */
private fun shareFolderVideos(context: android.content.Context, videos: List<VideoFile>) {
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
private fun VideoGrid(
    videos: List<VideoFile>,
    onVideoClick: (Uri) -> Unit,
    selected: Set<Long>,
    selectionMode: Boolean,
    onToggleSelect: (VideoFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (videos.isEmpty()) {
        LibraryEmptyState(
            iconRes = R.drawable.ic_nav_video,
            title = "No videos in this folder",
            body = "This folder doesn't contain any playable videos.",
            modifier = modifier,
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        items(videos, key = { it.mediaStoreId }) { video ->
            VideoCard(
                video = video,
                isSelected = video.mediaStoreId in selected,
                onClick = {
                    if (selectionMode) onToggleSelect(video) else onVideoClick(video.uri)
                },
                onLongClick = { onToggleSelect(video) },
            )
        }
    }
}
