package com.devbytes.vixplayer.app.ui.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.AudioRepository
import com.devbytes.vixplayer.app.data.repository.AudioTrack
import com.devbytes.vixplayer.app.data.db.entity.Playlist
import com.devbytes.vixplayer.app.data.repository.DeleteResult
import com.devbytes.vixplayer.app.data.repository.MediaDeleter
import com.devbytes.vixplayer.app.data.repository.PlaylistRepository
import com.devbytes.vixplayer.app.player.PlayerController
import com.devbytes.vixplayer.app.player.QueueItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Library groupings. Playlists joined the set once it had a real data model — it was
 * omitted while a tab could only ever have been empty.
 */
enum class AudioTab(val label: String) {
    TRACKS("Tracks"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FOLDERS("Folders"),
    PLAYLISTS("Playlists"),
}

/** One row in a grouping list (an album, artist, or folder). */
data class AudioGroup(
    val name: String,
    val trackCount: Int,
    val artUri: String,
    val tracks: List<AudioTrack>,
)

sealed interface AudioLibraryUiState {
    data object Loading : AudioLibraryUiState
    data object Empty : AudioLibraryUiState
    data class Loaded(val tracks: List<AudioTrack>) : AudioLibraryUiState
}

@HiltViewModel
class AudioLibraryViewModel @Inject constructor(
    private val audioRepository: AudioRepository,
    private val playerController: PlayerController,
    private val playlistRepository: PlaylistRepository,
    private val mediaDeleter: MediaDeleter,
) : ViewModel() {

    /** One-shot message for the delete outcome; consumed by the screen. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() { _message.value = null }

    /** True when the platform shows its own delete confirmation, so we shouldn't. */
    val systemConfirmsDelete: Boolean get() = mediaDeleter.systemConfirms()

    /**
     * True when removal goes to the system trash and can be undone. Drives the wording,
     * so the UI promises exactly what the device will actually do.
     */
    val deleteIsRecoverable: Boolean get() = mediaDeleter.isRecoverable()

    /** URIs pending deletion, kept so the queue can be reconciled after consent. */
    private var pendingDeleteUris: List<String> = emptyList()

    /** Playlists, for the tab and for the add-to-playlist picker. */
    val playlists: StateFlow<List<Playlist>> = playlistRepository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addToPlaylist(playlistId: Long, track: AudioTrack) {
        viewModelScope.launch { playlistRepository.addTrack(playlistId, track) }
    }

    fun createPlaylistWith(name: String, track: AudioTrack) {
        viewModelScope.launch {
            val id = playlistRepository.create(name.trim())
            playlistRepository.addTrack(id, track)
        }
    }

    private val _state = MutableStateFlow<AudioLibraryUiState>(AudioLibraryUiState.Loading)
    val state: StateFlow<AudioLibraryUiState> = _state.asStateFlow()

    private val _tab = MutableStateFlow(AudioTab.TRACKS)
    val tab: StateFlow<AudioTab> = _tab.asStateFlow()

    /**
     * Selected track ids. Long-press was deliberately reserved for this during the
     * playlists pass, which is why add-to-playlist went on a row overflow instead.
     */
    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected.asStateFlow()

    /** Non-null while drilled into one group; mirrors FolderBrowserScreen's in-place drill. */
    private val _openGroup = MutableStateFlow<AudioGroup?>(null)
    val openGroup: StateFlow<AudioGroup?> = _openGroup.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = AudioLibraryUiState.Loading
            val tracks = audioRepository.queryAllTracks()
            _state.value =
                if (tracks.isEmpty()) AudioLibraryUiState.Empty
                else AudioLibraryUiState.Loaded(tracks)
        }
    }

    fun selectTab(tab: AudioTab) {
        _tab.value = tab
        // Leaving a tab abandons its drill; returning should start at the grouping list.
        _openGroup.value = null
        clearSelection()
    }

    fun toggleSelection(track: AudioTrack) {
        val current = _selected.value
        _selected.value =
            if (track.mediaStoreId in current) current - track.mediaStoreId
            else current + track.mediaStoreId
    }

    fun selectAll(tracks: List<AudioTrack>) {
        _selected.value = tracks.map { it.mediaStoreId }.toSet()
    }

    fun clearSelection() {
        if (_selected.value.isNotEmpty()) _selected.value = emptySet()
    }

    /** Returns the selected tracks in the order they appear in [visible]. */
    fun selectedTracks(visible: List<AudioTrack>): List<AudioTrack> =
        visible.filter { it.mediaStoreId in _selected.value }

    /** Appends the selection to the queue without interrupting what is playing. */
    fun enqueueSelection(visible: List<AudioTrack>) {
        val tracks = selectedTracks(visible)
        if (tracks.isEmpty()) return
        playerController.enqueue(tracks.map { it.toQueueItem() })
        clearSelection()
    }

    fun addSelectionToPlaylist(playlistId: Long, visible: List<AudioTrack>) {
        val tracks = selectedTracks(visible)
        viewModelScope.launch {
            tracks.forEach { playlistRepository.addTrack(playlistId, it) }
            clearSelection()
        }
    }

    fun createPlaylistWithSelection(name: String, visible: List<AudioTrack>) {
        val tracks = selectedTracks(visible)
        viewModelScope.launch {
            val id = playlistRepository.create(name.trim())
            tracks.forEach { playlistRepository.addTrack(id, it) }
            clearSelection()
        }
    }

    fun openGroup(group: AudioGroup) {
        _openGroup.value = group
        clearSelection()
    }

    /** Returns true if a drill was popped, so the caller can swallow the back press. */
    fun closeGroup(): Boolean {
        if (_openGroup.value == null) return false
        _openGroup.value = null
        clearSelection()
        return true
    }

    /**
     * Groups for [tab], derived in memory from the single track query rather than from
     * MediaStore's Albums/Artists tables — those don't know about the `IS_MUSIC` filter,
     * so their track counts could disagree with the tracks actually listed.
     */
    fun groupsFor(tab: AudioTab, tracks: List<AudioTrack>): List<AudioGroup> {
        val keyed = when (tab) {
            AudioTab.ALBUMS -> tracks.groupBy { it.album.ifBlank { "Unknown album" } }
            AudioTab.ARTISTS -> tracks.groupBy { it.artist }
            AudioTab.FOLDERS -> tracks.groupBy { it.folder }
            AudioTab.TRACKS, AudioTab.PLAYLISTS -> emptyMap()
        }
        return keyed.map { (name, groupTracks) ->
            AudioGroup(
                name = name,
                trackCount = groupTracks.size,
                artUri = groupTracks.first().albumArtUri.toString(),
                tracks = groupTracks,
            )
        }.sortedBy { it.name.lowercase() }
    }

    /**
     * Deletes the current selection. Returns an IntentSender when the platform wants to
     * confirm; the caller launches it and calls [onDeleteConsentResult] with the outcome.
     */
    fun deleteSelection(
        visible: List<AudioTrack>,
        onNeedsConsent: (android.content.IntentSender) -> Unit,
    ) {
        val tracks = selectedTracks(visible)
        if (tracks.isEmpty()) return
        pendingDeleteUris = tracks.map { it.uri.toString() }
        viewModelScope.launch {
            when (val result = mediaDeleter.delete(tracks.map { it.uri })) {
                is DeleteResult.NeedsConsent -> onNeedsConsent(result.intentSender)
                is DeleteResult.Deleted -> finishDelete(result.count)
                is DeleteResult.Failed -> {
                    _message.value = result.message
                    clearSelection()
                }
            }
        }
    }

    /** Called once the system dialog closes, whichever way the user answered. */
    fun onDeleteConsentResult(granted: Boolean) {
        if (!granted) {
            _message.value = "Cancelled"
            clearSelection()
            pendingDeleteUris = emptyList()
            return
        }
        finishDelete(pendingDeleteUris.size)
    }

    /**
     * Re-queries MediaStore rather than trusting what we asked to delete: the user can
     * deselect items in the system dialog, so the filesystem is the only honest source
     * for what actually survived.
     */
    private fun finishDelete(requested: Int) {
        val removed = pendingDeleteUris.toSet()
        pendingDeleteUris = emptyList()
        viewModelScope.launch {
            val before = (_state.value as? AudioLibraryUiState.Loaded)?.tracks?.size ?: 0
            val tracks = audioRepository.queryAllTracks()
            _state.value =
                if (tracks.isEmpty()) AudioLibraryUiState.Empty
                else AudioLibraryUiState.Loaded(tracks)

            val actuallyGone = (before - tracks.size).coerceAtLeast(0)
            playerController.removeFromQueue(removed)
            clearSelection()
            _openGroup.value = null
            val verb = if (mediaDeleter.isRecoverable()) "moved to trash" else "deleted"
            _message.value = when {
                actuallyGone == 0 -> "Nothing was removed"
                actuallyGone == 1 -> "1 file $verb"
                else -> "$actuallyGone files $verb"
            }
        }
    }

    /**
     * Plays [track] with [context] as the queue — the PRD's "adds context queue". The
     * queue is whatever list is on screen, so playing from an album queues that album
     * rather than the entire library.
     */
    fun play(track: AudioTrack, context: List<AudioTrack>) {
        val index = context.indexOfFirst { it.mediaStoreId == track.mediaStoreId }
        if (index < 0) return
        playerController.prepareQueue(context.map { it.toQueueItem() }, index)
    }
}

/** Carries the display metadata onto the MediaItem, for the session and both UIs. */
internal fun AudioTrack.toQueueItem() = QueueItem(
    uri = uri.toString(),
    title = title,
    artist = artist,
    album = album,
    artworkUri = albumArtUri,
)
