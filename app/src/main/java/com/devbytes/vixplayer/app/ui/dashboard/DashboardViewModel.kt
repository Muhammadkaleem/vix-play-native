package com.devbytes.vixplayer.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.FavoritesRepository
import com.devbytes.vixplayer.app.data.repository.FolderEntry
import com.devbytes.vixplayer.app.data.repository.MediaRepository
import com.devbytes.vixplayer.app.data.repository.PlaybackRepository
import com.devbytes.vixplayer.app.data.repository.SettingsRepository
import com.devbytes.vixplayer.app.data.repository.VideoFile
import com.devbytes.vixplayer.app.ui.library.VideoWithPosition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Assembles the Home dashboard from the existing repos. Loaded on entry and re-read on return
 * (and after a favourite/pin toggle); sections that come back empty are collapsed by the screen.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val mediaRepository: MediaRepository,
    private val favoritesRepository: FavoritesRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    data class PinnableFolder(val folder: FolderEntry, val isPinned: Boolean)

    data class DashboardState(
        val loading: Boolean = true,
        val continueHero: VideoWithPosition? = null,
        val fromThisFolder: List<VideoFile> = emptyList(),
        val folders: List<PinnableFolder> = emptyList(),
        val recentlyAdded: List<VideoFile> = emptyList(),
        val favorites: List<VideoFile> = emptyList(),
        val shortsCount: Int = 0,
        val shortsSamples: List<VideoFile> = emptyList(),
    )

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            // Continue watching: most-recent in-progress video (2%..97%).
            val inProgress = playbackRepository.getRecentPositions(20).mapNotNull { pos ->
                val video = runCatching { mediaRepository.queryVideoById(pos.mediaStoreId) }.getOrNull()
                    ?: return@mapNotNull null
                VideoWithPosition(video, pos.positionMs, pos.lastPlayedAt)
                    .takeIf { it.progressFraction in 0.02f..0.97f }
            }
            val hero = inProgress.firstOrNull()
            val fromFolder = hero
                ?.let { runCatching { mediaRepository.videosInFolderOf(it.video.mediaStoreId) }.getOrNull() }
                ?.take(10).orEmpty()

            // Folders, pinned first (queryFolders order preserved within each group).
            val pinned = settingsRepository.pinnedFolders.first()
            val folders = runCatching { mediaRepository.queryFolders() }.getOrDefault(emptyList())
                .map { PinnableFolder(it, it.bucketId in pinned) }
                .sortedByDescending { it.isPinned }
                .take(4)

            val recentlyAdded = runCatching { mediaRepository.queryRecentlyAdded(5) }.getOrDefault(emptyList())

            val favIds = favoritesRepository.observeIds().first()
            val favorites = runCatching { mediaRepository.queryVideosByIds(favIds) }.getOrDefault(emptyList()).take(4)

            val shortsCount = runCatching { mediaRepository.countShorts() }.getOrDefault(0)
            val shortsSamples = if (shortsCount > 0) {
                runCatching { mediaRepository.queryShorts(3) }.getOrDefault(emptyList())
            } else emptyList()

            _state.value = DashboardState(
                loading = false,
                continueHero = hero,
                fromThisFolder = fromFolder,
                folders = folders,
                recentlyAdded = recentlyAdded,
                favorites = favorites,
                shortsCount = shortsCount,
                shortsSamples = shortsSamples,
            )
        }
    }

    fun toggleFavorite(mediaStoreId: Long) = viewModelScope.launch {
        favoritesRepository.toggle(mediaStoreId); refresh()
    }

    fun togglePin(bucketId: Long) = viewModelScope.launch {
        settingsRepository.togglePinnedFolder(bucketId); refresh()
    }
}
