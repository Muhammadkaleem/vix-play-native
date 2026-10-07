package com.devbytes.vixplayer.app.ui.library

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.ads.AdBanner
import com.devbytes.vixplayer.app.ads.AdNativeCard
import com.devbytes.vixplayer.app.ads.AdUnits
import com.devbytes.vixplayer.app.ads.AdsViewModel
import com.devbytes.vixplayer.app.data.repository.FolderEntry
import com.devbytes.vixplayer.app.data.repository.VideoFile
import com.devbytes.vixplayer.app.ui.library.components.FolderListSkeleton
import com.devbytes.vixplayer.app.ui.library.components.LibraryEmptyState
import com.devbytes.vixplayer.app.ui.library.components.LibrarySkeleton
import com.devbytes.vixplayer.app.ui.library.components.VideoCard
import com.devbytes.vixplayer.app.ui.library.components.VideoListRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserScreen(
    onVideoClick: (Uri) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onTrashClick: () -> Unit = {},
    onOpenBriefing: () -> Unit = {},
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
    // Non-null (isMove) while the "new folder" name dialog is open, carried over from the picker.
    var newFolderMove by remember { mutableStateOf<Boolean?>(null) }
    var newFolderName by remember { mutableStateOf("") }
    val destinations by viewModel.destinations.collectAsStateWithLifecycle()
    val transfer by viewModel.transfer.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val folderSort by viewModel.folderSort.collectAsStateWithLifecycle()
    val resume by viewModel.resume.collectAsStateWithLifecycle()
    var sortMenu by remember { mutableStateOf(false) }

    val adsViewModel: AdsViewModel = hiltViewModel()
    val canRequestAds by adsViewModel.canRequestAds.collectAsStateWithLifecycle()
    // Selection mode hides ads (a contextual action bar is up); otherwise gate on consent.
    val showAds = adsViewModel.adsEnabled && canRequestAds && !selectionMode

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

    // Android 14 partial media access: the user granted only selected videos, so the library
    // is honestly a subset. Re-requesting on 34+ re-opens the "Select more / Allow all" system
    // sheet; on the result we re-check and re-scan so newly-allowed videos appear.
    var partialAccess by remember { mutableStateOf(isPartialMediaAccess(context)) }
    val mediaAccessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        partialAccess = isPartialMediaAccess(context)
        viewModel.refreshFolders()
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
        // Folder list is now the Video tab home, so it carries the app name.
        "VixPlay"
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
                title = { if (isInFolder) Text(title) else VideoHeaderTitle() },
                navigationIcon = {
                    // Up arrow only inside a folder; the folder list is the tab home,
                    // so it has no back affordance.
                    if (isInFolder) {
                        IconButton(onClick = goUp) {
                            Icon(
                                painter = painterResource(R.drawable.ic_back),
                                contentDescription = "Up",
                            )
                        }
                    }
                },
                actions = {
                    if (isInFolder) {
                        // In a folder: toggle grid/list of its videos.
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                painter = painterResource(
                                    if (viewMode == ViewMode.GRID) R.drawable.ic_list
                                    else R.drawable.ic_grid
                                ),
                                contentDescription = "Toggle view",
                            )
                        }
                    } else {
                        // Folder-list home: search, history, and folder ordering.
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = "Search",
                            )
                        }
                        IconButton(onClick = onHistoryClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_history),
                                contentDescription = "History",
                            )
                        }
                        // Trash only exists on API 30+, so the entry is hidden below that
                        // (there would be nothing to show).
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            IconButton(onClick = onTrashClick) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete),
                                    contentDescription = "Trash",
                                )
                            }
                        }
                        Box {
                            IconButton(onClick = { sortMenu = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_sort),
                                    contentDescription = "Sort folders",
                                )
                            }
                            DropdownMenu(
                                expanded = sortMenu,
                                onDismissRequest = { sortMenu = false },
                            ) {
                                FolderSort.entries.forEach { sort ->
                                    DropdownMenuItem(
                                        text = { Text(sort.label) },
                                        onClick = { sortMenu = false; viewModel.setFolderSort(sort) },
                                        trailingIcon = if (sort == folderSort) {
                                            {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_check),
                                                    contentDescription = null,
                                                )
                                            }
                                        } else null,
                                    )
                                }
                            }
                        }
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
                // In-folder grid → grid skeleton; folder list and in-folder list both use
                // the row-shaped skeleton so content swaps in without a layout shift.
                if (pendingGrid && viewMode == ViewMode.GRID) {
                    LibrarySkeleton(columns = 3, modifier = Modifier.padding(padding))
                } else {
                    FolderListSkeleton(modifier = Modifier.padding(padding))
                }

            is FolderBrowserUiState.FolderList -> Column(modifier = Modifier.padding(padding)) {
                if (!selectionMode) {
                    com.devbytes.vixplayer.app.briefing.ui.BriefingHomeCard(onOpen = onOpenBriefing)
                }
                if (partialAccess && !selectionMode) {
                    PartialAccessBanner(
                        onManage = {
                            mediaAccessLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_MEDIA_VIDEO,
                                    "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
                                ),
                            )
                        },
                    )
                }
                FolderList(
                    folders = s.folders,
                    onFolderClick = { folder ->
                        pendingGrid = true
                        viewModel.enterFolder(folder.bucketId, folder.name)
                    },
                    showAds = showAds,
                    modifier = Modifier.weight(1f),
                )
            }

            is FolderBrowserUiState.VideoList -> VideoGrid(
                videos = s.videos,
                onVideoClick = onVideoClick,
                selected = selected,
                selectionMode = selectionMode,
                viewMode = viewMode,
                resume = resume,
                onToggleSelect = { viewModel.toggleSelection(it) },
                showAds = showAds,
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
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    // Create-a-new-folder entry, always available (even with no existing folders).
                    Text(
                        text = "+  New folder…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                choosingDestination = null
                                newFolderName = ""
                                newFolderMove = isMove
                            }
                            .padding(vertical = 12.dp),
                    )
                    if (destinations.isEmpty()) {
                        Text(
                            text = "No other folders with videos yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    } else {
                        destinations.forEach { folder ->
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        choosingDestination = null
                                        viewModel.transferSelection(folder, isMove) { sender ->
                                            deleteConsent.launch(
                                                androidx.activity.result.IntentSenderRequest
                                                    .Builder(sender).build()
                                            )
                                        }
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

    // Name a new destination folder. MediaStore can't make an empty dir, so the folder
    // appears once the moved/copied files land in it.
    newFolderMove?.let { isMove ->
        val existing = remember(destinations) { destinations.map { it.name.lowercase() }.toSet() }
        val error = newFolderNameError(newFolderName, existing)
        AlertDialog(
            onDismissRequest = { newFolderMove = null },
            title = { Text("New folder") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        singleLine = true,
                        isError = newFolderName.isNotEmpty() && error != null,
                        label = { Text("Folder name") },
                    )
                    if (newFolderName.isNotEmpty() && error != null) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = error == null,
                    onClick = {
                        val name = newFolderName
                        newFolderMove = null
                        viewModel.transferToNewFolder(name, isMove) { sender ->
                            deleteConsent.launch(
                                androidx.activity.result.IntentSenderRequest.Builder(sender).build()
                            )
                        }
                    },
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { newFolderMove = null }) { Text("Cancel") }
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

/** Validates a proposed new-folder name; returns an error message, or null when valid. */
private fun newFolderNameError(raw: String, existingLowercase: Set<String>): String? {
    val name = raw.trim()
    return when {
        name.isBlank() -> "Enter a name"
        name.startsWith(".") -> "Name can't start with a dot"
        name.any { it in "/\\:*?\"<>|" || it.code < 32 } -> "Name has invalid characters"
        name.length > 100 -> "Name is too long"
        name.trimEnd('.', ' ').lowercase() in existingLowercase ->
            "A folder with that name already exists"
        else -> null
    }
}

/** True only on Android 14+ when access is the partial "selected videos" grant, not full. */
private fun isPartialMediaAccess(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
    fun granted(p: String) =
        ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
    return !granted(Manifest.permission.READ_MEDIA_VIDEO) &&
        granted("android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
}

/**
 * A calm, non-blocking notice that only some videos are shared, with a one-tap path to widen
 * the grant. Reuses the surface/token vocabulary rather than a hardcoded style.
 */
@Composable
private fun PartialAccessBanner(onManage: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "You've shared only some videos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onManage) { Text("Manage") }
        }
    }
}

/** Video-tab home header: "Vix" bold, "Play" italic + light. */
@Composable
internal fun VideoHeaderTitle() {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Vix") }
            withStyle(
                SpanStyle(fontWeight = FontWeight.Light, fontStyle = FontStyle.Italic),
            ) { append("Play") }
        },
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun FolderList(
    folders: List<FolderEntry>,
    onFolderClick: (FolderEntry) -> Unit,
    showAds: Boolean,
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
    // One native card a few rows down (or at the end for short lists), so it's inline and
    // seen rather than buried at the bottom.
    val adAfter = if (folders.size > 3) 3 else folders.size
    LazyColumn(modifier = modifier.fillMaxSize()) {
        folders.take(adAfter).forEach { folder ->
            item(key = folder.bucketId) {
                FolderRow(folder = folder, onClick = { onFolderClick(folder) })
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            }
        }
        if (showAds) {
            item(key = "native_folder_ad") {
                AdNativeCard(
                    adUnitId = AdUnits.nativeFolderList,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        folders.drop(adAfter).forEach { folder ->
            item(key = folder.bucketId) {
                FolderRow(folder = folder, onClick = { onFolderClick(folder) })
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            }
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
            // A folder icon rather than a sample frame: this is a container of videos,
            // and a single frame misrepresents the folder's contents.
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_folder),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp),
                    )
                }
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

private const val VIDEO_LIST_AD_INTERVAL = 8

@Composable
private fun VideoGrid(
    videos: List<VideoFile>,
    onVideoClick: (Uri) -> Unit,
    selected: Set<Long>,
    selectionMode: Boolean,
    viewMode: ViewMode,
    resume: Map<Long, Float>,
    onToggleSelect: (VideoFile) -> Unit,
    showAds: Boolean,
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
    val isList = viewMode == ViewMode.LIST
    val fullSpan: androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> GridItemSpan =
        { GridItemSpan(maxLineSpan) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isList) 1 else 3),
        contentPadding = PaddingValues(if (isList) 0.dp else 12.dp),
        horizontalArrangement = Arrangement.spacedBy(if (isList) 0.dp else 8.dp),
        verticalArrangement = Arrangement.spacedBy(if (isList) 0.dp else 8.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        // Banner across the top of the video list.
        if (showAds) {
            item(span = fullSpan, key = "video_banner") {
                AdBanner(
                    adUnitId = AdUnits.bannerVideoList,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
        videos.forEachIndexed { index, video ->
            item(key = video.mediaStoreId) {
                val onClick: () -> Unit = {
                    if (selectionMode) onToggleSelect(video) else onVideoClick(video.uri)
                }
                if (isList) {
                    VideoListRow(
                        video = video,
                        resumeFraction = resume[video.mediaStoreId] ?: 0f,
                        isSelected = video.mediaStoreId in selected,
                        onClick = onClick,
                        onLongClick = { onToggleSelect(video) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                } else {
                    VideoCard(
                        video = video,
                        resumeFraction = resume[video.mediaStoreId] ?: 0f,
                        isSelected = video.mediaStoreId in selected,
                        onClick = onClick,
                        onLongClick = { onToggleSelect(video) },
                    )
                }
            }
            // Native card inline every N videos.
            if (showAds && (index + 1) % VIDEO_LIST_AD_INTERVAL == 0 && index != videos.lastIndex) {
                item(span = fullSpan, key = "native_video_ad_$index") {
                    AdNativeCard(
                        adUnitId = AdUnits.nativeVideoList,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
