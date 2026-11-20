package com.devbytes.vixplayer.app.ui.library

import android.os.Build
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
import com.devbytes.vixplayer.app.data.series.NaturalOrder
import com.devbytes.vixplayer.app.data.repository.PlaybackRepository
import com.devbytes.vixplayer.app.data.repository.SettingsRepository
import com.devbytes.vixplayer.app.data.repository.RenameResult
import com.devbytes.vixplayer.app.ui.common.SelectionHolder
import com.devbytes.vixplayer.app.data.repository.VideoFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FolderSort(val label: String) {
    NAME("Name"),
    RECENT("Recent"),
    COUNT("Item count"),
}

sealed interface FolderBrowserUiState {
    data object Loading : FolderBrowserUiState
    data class FolderList(val folders: List<FolderEntry>) : FolderBrowserUiState
    data class VideoList(val folderName: String, val videos: List<VideoFile>) : FolderBrowserUiState
    data class Error(val message: String) : FolderBrowserUiState
}

/** The floating resume button's current target (null ⇒ no button). */
data class ResumeTarget(val mediaStoreId: Long, val uri: String, val name: String)

@HiltViewModel
class FolderBrowserViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val mediaDeleter: MediaDeleter,
    private val mediaRenamer: MediaRenamer,
    private val mediaTransfer: MediaTransfer,
    private val playbackRepository: PlaybackRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<FolderBrowserUiState>(FolderBrowserUiState.Loading)
    val state: StateFlow<FolderBrowserUiState> = _state.asStateFlow()

    /** Grid vs list — shared, persisted, mirrors the video library. */
    val viewMode: StateFlow<ViewMode> = settingsRepository.viewMode
        .map { runCatching { ViewMode.valueOf(it) }.getOrDefault(ViewMode.GRID) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ViewMode.GRID)

    private val _folderSort = MutableStateFlow(FolderSort.NAME)
    val folderSort: StateFlow<FolderSort> = _folderSort.asStateFlow()

    /** Resume fraction per video in the open folder, keyed by mediaStoreId. */
    private val _resume = MutableStateFlow<Map<Long, Float>>(emptyMap())
    val resume: StateFlow<Map<Long, Float>> = _resume.asStateFlow()

    // ── Floating resume button ────────────────────────────────────────────────
    /** The most-recent eligible video for the current scope (global on the folder list,
     *  this-folder-only inside a folder). Null hides the button. */
    private val _resumeTarget = MutableStateFlow<ResumeTarget?>(null)
    val resumeTarget: StateFlow<ResumeTarget?> = _resumeTarget.asStateFlow()

    /** One-shot: a validated URI to open in the player (consumed by the screen). */
    private val _openPlayerUri = MutableStateFlow<String?>(null)
    val openPlayerUri: StateFlow<String?> = _openPlayerUri.asStateFlow()

    /** Ids found unavailable at tap-time — suppressed as candidates (not deleted from
     *  history), cleared on an explicit media/permission refresh. */
    private val suppressedIds = mutableSetOf<Long>()

    /** Unsorted folder list, re-sorted in memory when [folderSort] changes. */
    private var rawFolders: List<FolderEntry> = emptyList()

    init {
        viewModelScope.launch {
            settingsRepository.folderSort.collect { name ->
                _folderSort.value = runCatching { FolderSort.valueOf(name) }
                    .getOrDefault(FolderSort.NAME)
                if (_state.value is FolderBrowserUiState.FolderList) {
                    _state.value = FolderBrowserUiState.FolderList(sortFolders(rawFolders))
                }
            }
        }
    }

    fun toggleViewMode() {
        val next = if (viewMode.value == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
        viewModelScope.launch { settingsRepository.setViewMode(next.name) }
    }

    fun setFolderSort(sort: FolderSort) {
        viewModelScope.launch { settingsRepository.setFolderSort(sort.name) }
    }

    private fun sortFolders(folders: List<FolderEntry>): List<FolderEntry> = when (_folderSort.value) {
        FolderSort.NAME -> folders.sortedWith(compareBy(NaturalOrder) { it.name })
        FolderSort.RECENT -> folders.sortedByDescending { it.dateModified }
        FolderSort.COUNT -> folders.sortedByDescending { it.videoCount }
    }

    /** Fetches resume fractions for the videos now on screen. */
    private suspend fun refreshResume(videos: List<VideoFile>) {
        val recent = runCatching { playbackRepository.getRecentPositions(200) }.getOrDefault(emptyList())
        val byId = recent.associateBy { it.mediaStoreId }
        _resume.value = videos.mapNotNull { v ->
            val pos = byId[v.mediaStoreId] ?: return@mapNotNull null
            if (v.durationMs <= 0) return@mapNotNull null
            v.mediaStoreId to (pos.positionMs.toFloat() / v.durationMs).coerceIn(0f, 1f)
        }.toMap()
    }

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

    /** Non-null while a same-volume fast move waits for its write-consent result:
     *  (source uris, destination dir). Set before prompting, applied on consent. */
    private var pendingFastMove: Pair<List<android.net.Uri>, String>? = null

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
        // Same-volume fast move: consent granted the writes, so apply the RELATIVE_PATH
        // updates now (a granted delete-request would have been performed by the OS itself).
        pendingFastMove?.let { (uris, dir) ->
            pendingFastMove = null
            viewModelScope.launch {
                val result = if (granted) {
                    mediaTransfer.applyMove(uris, dir)
                } else {
                    TransferResult.Cancelled(0)
                }
                reloadCurrentFolder()
                clearSelection()
                _message.value = describe(result, moved = true)
            }
            return
        }

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
    ) = runTransfer(deleteSource, onNeedsConsent) {
        // Resolve the destination directory from a file that already lives there;
        // FolderEntry carries no path of its own.
        runCatching {
            mediaRepository.queryVideosInFolder(destination.bucketId)
                .firstOrNull()?.path?.substringBeforeLast('/')
        }.getOrNull()
    }

    /**
     * Copies (or moves) the selection into a **new** folder [name] under the primary volume's
     * Movies directory. MediaStore can't create an empty dir, so the folder materialises when
     * the files land there (RELATIVE_PATH insert/update). Same volume, so a move gets the
     * in-place fast path for free. [name] is assumed already validated by the caller.
     */
    fun transferToNewFolder(
        name: String,
        deleteSource: Boolean,
        onNeedsConsent: (android.content.IntentSender) -> Unit,
    ) = runTransfer(deleteSource, onNeedsConsent) {
        "/storage/emulated/0/Movies/${name.trim().trimEnd('.', ' ')}"
    }

    /** Shared move/copy core, keyed on a lazily-resolved destination directory. */
    private fun runTransfer(
        deleteSource: Boolean,
        onNeedsConsent: (android.content.IntentSender) -> Unit,
        dirResolver: suspend () -> String?,
    ) {
        val videos = selectedVideos(visibleVideos())
        if (videos.isEmpty()) return

        transferJob = viewModelScope.launch {
            val dir = dirResolver()

            if (dir.isNullOrBlank()) {
                _message.value = "Couldn't resolve that folder"
                clearSelection()
                return@launch
            }

            // Same-volume fast path (API 30+): move in place via a RELATIVE_PATH update — no
            // byte copy — behind one batch write-consent. Decided before prompting so we never
            // show the wrong dialog. Anything else falls through to copy-then-delete below.
            if (deleteSource &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                sameVolume(videos, dir)
            ) {
                pendingFastMove = videos.map { it.uri } to dir
                onNeedsConsent(mediaTransfer.writeRequest(videos.map { it.uri }))
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

    /** True only when every source and the destination sit on the same storage volume —
     *  the precondition for an in-place RELATIVE_PATH move. */
    private fun sameVolume(videos: List<VideoFile>, destDir: String): Boolean {
        val destVol = volumeRoot(destDir) ?: return false
        return videos.all { volumeRoot(it.path) == destVol }
    }

    /** Extracts the volume root of an absolute media path:
     *  `/storage/emulated/0/…` → `/storage/emulated/0`; `/storage/1AF3-2B0C/…` → `/storage/1AF3-2B0C`. */
    private fun volumeRoot(path: String): String? {
        val parts = path.split('/').filter { it.isNotEmpty() }
        return when {
            parts.size >= 3 && parts[0] == "storage" && parts[1] == "emulated" ->
                "/storage/emulated/${parts[2]}"
            parts.size >= 2 && parts[0] == "storage" -> "/storage/${parts[1]}"
            else -> null
        }
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
                refreshResume(it)
                computeResumeTarget()
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
                    refreshResume(videos)
                    computeResumeTarget()
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

    /** Re-scan folders — used after a media-permission grant widens what MediaStore returns.
     *  Also clears tap-time suppression so recovered files can resurface. */
    fun refreshFolders() {
        suppressedIds.clear()
        loadFolders()
    }

    private fun loadFolders() {
        viewModelScope.launch {
            _state.value = FolderBrowserUiState.Loading
            runCatching { mediaRepository.queryFolders() }
                .onSuccess { folders ->
                    rawFolders = folders
                    _state.value = FolderBrowserUiState.FolderList(sortFolders(folders))
                    computeResumeTarget()
                }
                .onFailure { e ->
                    _state.value = FolderBrowserUiState.Error(e.message ?: "Unknown error")
                }
        }
    }

    // ── Floating resume button logic ──────────────────────────────────────────

    /** Recompute the resume target for the current scope from cached history (global on the
     *  folder list; this-folder-only inside a folder). Skips deleted / suppressed candidates. */
    private suspend fun computeResumeTarget() {
        val recents = runCatching { playbackRepository.getRecentPositions(200) }
            .getOrDefault(emptyList())
            .filter { it.mediaStoreId !in suppressedIds }
        _resumeTarget.value = when (val s = _state.value) {
            // Global: newest-played video that still resolves anywhere. A plain loop (not a
            // sequence) so the suspend queryVideoById is legal.
            is FolderBrowserUiState.FolderList -> {
                var found: ResumeTarget? = null
                for (pos in recents) {
                    val video = runCatching { mediaRepository.queryVideoById(pos.mediaStoreId) }.getOrNull()
                    if (video != null) { found = video.toResumeTarget(); break }
                }
                found
            }
            // Folder-scoped: newest-played among exactly this folder's videos (never falls
            // back to another folder). Membership = the loaded list, so no query needed.
            is FolderBrowserUiState.VideoList -> {
                val byId = s.videos.associateBy { it.mediaStoreId }
                recents.firstNotNullOfOrNull { byId[it.mediaStoreId] }?.toResumeTarget()
            }
            else -> null
        }
    }

    private fun VideoFile.toResumeTarget() = ResumeTarget(mediaStoreId, uri.toString(), name)

    /** Called when the browser returns to the foreground (e.g. back from the player) — a new
     *  last-played may have appeared. */
    fun onScreenResumed() {
        viewModelScope.launch { computeResumeTarget() }
    }

    /** FAB tap: re-validate the target at tap time, then open it — or, if it vanished, report
     *  it, suppress it and retarget (never silently play a different video). */
    fun onResumeClicked() {
        if (_openPlayerUri.value != null) return // a launch is already pending
        val target = _resumeTarget.value ?: return
        viewModelScope.launch {
            val video = runCatching { mediaRepository.queryVideoById(target.mediaStoreId) }.getOrNull()
            val inScope = when (val s = _state.value) {
                is FolderBrowserUiState.VideoList -> s.videos.any { it.mediaStoreId == target.mediaStoreId }
                is FolderBrowserUiState.FolderList -> video != null
                else -> false
            }
            if (video != null && inScope) {
                _openPlayerUri.value = video.uri.toString()
            } else {
                suppressedIds += target.mediaStoreId
                _message.value = "This video is no longer available"
                computeResumeTarget()
            }
        }
    }

    fun consumeOpenPlayer() { _openPlayerUri.value = null }
}
