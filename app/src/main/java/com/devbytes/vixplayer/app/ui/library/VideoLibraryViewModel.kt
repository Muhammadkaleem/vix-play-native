package com.devbytes.vixplayer.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.DeleteResult
import com.devbytes.vixplayer.app.data.repository.MediaDeleter
import com.devbytes.vixplayer.app.data.repository.MediaRepository
import com.devbytes.vixplayer.app.data.repository.PlaybackRepository
import com.devbytes.vixplayer.app.data.repository.PrivateVault
import com.devbytes.vixplayer.app.data.repository.SettingsRepository
import com.devbytes.vixplayer.app.ui.common.SelectionHolder
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
    private val mediaDeleter: MediaDeleter,
    private val settingsRepository: SettingsRepository,
    private val privateVault: PrivateVault,
) : ViewModel() {

    init {
        // Restore the persisted view mode; toggling writes it back.
        viewModelScope.launch {
            settingsRepository.viewMode.collect { mode ->
                _uiState.value = _uiState.value.copy(
                    viewMode = runCatching { ViewMode.valueOf(mode) }.getOrDefault(ViewMode.GRID),
                )
            }
        }
    }

    private val _uiState = MutableStateFlow(VideoLibraryUiState())
    val uiState: StateFlow<VideoLibraryUiState> = _uiState.asStateFlow()

    /** Multi-select state, shared implementation with the audio library. */
    private val selection = SelectionHolder()
    val selected: StateFlow<Set<Long>> = selection.selected

    /** One-shot message for the trash/delete outcome; consumed by the screen. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() { _message.value = null }

    /** True when the platform shows its own confirmation, so we shouldn't. */
    val systemConfirmsDelete: Boolean get() = mediaDeleter.systemConfirms()

    /** True when removal is recoverable (system trash), driving the wording. */
    val deleteIsRecoverable: Boolean get() = mediaDeleter.isRecoverable()

    private var pendingDeleteCount = 0
    // Non-null while a "move to private" awaits consent to remove the public originals.
    private var pendingPrivateIds: List<Long> = emptyList()

    /**
     * Copies the selection into the private vault, then removes the public originals via
     * the same consent path as delete. Hiding also scrubs each file's resume position so
     * a private item leaves no breadcrumb in Continue Watching or History.
     */
    fun moveToPrivate(
        visible: List<VideoFile>,
        onNeedsConsent: (android.content.IntentSender) -> Unit,
    ) {
        val videos = selectedVideos(visible)
        if (videos.isEmpty()) return
        viewModelScope.launch {
            val copied = privateVault.hide(videos)
            if (copied.isEmpty()) {
                _message.value = "Couldn't move to private"
                clearSelection()
                return@launch
            }
            pendingPrivateIds = videos
                .filter { it.uri in copied }
                .map { it.mediaStoreId }
            when (val result = mediaDeleter.delete(copied)) {
                is DeleteResult.NeedsConsent -> onNeedsConsent(result.intentSender)
                is DeleteResult.Deleted -> finishPrivate()
                is DeleteResult.Failed -> {
                    _message.value = result.message
                    clearSelection()
                }
            }
        }
    }

    private fun finishPrivate() {
        val ids = pendingPrivateIds
        pendingPrivateIds = emptyList()
        viewModelScope.launch {
            ids.forEach { playbackRepository.clearPosition(it) }
            loadContent()
            clearSelection()
            _message.value = if (ids.size == 1) "Moved to private" else "${ids.size} moved to private"
        }
    }

    fun toggleSelection(video: VideoFile) = selection.toggle(video.mediaStoreId)

    fun selectAll(videos: List<VideoFile>) =
        selection.selectAll(videos.map { it.mediaStoreId })

    fun clearSelection() = selection.clear()

    fun selectedVideos(visible: List<VideoFile>): List<VideoFile> =
        selection.filter(visible) { it.mediaStoreId }

    /**
     * Removes the current selection. On API 30+ this trashes (recoverable); below that it
     * deletes permanently, which is why the screen confirms first on those versions.
     */
    fun deleteSelection(
        visible: List<VideoFile>,
        onNeedsConsent: (android.content.IntentSender) -> Unit,
    ) {
        val videos = selectedVideos(visible)
        if (videos.isEmpty()) return
        pendingDeleteCount = videos.size
        viewModelScope.launch {
            when (val result = mediaDeleter.delete(videos.map { it.uri })) {
                is DeleteResult.NeedsConsent -> onNeedsConsent(result.intentSender)
                is DeleteResult.Deleted -> finishDelete()
                is DeleteResult.Failed -> {
                    _message.value = result.message
                    clearSelection()
                }
            }
        }
    }

    fun onDeleteConsentResult(granted: Boolean) {
        val privateMove = pendingPrivateIds.isNotEmpty()
        if (!granted) {
            // The vault copy already exists; a refusal just leaves the public copy too.
            _message.value = if (privateMove) "Copied to private, original kept" else "Cancelled"
            clearSelection()
            pendingDeleteCount = 0
            pendingPrivateIds = emptyList()
            return
        }
        if (privateMove) finishPrivate() else finishDelete()
    }

    /**
     * Re-queries MediaStore instead of trusting what was requested: the user can deselect
     * items inside the system dialog, so the request is not the outcome.
     */
    private fun finishDelete() {
        pendingDeleteCount = 0
        viewModelScope.launch {
            val before = rawVideos.size
            loadContent()
            val after = mediaRepository.queryAllVideos().size
            val gone = (before - after).coerceAtLeast(0)
            clearSelection()
            val verb = if (mediaDeleter.isRecoverable()) "moved to trash" else "deleted"
            _message.value = when {
                gone == 0 -> "Nothing was removed"
                gone == 1 -> "1 video $verb"
                else -> "$gone videos $verb"
            }
        }
    }

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
        val next = if (_uiState.value.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
        viewModelScope.launch { settingsRepository.setViewMode(next.name) }
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
