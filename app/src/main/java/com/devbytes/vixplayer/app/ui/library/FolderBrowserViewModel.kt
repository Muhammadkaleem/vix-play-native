package com.devbytes.vixplayer.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.FolderEntry
import com.devbytes.vixplayer.app.data.repository.DeleteResult
import com.devbytes.vixplayer.app.data.repository.MediaDeleter
import com.devbytes.vixplayer.app.data.repository.MediaRenamer
import com.devbytes.vixplayer.app.data.repository.MediaTransfer
import com.devbytes.vixplayer.app.data.repository.TransferProgress
import com.devbytes.vixplayer.app.data.repository.TransferResult
import com.devbytes.vixplayer.app.data.repository.MediaRepository
import com.devbytes.vixplayer.app.data.repository.RenameResult
import com.devbytes.vixplayer.app.ui.common.SelectionHolder
import com.devbytes.vixplayer.app.data.repository.VideoFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FolderBrowserUiState {
    data object Loading : FolderBrowserUiState
    data class FolderList(val folders: List<FolderEntry>) : FolderBrowserUiState
    data class VideoList(val folderName: String, val videos: List<VideoFile>) : FolderBrowserUiState
    data class Error(val message: String) : FolderBrowserUiState
}

@HiltViewModel
class FolderBrowserViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val mediaDeleter: MediaDeleter,
    private val mediaRenamer: MediaRenamer,
    private val mediaTransfer: MediaTransfer,
) : ViewModel() {

    private val _state = MutableStateFlow<FolderBrowserUiState>(FolderBrowserUiState.Loading)
    val state: StateFlow<FolderBrowserUiState> = _state.asStateFlow()

    /** Shared with both libraries; see [SelectionHolder]. */
    private val selection = SelectionHolder()
    val selected: StateFlow<Set<Long>> = selection.selected

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() { _message.value = null }

    val systemConfirmsDelete: Boolean get() = mediaDeleter.systemConfirms()
    val deleteIsRecoverable: Boolean get() = mediaDeleter.isRecoverable()

    /** True while a move is awaiting consent to remove the originals. */
    private var movePending = false

    /** The rename awaiting system consent, if any. */
    private var pendingRename: Triple<android.net.Uri, String, String>? = null

    /** Destination choices for move/copy: folders that already hold videos. */
    private val _destinations = MutableStateFlow<List<FolderEntry>>(emptyList())
    val destinations: StateFlow<List<FolderEntry>> = _destinations.asStateFlow()

    /** Non-null while a transfer is running; drives the modal progress dialog. */
    private val _transfer = MutableStateFlow<TransferProgress?>(null)
    val transfer: StateFlow<TransferProgress?> = _transfer.asStateFlow()

    private var transferJob: Job? = null

    init { loadFolders() }

    // ── Selection ────────────────────────────────────────────────────────────

    fun toggleSelection(video: VideoFile) = selection.toggle(video.mediaStoreId)

    fun selectAll(videos: List<VideoFile>) =
        selection.selectAll(videos.map { it.mediaStoreId })

    fun clearSelection() = selection.clear()

    fun selectedVideos(visible: List<VideoFile>): List<VideoFile> =
        selection.filter(visible) { it.mediaStoreId }

    private fun visibleVideos(): List<VideoFile> =
        (_state.value as? FolderBrowserUiState.VideoList)?.videos.orEmpty()

    // ── Remove (trash on API 30+, permanent below) ───────────────────────────

    fun deleteSelection(onNeedsConsent: (android.content.IntentSender) -> Unit) {
        val videos = selectedVideos(visibleVideos())
        if (videos.isEmpty()) return
        viewModelScope.launch {
            when (val result = mediaDeleter.delete(videos.map { it.uri })) {
                is DeleteResult.NeedsConsent -> onNeedsConsent(result.intentSender)
                is DeleteResult.Deleted -> finishRemoval()
                is DeleteResult.Failed -> {
                    _message.value = result.message
                    clearSelection()
                }
            }
        }
    }

    fun onDeleteConsentResult(granted: Boolean) {
        val wasMove = movePending
        movePending = false
        if (!granted) {
            // The copies are already made; only the originals survive the refusal.
            _message.value = if (wasMove) "Copied, originals kept" else "Cancelled"
            clearSelection()
            viewModelScope.launch { reloadCurrentFolder() }
            return
        }
        if (wasMove) {
            viewModelScope.launch {
                reloadCurrentFolder()
                clearSelection()
                _message.value = "Moved"
            }
            return
        }
        finishRemoval()
    }

    /** Re-reads the folder rather than trusting the request; the user can deselect
     *  inside the system dialog, so the request is not the outcome. */
    private fun finishRemoval() {
        val before = visibleVideos().size
        viewModelScope.launch {
            reloadCurrentFolder()
            val after = visibleVideos().size
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

    // ── Move / copy ──────────────────────────────────────────────────────────

    /** Loads pickable destinations, excluding the folder already being viewed. */
    fun loadDestinations() {
        viewModelScope.launch {
            val here = currentBucketId
            _destinations.value = runCatching { mediaRepository.queryFolders() }
                .getOrDefault(emptyList())
                .filter { it.bucketId != here }
        }
    }

    /**
     * Copies (or moves) the selection into [destination].
     *
     * Move is copy-then-delete inside [MediaTransfer]: the source goes only once its copy
     * is published, so an interruption leaves a duplicate rather than nothing.
     */
    fun transferSelection(
        destination: FolderEntry,
        deleteSource: Boolean,
        onNeedsConsent: (android.content.IntentSender) -> Unit,
    ) {
        val videos = selectedVideos(visibleVideos())
        if (videos.isEmpty()) return

        transferJob = viewModelScope.launch {
            // Resolve the destination directory from a file that already lives there;
            // FolderEntry carries no path of its own.
            val dir = runCatching {
                mediaRepository.queryVideosInFolder(destination.bucketId)
                    .firstOrNull()?.path?.substringBeforeLast('/')
            }.getOrNull()

            if (dir.isNullOrBlank()) {
                _message.value = "Couldn't resolve that folder"
                clearSelection()
                return@launch
            }

            _transfer.value = TransferProgress(0, videos.size, "", 0f)
            val result = mediaTransfer.transfer(videos, dir, deleteSource) { progress ->
                _transfer.value = progress
            }
            _transfer.value = null

            val sources = (result as? TransferResult.Completed)?.copiedSources.orEmpty()
            if (deleteSource && sources.isNotEmpty()) {
                // One consent prompt for the whole batch, reusing the delete path that
                // scoped storage actually permits.
                movePending = true
                when (val removal = mediaDeleter.delete(sources)) {
                    is DeleteResult.NeedsConsent -> {
                        onNeedsConsent(removal.intentSender)
                        return@launch
                    }
                    else -> Unit
                }
            }

            reloadCurrentFolder()
            clearSelection()
            _message.value = describe(result, deleteSource)
        }
    }

    fun cancelTransfer() {
        transferJob?.cancel()
        transferJob = null
        _transfer.value = null
    }

    private fun describe(result: TransferResult, moved: Boolean): String {
        val verb = if (moved) "moved" else "copied"
        return when (result) {
            is TransferResult.Completed -> when {
                result.succeeded == 0 -> "Nothing was $verb"
                result.failed > 0 -> "${result.succeeded} $verb, ${result.failed} failed"
                result.succeeded == 1 -> "1 video $verb"
                else -> "${result.succeeded} videos $verb"
            }
            is TransferResult.Cancelled ->
                if (result.succeeded == 0) "Cancelled" else "Cancelled after ${result.succeeded}"
            is TransferResult.Failed -> result.message
        }
    }

    // ── Rename (single selection only) ───────────────────────────────────────

    fun renameSelected(newName: String, onNeedsConsent: (android.content.IntentSender) -> Unit) {
        val video = selectedVideos(visibleVideos()).singleOrNull() ?: return
        pendingRename = Triple(video.uri, video.name, newName)
        viewModelScope.launch {
            when (val result = mediaRenamer.rename(video.uri, video.name, newName)) {
                is RenameResult.NeedsConsent -> onNeedsConsent(result.intentSender)
                is RenameResult.Renamed -> finishRename(true)
                is RenameResult.Failed -> {
                    _message.value = result.message
                    pendingRename = null
                    clearSelection()
                }
            }
        }
    }

    fun onRenameConsentResult(granted: Boolean) {
        val pending = pendingRename
        if (!granted || pending == null) {
            _message.value = "Cancelled"
            pendingRename = null
            clearSelection()
            return
        }
        viewModelScope.launch {
            val result = mediaRenamer.applyAfterConsent(pending.first, pending.second, pending.third)
            pendingRename = null
            finishRename(result is RenameResult.Renamed)
        }
    }

    private fun finishRename(success: Boolean) {
        viewModelScope.launch {
            reloadCurrentFolder()
            clearSelection()
            _message.value = if (success) "Renamed" else "Couldn't rename"
        }
    }

    /** Re-queries the folder currently open, leaving the folder list alone. */
    private suspend fun reloadCurrentFolder() {
        val current = _state.value as? FolderBrowserUiState.VideoList ?: return
        val bucketId = currentBucketId ?: return
        runCatching { mediaRepository.queryVideosInFolder(bucketId) }
            .onSuccess {
                _state.value = FolderBrowserUiState.VideoList(current.folderName, it)
            }
    }

    private var currentBucketId: Long? = null

    fun enterFolder(bucketId: Long, folderName: String) {
        currentBucketId = bucketId
        clearSelection()
        viewModelScope.launch {
            _state.value = FolderBrowserUiState.Loading
            runCatching { mediaRepository.queryVideosInFolder(bucketId) }
                .onSuccess { videos ->
                    _state.value = FolderBrowserUiState.VideoList(folderName, videos)
                }
                .onFailure { e ->
                    _state.value = FolderBrowserUiState.Error(e.message ?: "Unknown error")
                }
        }
    }

    fun navigateUp() {
        currentBucketId = null
        clearSelection()
        loadFolders()
    }

    private fun loadFolders() {
        viewModelScope.launch {
            _state.value = FolderBrowserUiState.Loading
            runCatching { mediaRepository.queryFolders() }
                .onSuccess { folders ->
                    _state.value = FolderBrowserUiState.FolderList(folders)
                }
                .onFailure { e ->
                    _state.value = FolderBrowserUiState.Error(e.message ?: "Unknown error")
                }
        }
    }
}
