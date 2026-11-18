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

    data class LibraryCategory(val name: String, val count: Int)

    data class LibraryStats(
        val videoCount: Int,
        val totalBytes: Long,
        val unwatchedMs: Long,
        val categories: List<LibraryCategory>,
    )

    data class DashboardState(
        val loading: Boolean = true,
        val stats: LibraryStats? = null,
        val continueHero: VideoWithPosition? = null,
        // Up to 5 in-progress videos (newest playback first) for the Continue Watching carousel.
        val continueWatching: List<VideoWithPosition> = emptyList(),
        // Folder videos paired with resume position → "Xm left" + progress bar when in progress.
        val fromThisFolder: List<VideoWithPosition> = emptyList(),
        val folders: List<PinnableFolder> = emptyList(),
        val recentlyAdded: List<VideoFile> = emptyList(),
        // Videos added in the last 7 days (newest first) for the "New on this device" list.
        val newOnDevice: List<VideoFile> = emptyList(),
        val highRes: List<VideoFile> = emptyList(),
        // Videos ≤ 8h (longest-fitting first); the "How much time do you have?" rail filters this.
        val timeWindowPool: List<VideoFile> = emptyList(),
        val favorites: List<VideoFile> = emptyList(),
        val shortsCount: Int = 0,
        val shortsSamples: List<VideoFile> = emptyList(),
    )

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            // One pass over recent positions: drives Continue Watching + the "From this folder"
            // resume labels (so a folder video shows "Xm left" if it's been partly watched).
            val recentPositions = runCatching { playbackRepository.getRecentPositions(200) }.getOrDefault(emptyList())
            val posByMediaId = recentPositions.associateBy { it.mediaStoreId }

            // Continue watching: most-recent in-progress video (2%..97%).
            val inProgress = recentPositions.take(20).mapNotNull { pos ->
                val video = runCatching { mediaRepository.queryVideoById(pos.mediaStoreId) }.getOrNull()
                    ?: return@mapNotNull null
                VideoWithPosition(video, pos.positionMs, pos.lastPlayedAt)
                    .takeIf { it.progressFraction in 0.02f..0.97f }
            }
            val hero = inProgress.firstOrNull()
            val continueWatching = inProgress.take(5)
            val fromFolder = hero
                ?.let { runCatching { mediaRepository.videosInFolderOf(it.video.mediaStoreId) }.getOrNull() }
                ?.take(10).orEmpty()
                .map { v ->
                    val p = posByMediaId[v.mediaStoreId]
                    VideoWithPosition(v, p?.positionMs ?: 0L, p?.lastPlayedAt ?: 0L)
                }

            // Folders, pinned first (queryFolders order preserved within each group).
            val pinned = settingsRepository.pinnedFolders.first()
            val allFolders = runCatching { mediaRepository.queryFolders() }.getOrDefault(emptyList())
            val folders = allFolders
                .map { PinnableFolder(it, it.bucketId in pinned) }
                .sortedByDescending { it.isPinned }
                .take(4)

            // Library overview stats (count / size / unwatched hours / category breakdown).
            val allVideos = runCatching { mediaRepository.queryAllVideos() }.getOrDefault(emptyList())
            val watchedIds = runCatching { playbackRepository.getRecentPositions(10000) }
                .getOrDefault(emptyList())
                .filter { it.positionMs > 0 }.map { it.mediaStoreId }.toSet()
            val byCount = allFolders.sortedByDescending { it.videoCount }
            val categories = buildList {
                byCount.take(3).forEach { add(LibraryCategory(it.name, it.videoCount)) }
                val other = byCount.drop(3).sumOf { it.videoCount }
                if (other > 0) add(LibraryCategory("Other", other))
            }
            val stats = LibraryStats(
                videoCount = allVideos.size,
                totalBytes = allVideos.sumOf { it.sizeBytes },
                unwatchedMs = allVideos.filter { it.mediaStoreId !in watchedIds }.sumOf { it.durationMs },
                categories = categories,
            )

            val recent = runCatching { mediaRepository.queryRecentlyAdded(30) }.getOrDefault(emptyList())
            val recentlyAdded = recent.take(5)
            val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 3_600_000L
            val newOnDevice = recent.filter { it.dateAddedMs >= sevenDaysAgo }

            val highRes = runCatching { mediaRepository.queryHighResVideos(5) }.getOrDefault(emptyList())

            // Pool for the "How much time do you have?" rail: everything up to ~8h (All night).
            val timeWindowPool = runCatching {
                mediaRepository.queryByMaxDuration(8L * 3_600_000L, 60)
            }.getOrDefault(emptyList())

            val favIds = favoritesRepository.observeIds().first()
            val favorites = runCatching { mediaRepository.queryVideosByIds(favIds) }.getOrDefault(emptyList()).take(4)

            val shortsCount = runCatching { mediaRepository.countShorts() }.getOrDefault(0)
            val shortsSamples = if (shortsCount > 0) {
                runCatching { mediaRepository.queryShorts(3) }.getOrDefault(emptyList())
            } else emptyList()

            _state.value = DashboardState(
                loading = false,
                stats = stats,
                continueHero = hero,
                continueWatching = continueWatching,
                fromThisFolder = fromFolder,
                folders = folders,
                recentlyAdded = recentlyAdded,
                newOnDevice = newOnDevice,
                highRes = highRes,
                timeWindowPool = timeWindowPool,
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
