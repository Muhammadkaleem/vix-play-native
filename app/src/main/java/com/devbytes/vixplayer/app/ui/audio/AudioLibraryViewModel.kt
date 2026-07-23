package com.devbytes.vixplayer.app.ui.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.AudioRepository
import com.devbytes.vixplayer.app.data.repository.AudioTrack
import com.devbytes.vixplayer.app.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AudioLibraryUiState {
    data object Loading : AudioLibraryUiState
    data object Empty : AudioLibraryUiState
    data class Tracks(val tracks: List<AudioTrack>) : AudioLibraryUiState
}

@HiltViewModel
class AudioLibraryViewModel @Inject constructor(
    private val audioRepository: AudioRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val _state = MutableStateFlow<AudioLibraryUiState>(AudioLibraryUiState.Loading)
    val state: StateFlow<AudioLibraryUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = AudioLibraryUiState.Loading
            val tracks = audioRepository.queryAllTracks()
            _state.value =
                if (tracks.isEmpty()) AudioLibraryUiState.Empty
                else AudioLibraryUiState.Tracks(tracks)
        }
    }

    /**
     * Plays [track], queueing the whole visible list from that point so next/prev and
     * shuffle have context — the PRD's "tap track → Audio Player (adds context queue)".
     */
    fun play(track: AudioTrack) {
        val tracks = (_state.value as? AudioLibraryUiState.Tracks)?.tracks ?: return
        val index = tracks.indexOfFirst { it.mediaStoreId == track.mediaStoreId }
        if (index < 0) return
        playerController.prepareQueue(tracks.map { it.uri.toString() }, index)
    }
}
