package com.devbytes.vixplayer.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.FavoritesRepository
import com.devbytes.vixplayer.app.data.repository.MediaRepository
import com.devbytes.vixplayer.app.data.repository.VideoFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the shared "View all" list for Recently added / Favorites / Shorts. */
@HiltViewModel
class VideoListViewModel @Inject constructor(
    private val media: MediaRepository,
    private val favorites: FavoritesRepository,
) : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoFile>?>(null) // null = loading
    val videos: StateFlow<List<VideoFile>?> = _videos.asStateFlow()

    fun load(source: DashboardSource) {
        viewModelScope.launch {
            _videos.value = runCatching {
                when (source) {
                    DashboardSource.RECENTLY_ADDED -> media.queryRecentlyAdded(500)
                    DashboardSource.FAVORITES -> media.queryVideosByIds(favorites.observeIds().first())
                }
            }.getOrDefault(emptyList())
        }
    }

    /** All videos in one folder — backs the "open a folder" tap from the dashboard. */
    fun loadFolder(bucketId: Long) {
        viewModelScope.launch {
            _videos.value = runCatching { media.queryVideosInFolder(bucketId) }.getOrDefault(emptyList())
        }
    }
}
