package com.devbytes.vixplayer.app.ui.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.db.entity.PlaylistItem
import com.devbytes.vixplayer.app.data.repository.PlaylistRepository
import com.devbytes.vixplayer.app.data.repository.PlaylistRow
import com.devbytes.vixplayer.app.player.PlayerController
import com.devbytes.vixplayer.app.player.QueueItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val _rows = MutableStateFlow<List<PlaylistRow>>(emptyList())
    val rows: StateFlow<List<PlaylistRow>> = _rows.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private var playlistId: Long = -1

    fun load(id: Long) {
        playlistId = id
        viewModelScope.launch {
            _name.value = repository.get(id)?.name.orEmpty()
            _rows.value = repository.rowsFor(id)
        }
    }

    /** Local swap during a drag; nothing is persisted until the finger lifts. */
    fun move(from: Int, to: Int) {
        val list = _rows.value.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        list.add(to, list.removeAt(from))
        _rows.value = list
    }

    fun persistOrder() {
        val items = _rows.value.map { it.item }
        viewModelScope.launch { repository.reorder(items) }
    }

    fun remove(item: PlaylistItem) {
        viewModelScope.launch {
            repository.removeItem(item)
            _rows.value = repository.rowsFor(playlistId)
        }
    }

    /**
     * Plays from [startIndex], skipping unavailable items. Their positions are dropped
     * from the queue, so the index is remapped rather than passed through.
     */
    fun play(startIndex: Int, shuffle: Boolean = false) {
        val rows = _rows.value
        val playable = rows.filter { it.available }
        if (playable.isEmpty()) return
        val target = rows.getOrNull(startIndex)?.takeIf { it.available }
        val index = target?.let { playable.indexOf(it) } ?: 0
        playerController.player.shuffleModeEnabled = shuffle
        playerController.prepareQueue(
            playable.map {
                QueueItem(
                    uri = it.item.uri,
                    title = it.item.title,
                    artist = it.item.artist,
                    album = "",
                    artworkUri = it.albumArtUri?.let(android.net.Uri::parse),
                )
            },
            index,
        )
    }
}
