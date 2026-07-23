package com.devbytes.vixplayer.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.MediaRepository
import com.devbytes.vixplayer.app.data.repository.PlaybackRepository
import com.devbytes.vixplayer.app.data.repository.VideoFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ViewMode { GRID, LIST }

enum class SortOrder { RECENT, NAME, DURATION, SIZE }

data class VideoWithPosition(
    val video: VideoFile,
    val positionMs: Long,
    val lastPlayedAt: Long,
) {
    val progressFraction: Float get() =
        if (video.durationMs > 0) positionMs.toFloat() / video.durationMs else 0f
}

data class VideoLibraryUiState(
    val hasPermission: Boolean = false,
    val isLoading: Boolean = false,
    val allVideos: List<VideoFile> = emptyList(),
    val continueWatching: List<VideoWithPosition> = emptyList(),
    val viewMode: ViewMode = ViewMode.GRID,
    val sortOrder: SortOrder = SortOrder.RECENT,
    val error: String? = null,
)

@HiltViewModel
class VideoLibraryViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val playbackRepository: PlaybackRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoLibraryUiState())
    val uiState: StateFlow<VideoLibraryUiState> = _uiState.asStateFlow()

    // Query order (DATE_MODIFIED DESC) preserved so SortOrder.RECENT stays available.
    private var rawVideos: List<VideoFile> = emptyList()

    fun onPermissionGranted() {
        _uiState.value = _uiState.value.copy(hasPermission = true)
        loadContent()
    }

    fun onPermissionDenied() {
        _uiState.value = _uiState.value.copy(hasPermission = false)
    }

    fun toggleViewMode() {
        _uiState.value = _uiState.value.copy(
            viewMode = if (_uiState.value.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID,
        )
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.value = _uiState.value.copy(
            sortOrder = order,
            allVideos = sortedBy(rawVideos, order),
        )
    }

    private fun sortedBy(list: List<VideoFile>, order: SortOrder): List<VideoFile> = when (order) {
        SortOrder.RECENT -> list
        SortOrder.NAME -> list.sortedBy { it.name.lowercase() }
        SortOrder.DURATION -> list.sortedByDescending { it.durationMs }
        SortOrder.SIZE -> list.sortedByDescending { it.sizeBytes }
    }

    private fun loadContent() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            runCatching {
                val videos = mediaRepository.queryAllVideos()
                rawVideos = videos
                val recentPositions = playbackRepository.getRecentPositions()
                val videoMap = videos.associateBy { it.mediaStoreId }

                val continueWatching = recentPositions
                    .mapNotNull { pos ->
                        videoMap[pos.mediaStoreId]?.let { video ->
                            VideoWithPosition(video, pos.positionMs, pos.lastPlayedAt)
                        }
                    }
                    .filter { it.progressFraction in 0.02f..0.97f }

                _uiState.value = _uiState.value.copy(
                    allVideos = sortedBy(videos, _uiState.value.sortOrder),
                    continueWatching = continueWatching,
                    isLoading = false,
                    error = null,
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
