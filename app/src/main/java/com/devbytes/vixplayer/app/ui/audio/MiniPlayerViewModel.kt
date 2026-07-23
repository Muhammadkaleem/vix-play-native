package com.devbytes.vixplayer.app.ui.audio

import androidx.lifecycle.ViewModel
import com.devbytes.vixplayer.app.player.PlaybackKind
import com.devbytes.vixplayer.app.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MiniPlayerViewModel @Inject constructor(
    playerController: PlayerController,
) : ViewModel() {

    val player = playerController.player

    /**
     * Video and audio share one player and `stop()` keeps the playlist, so visibility
     * keys off what was actually loaded rather than "is something queued".
     */
    val kind: StateFlow<PlaybackKind> = playerController.kind
}
