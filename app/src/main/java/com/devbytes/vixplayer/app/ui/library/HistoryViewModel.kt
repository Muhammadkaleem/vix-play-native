package com.devbytes.vixplayer.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.MediaRepository
import com.devbytes.vixplayer.app.data.repository.PlaybackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data object Empty : HistoryUiState
    data class Populated(val entries: List<VideoWithPosition>) : HistoryUiState
}

/**
 * Recently-played history: the full chronological set of resume positions
 * (`lastPlayedAt DESC`) joined to their videos. Unlike the library's
 * continue-watching rail this keeps finished items (shown as "Watched") and
 * applies no in-progress filter. Missing sources are purged on read via
 * `mapNotNull` (matches the PRD's purge-on-delete default).
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val playbackRepository: PlaybackRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init { load() }

    fun clearHistory() {
        viewModelScope.launch {
            playbackRepository.clearAll()
            _state.value = HistoryUiState.Empty
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = HistoryUiState.Loading
            val positions = playbackRepository.getRecentPositions(limit = 200)
            val videoMap = mediaRepository.queryAllVideos().associateBy { it.mediaStoreId }
            val entries = positions.mapNotNull { pos ->
                videoMap[pos.mediaStoreId]?.let { video ->
                    VideoWithPosition(video, pos.positionMs, pos.lastPlayedAt)
                }
            }
            _state.value = if (entries.isEmpty()) HistoryUiState.Empty
                           else HistoryUiState.Populated(entries)
        }
    }
}
