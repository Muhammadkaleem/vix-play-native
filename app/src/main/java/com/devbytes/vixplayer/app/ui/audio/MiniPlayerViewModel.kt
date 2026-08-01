package com.devbytes.vixplayer.app.ui.audio

import androidx.lifecycle.ViewModel
import com.devbytes.vixplayer.app.player.PlaybackKind
import com.devbytes.vixplayer.app.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MiniPlayerViewModel @Inject constructor(
    private val playerController: PlayerController,
) : ViewModel() {

    val player = playerController.player

    /**
     * Video and audio share one player and `stop()` keeps the playlist, so visibility
     * keys off what was actually loaded rather than "is something queued".
     */
    val kind: StateFlow<PlaybackKind> = playerController.kind

    /**
     * Dismisses the bar by stopping playback (kind → NONE, which hides it). There is no
     * "hide but keep playing" — without a visible bar there'd be no way back to it, and
     * the notification already offers a keep-playing surface.
     */
    fun dismiss() = playerController.stop()
}
