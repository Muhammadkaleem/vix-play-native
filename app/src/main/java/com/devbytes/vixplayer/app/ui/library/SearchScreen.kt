package com.devbytes.vixplayer.app.ui.library

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFramePercent
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.data.repository.AudioTrack
import com.devbytes.vixplayer.app.data.repository.FolderEntry
import com.devbytes.vixplayer.app.data.repository.VideoFile
import com.devbytes.vixplayer.app.ui.audio.AlbumArt
import com.devbytes.vixplayer.app.ui.audio.formatTrackDuration
import com.devbytes.vixplayer.app.ui.library.components.LibraryEmptyState
import com.devbytes.vixplayer.app.ui.theme.OnScrim
import com.devbytes.vixplayer.app.ui.theme.ScrimStrong
import com.devbytes.vixplayer.app.ui.common.screenInsets
import com.devbytes.vixplayer.app.ui.common.focusRing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    initialQuery: String? = null,
    onVideoClick: (Uri) -> Unit = {},
    onAudioClick: (Long) -> Unit = {},
    onFolderClick: (Long, String) -> Unit = { _, _ -> },
    onBack: () -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val recents by viewModel.recentSearches.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (!initialQuery.isNullOrBlank()) viewModel.setQuery(initialQuery)
        focusRequester.requestFocus()
    }

    Scaffold(
        contentWindowInsets = screenInsets(),
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = viewModel::setQuery,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        placeholder = { Text("Search videos, audio, folders") },
                        singleLine = true,
                        trailingIcon = if (query.isNotEmpty()) {
                            {
                                IconButton(onClick = { viewModel.setQuery("") }) {
                                    Icon(painterResource(R.drawable.ic_close), contentDescription = "Clear")
                                }
                            }
                        } else null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            viewModel.commitSearch()
                            keyboard?.hide()
                        }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val results = uiState as? SearchUiState.Results
            if (results != null && !results.isEmpty) {
                CategoryTabs(
                    category = category,
                    videoCount = results.videos.size,
                    audioCount = results.audio.size,
                    folderCount = results.folders.size,
                    onSelect = viewModel::setCategory,
                )
            }

            when (val state = uiState) {
                is SearchUiState.Idle -> IdleContent(
                    recents = recents,
                    onPick = { viewModel.setQuery(it) },
                    onClearRecents = viewModel::clearRecentSearches,
                )

                is SearchUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                is SearchUiState.Results ->
                    if (state.isEmpty) {
                        LibraryEmptyState(
                            iconRes = R.drawable.ic_search,
                            title = "No matches",
                            body = "No files or folders match \"${state.query}\".",
                        )
                    } else {
                        // Fade only on tab switches; typing updates the list in place (no flicker).
                        Crossfade(targetState = category, label = "searchCategory") { cat ->
                            ResultsList(
                                state = state,
                                category = cat,
                                onVideo = { v -> viewModel.commitSearch(); onVideoClick(v.uri) },
                                onAudio = { a -> viewModel.commitSearch(); onAudioClick(a.mediaStoreId) },
                                onFolder = { f -> viewModel.commitSearch(); onFolderClick(f.bucketId, f.name) },
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun CategoryTabs(
    category: SearchCategory,
    videoCount: Int,
    audioCount: Int,
    folderCount: Int,
    onSelect: (SearchCategory) -> Unit,
) {
    fun countFor(c: SearchCategory) = when (c) {
        SearchCategory.ALL -> videoCount + audioCount + folderCount
        SearchCategory.VIDEOS -> videoCount
        SearchCategory.AUDIO -> audioCount
        SearchCategory.FOLDERS -> folderCount
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SearchCategory.entries.forEach { c ->
            val active = c == category
            Box(
                Modifier.clip(RoundedCornerShape(50))
                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    .clickable { onSelect(c) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    "${c.label} ${countFor(c)}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdleContent(recents: List<String>, onPick: (String) -> Unit, onClearRecents: () -> Unit) {
    if (recents.isEmpty()) {
        LibraryEmptyState(
            iconRes = R.drawable.ic_search,
            title = "Search your library",
            body = "Find videos, audio, and folders by name.",
        )
        return
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Recent searches", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClearRecents) { Text("Clear") }
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            recents.forEach { r ->
                Box(
                    Modifier.clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        .clickable { onPick(r) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(r, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun ResultsList(
    state: SearchUiState.Results,
    category: SearchCategory,
    onVideo: (VideoFile) -> Unit,
    onAudio: (AudioTrack) -> Unit,
    onFolder: (FolderEntry) -> Unit,
) {
    val showVideos = category == SearchCategory.ALL || category == SearchCategory.VIDEOS
    val showAudio = category == SearchCategory.ALL || category == SearchCategory.AUDIO
    val showFolders = category == SearchCategory.ALL || category == SearchCategory.FOLDERS
    val grouped = category == SearchCategory.ALL

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (showVideos && state.videos.isNotEmpty()) {
            if (grouped) item("h-v") { SectionLabel("Videos", state.videos.size) }
            items(state.videos, key = { "v${it.mediaStoreId}" }) { v -> VideoResultCard(v) { onVideo(v) } }
        }
        if (showAudio && state.audio.isNotEmpty()) {
            if (grouped) item("h-a") { SectionLabel("Audio", state.audio.size) }
            items(state.audio, key = { "a${it.mediaStoreId}" }) { a -> AudioResultCard(a) { onAudio(a) } }
        }
        if (showFolders && state.folders.isNotEmpty()) {
            if (grouped) item("h-f") { SectionLabel("Folders", state.folders.size) }
            items(state.folders, key = { "f${it.bucketId}" }) { f -> FolderResultCard(f) { onFolder(f) } }
        }
    }
}

@Composable
private fun SectionLabel(title: String, count: Int) {
    Text(
        "$title · $count",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 2.dp),
    )
}

@Composable
private fun VideoResultCard(video: VideoFile, onClick: () -> Unit) {
    ResultRow(onClick) {
        Box(Modifier.width(104.dp).height(60.dp).clip(RoundedCornerShape(10.dp))) {
            AsyncImage(
                model = videoThumb(video.uri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            qualityLabel(video.width, video.height)?.let { q ->
                Box(
                    Modifier.align(Alignment.TopEnd).padding(5.dp).clip(RoundedCornerShape(6.dp))
                        .background(ScrimStrong).padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text(q, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), fontWeight = FontWeight.SemiBold, color = OnScrim)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Title(video.name.substringBeforeLast('.'))
            Spacer(Modifier.height(3.dp))
            Subtitle(
                listOfNotNull(
                    videoDuration(video.durationMs),
                    formatSize(video.sizeBytes).takeIf { video.sizeBytes > 0 },
                    extOf(video.name),
                ).joinToString(" · "),
            )
        }
    }
}

@Composable
private fun AudioResultCard(track: AudioTrack, onClick: () -> Unit) {
    ResultRow(onClick) {
        AlbumArt(uri = track.albumArtUri.toString(), size = 52.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Title(track.title)
            Spacer(Modifier.height(3.dp))
            Subtitle(
                listOf(
                    track.artist.ifBlank { "Unknown artist" },
                    formatTrackDuration(track.durationMs),
                ).joinToString(" · "),
            )
        }
        Icon(
            painterResource(R.drawable.ic_nav_audio),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun FolderResultCard(folder: FolderEntry, onClick: () -> Unit) {
    ResultRow(onClick) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant), Alignment.Center) {
            AsyncImage(
                model = videoThumb(folder.sampleUri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Title(folder.name)
            Spacer(Modifier.height(3.dp))
            Subtitle("${folder.videoCount} ${if (folder.videoCount == 1) "video" else "videos"}")
        }
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Shared compact result row with a press-scale micro-interaction. */
@Composable
private fun ResultRow(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "pressScale")
    Row(
        Modifier.fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .focusRing(interaction, RoundedCornerShape(14.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun Title(text: String) = Text(
    text,
    style = MaterialTheme.typography.bodyLarge,
    fontWeight = FontWeight.SemiBold,
    color = MaterialTheme.colorScheme.onSurface,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
)

@Composable
private fun Subtitle(text: String) = Text(
    text,
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
)

@Composable
private fun videoThumb(uri: Uri) = ImageRequest.Builder(LocalContext.current)
    .data(uri)
    .decoderFactory(VideoFrameDecoder.Factory())
    .videoFramePercent(0.1)
    .build()

private fun qualityLabel(width: Int, height: Int): String? {
    val p = minOf(width, height)
    return when {
        p >= 2160 -> "4K"
        p >= 1080 -> "1080p"
        p >= 720 -> "720p"
        else -> null
    }
}

private fun videoDuration(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

private fun formatSize(bytes: Long): String {
    val gb = bytes / 1_000_000_000.0
    val mb = bytes / 1_000_000.0
    return if (gb >= 1.0) "%.1f GB".format(gb) else "%.0f MB".format(mb)
}

private fun extOf(name: String): String = name.substringAfterLast('.', "").uppercase().ifBlank { "VIDEO" }
