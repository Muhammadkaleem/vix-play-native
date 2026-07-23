package com.devbytes.vixplayer.app.ui.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.AudioRepository
import com.devbytes.vixplayer.app.data.repository.AudioTrack
import com.devbytes.vixplayer.app.player.PlayerController
import com.devbytes.vixplayer.app.player.QueueItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Library groupings. Playlists is deliberately **not** here: it is P1 in the PRD with its
 * own `/playlists` route, needs a Room data model that doesn't exist, and is Step 7 in the
 * roadmap — a selectable tab that can never hold anything would be dead UI.
 */
enum class AudioTab(val label: String) {
    TRACKS("Tracks"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FOLDERS("Folders"),
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
) : ViewModel() {

    private val _state = MutableStateFlow<AudioLibraryUiState>(AudioLibraryUiState.Loading)
    val state: StateFlow<AudioLibraryUiState> = _state.asStateFlow()

    private val _tab = MutableStateFlow(AudioTab.TRACKS)
    val tab: StateFlow<AudioTab> = _tab.asStateFlow()

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
    }

    fun openGroup(group: AudioGroup) {
        _openGroup.value = group
    }

    /** Returns true if a drill was popped, so the caller can swallow the back press. */
    fun closeGroup(): Boolean {
        if (_openGroup.value == null) return false
        _openGroup.value = null
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
            AudioTab.TRACKS -> emptyMap()
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
