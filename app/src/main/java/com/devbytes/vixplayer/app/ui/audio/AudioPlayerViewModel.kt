package com.devbytes.vixplayer.app.ui.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.AudioRepository
import com.devbytes.vixplayer.app.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioPlayerViewModel @Inject constructor(
    private val audioRepository: AudioRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    /** The shared app-scoped player — the same instance the library queued into. */
    val player = playerController.player

    /**
     * Queues [mediaStoreId] only if nothing is loaded — covers reaching this screen
     * without the library having queued first (deep link, notification, external intent).
     * A no-op in the normal path, so it never restarts what is already playing.
     */
    fun ensureQueued(mediaStoreId: Long) {
        if (mediaStoreId <= 0L || player.mediaItemCount > 0) return
        viewModelScope.launch {
            val tracks = audioRepository.queryAllTracks()
            val index = tracks.indexOfFirst { it.mediaStoreId == mediaStoreId }
            if (index >= 0) {
                playerController.prepareQueue(tracks.map { it.toQueueItem() }, index)
            }
        }
    }
}
