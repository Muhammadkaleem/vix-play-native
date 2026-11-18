package com.devbytes.vixplayer.app.ui.audio

import android.content.ContentUris
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.AudioRepository
import com.devbytes.vixplayer.app.player.PlaybackKind
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
     * Queues [mediaStoreId] (with the whole library, so next/previous work) unless it is
     * already the current audio item — covers reaching this screen without the library
     * having queued first (search, deep link, notification). A no-op in the normal path, so
     * it never restarts what is already playing. Checking the current item rather than "is
     * anything loaded" matters: a stopped video, or another track's queue, stays loaded.
     */
    fun ensureQueued(mediaStoreId: Long) {
        if (mediaStoreId <= 0L) return
        val target = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaStoreId)
        val current = player.currentMediaItem?.localConfiguration?.uri
        if (playerController.kind.value == PlaybackKind.AUDIO && current == target) return
        viewModelScope.launch {
            val tracks = audioRepository.queryAllTracks()
            val index = tracks.indexOfFirst { it.mediaStoreId == mediaStoreId }
            if (index >= 0) {
                playerController.prepareQueue(tracks.map { it.toQueueItem() }, index)
            }
        }
    }
}
