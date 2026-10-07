package com.devbytes.vixplayer.app.ui.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFramePercent
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.data.repository.VideoFile
import com.devbytes.vixplayer.app.ui.library.VideoHeaderTitle
import com.devbytes.vixplayer.app.ui.library.components.VideoCard
import com.devbytes.vixplayer.app.ui.theme.OnScrim
import com.devbytes.vixplayer.app.ui.theme.ScrimStrong
import com.devbytes.vixplayer.app.ui.theme.TrackInactive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onVideoClick: (VideoFile) -> Unit = {},
    onFolderClick: (bucketId: Long, name: String) -> Unit = { _, _ -> },
    onBrowseFolders: () -> Unit = {},
    onViewAll: (DashboardSource) -> Unit = {},
    onOpenShorts: () -> Unit = {},
    onSearch: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Re-read every time the dashboard comes to the foreground (tab re-selection, returning from
    // the player), so favourites / pins / new videos / resume positions reflect back — a plain
    // LaunchedEffect(Unit) only fires on first composition and would show stale data.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { VideoHeaderTitle() },
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(painterResource(R.drawable.ic_search), contentDescription = "Search")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 1. Continue watching hero
            state.continueHero?.let { hero ->
                item { ContinueHero(hero.video, hero.progressFraction, hero.positionMs, onClick = { onVideoClick(hero.video) }) }
            }

            // 2. From this folder
            if (state.fromThisFolder.isNotEmpty()) {
                item { SectionHeader("From this folder", onViewAll = onBrowseFolders) }
                item { VideoRail(state.fromThisFolder, onVideoClick) }
            }

            // 3. Your folders
            if (state.folders.isNotEmpty()) {
                item { SectionHeader("Your folders", onViewAll = onBrowseFolders) }
                items2(state.folders) { pf ->
                    FolderCard(
                        name = pf.folder.name,
                        count = pf.folder.videoCount,
                        sampleUri = pf.folder.sampleUri,
                        isPinned = pf.isPinned,
                        onClick = { onFolderClick(pf.folder.bucketId, pf.folder.name) },
                        onLongClick = { viewModel.togglePin(pf.folder.bucketId) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // 4. Recently added — horizontal rail of the latest 5, "View all" opens the full list.
            if (state.recentlyAdded.isNotEmpty()) {
                item { SectionHeader("Recently added", onViewAll = { onViewAll(DashboardSource.RECENTLY_ADDED) }) }
                item { VideoRail(state.recentlyAdded, onVideoClick) }
            }

            // 5. Favorites
            if (state.favorites.isNotEmpty()) {
                item { SectionHeader("Favorites", onViewAll = { onViewAll(DashboardSource.FAVORITES) }) }
                items(state.favorites.size) { i ->
                    val video = state.favorites[i]
                    FavoriteRow(
                        video = video,
                        onClick = { onVideoClick(video) },
                        onUnfavorite = { viewModel.toggleFavorite(video.mediaStoreId) },
                    )
                }
            }

            // 6. Your Shorts — opens the TikTok-style vertical feed.
            if (state.shortsCount > 0) {
                item { SectionHeader("Your Shorts", onViewAll = null) }
                item {
                    ShortsCard(
                        samples = state.shortsSamples,
                        count = state.shortsCount,
                        onClick = onOpenShorts,
                    )
                }
            }
        }
    }
}

/** Lays [items] out two-per-row inside a LazyColumn (can't nest a LazyVerticalGrid). */
private fun <T> androidx.compose.foundation.lazy.LazyListScope.items2(
    items: List<T>,
    content: @Composable androidx.compose.foundation.layout.RowScope.(T) -> Unit,
) {
    val rows = items.chunked(2)
    items(rows.size) { r ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            rows[r].forEach { content(it) }
            if (rows[r].size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun SectionHeader(title: String, onViewAll: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (onViewAll != null) {
            TextButton(onClick = onViewAll) { Text("View all") }
        }
    }
}

@Composable
private fun thumbRequest(uri: Any?) = ImageRequest.Builder(LocalContext.current)
    .data(uri)
    .decoderFactory(VideoFrameDecoder.Factory())
    .videoFramePercent(0.1)
    .build()

@Composable
private fun ContinueHero(video: VideoFile, fraction: Float, positionMs: Long, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text(
            "Continue watching",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.combinedClickableSafe(onClick),
        ) {
            Column {
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))) {
                    AsyncImage(
                        model = thumbRequest(video.uri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Surface(
                        color = ScrimStrong,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                    ) {
                        Text("LAST PLAYED", style = MaterialTheme.typography.labelSmall, color = OnScrim,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                    if (fraction > 0f) {
                        Box(Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomStart).background(TrackInactive))
                        Box(Modifier.fillMaxWidth(fraction).height(3.dp).align(Alignment.BottomStart)
                            .background(MaterialTheme.colorScheme.primary))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(video.name, style = MaterialTheme.typography.titleSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Resume at ${formatClock(positionMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = onClick) {
                        Icon(painterResource(R.drawable.ic_play), contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  Resume")
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoRail(videos: List<VideoFile>, onClick: (VideoFile) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(videos.size) { i ->
            VideoCard(video = videos[i], onClick = { onClick(videos[i]) }, modifier = Modifier.width(220.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderCard(
    name: String,
    count: Int,
    sampleUri: Any?,
    isPinned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AsyncImage(
                model = thumbRequest(sampleUri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(width = 64.dp, height = 44.dp).clip(RoundedCornerShape(6.dp)),
            )
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPinned) Text("📌 ", style = MaterialTheme.typography.bodySmall)
                    Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("$count ${if (count == 1) "video" else "videos"}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FavoriteRow(video: VideoFile, onClick: () -> Unit, onUnfavorite: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .combinedClickableSafe(onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = thumbRequest(video.uri),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 72.dp, height = 44.dp).clip(RoundedCornerShape(6.dp)),
        )
        Text(video.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        IconButton(onClick = onUnfavorite) {
            Icon(painterResource(R.drawable.ic_favorite), contentDescription = "Remove from favorites",
                tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ShortsCard(samples: List<VideoFile>, count: Int, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).combinedClickableSafe(onClick),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            samples.take(3).forEach { v ->
                AsyncImage(
                    model = thumbRequest(v.uri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(width = 44.dp, height = 68.dp).clip(RoundedCornerShape(6.dp)),
                )
            }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text("Your vertical videos", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("$count ${if (count == 1) "video" else "videos"}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableSafe(onClick: () -> Unit): Modifier =
    this.combinedClickable(onClick = onClick, onLongClick = null)

/** m:ss / h:mm:ss for the resume position. */
private fun formatClock(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
