package com.devbytes.vixplayer.app.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.data.repository.VideoFile
import com.devbytes.vixplayer.app.ui.library.components.LibraryEmptyState
import com.devbytes.vixplayer.app.ui.library.components.VideoCard

/**
 * Shared flat video list. Either backs a [source] (Recently added / Favorites) or, when
 * [folderBucketId] is set, lists one folder's videos (the dashboard "Your folders" tap).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoListScreen(
    source: DashboardSource? = null,
    folderBucketId: Long? = null,
    folderTitle: String? = null,
    onVideoClick: (VideoFile) -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: VideoListViewModel = hiltViewModel(),
) {
    val videos by viewModel.videos.collectAsState()
    LaunchedEffect(source, folderBucketId) {
        if (folderBucketId != null) viewModel.loadFolder(folderBucketId)
        else source?.let { viewModel.load(it) }
    }
    val title = folderTitle ?: source?.title.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        val list = videos
        when {
            list == null ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            list.isEmpty() ->
                LibraryEmptyState(
                    iconRes = R.drawable.ic_nav_video,
                    title = "Nothing here yet",
                    body = when (source) {
                        DashboardSource.FAVORITES -> "Tap the heart on a video to add it to favorites."
                        DashboardSource.RECENTLY_ADDED -> "New videos you add will appear here."
                        DashboardSource.HIGH_RES -> "No 1080p or higher videos found in your storage."
                        DashboardSource.NEW_ON_DEVICE -> "Nothing added in the last 7 days."
                        null -> "This folder has no videos."
                    },
                    modifier = Modifier.padding(padding),
                )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                items(list, key = { it.mediaStoreId }) { video ->
                    VideoCard(video = video, onClick = { onVideoClick(video) })
                }
            }
        }
    }
}
