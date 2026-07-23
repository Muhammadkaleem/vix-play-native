package com.devbytes.vixplayer.app.ui.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.db.entity.Playlist
import com.devbytes.vixplayer.app.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val repository: PlaylistRepository,
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = repository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _counts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val counts: StateFlow<Map<Long, Int>> = _counts.asStateFlow()

    init {
        viewModelScope.launch {
            playlists.collect { list ->
                _counts.value = list.associate { it.id to repository.itemCount(it.id) }
            }
        }
    }

    fun create(name: String) {
        viewModelScope.launch { repository.create(name.trim()) }
    }

    fun rename(playlist: Playlist, name: String) {
        viewModelScope.launch { repository.rename(playlist, name.trim()) }
    }

    fun delete(playlist: Playlist) {
        viewModelScope.launch { repository.delete(playlist) }
    }
}
