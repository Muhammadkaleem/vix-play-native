package com.devbytes.vixplayer.app.ui.library

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
                        // Rename is single-item only — there is no sensible bulk meaning.
                        if (selected.size == 1) {
                            IconButton(onClick = { renaming = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_edit),
                                    contentDescription = "Rename",
                                )
                            }
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
