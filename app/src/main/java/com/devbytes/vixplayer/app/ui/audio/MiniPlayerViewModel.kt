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

    /** The queue the last [dismiss] stopped, kept until [undoDismiss] or the next dismissal. */
    private var lastDismissed: PlayerController.QueueSnapshot? = null

    /**
     * Dismisses the bar by stopping playback (kind → NONE, which hides it). There is no
     * "hide but keep playing" — without a visible bar there'd be no way back to it, and
     * the notification already offers a keep-playing surface. The queue is snapshotted
     * first so the dismissal can be undone.
     */
    fun dismiss() {
        lastDismissed = playerController.snapshotQueue()
        playerController.stop()
    }

    /**
     * Restores the queue [dismiss] stopped — only while nothing else has started since, so
     * a late Undo can never clobber a video or a newly chosen track.
     */
    fun undoDismiss() {
        val snapshot = lastDismissed ?: return
        lastDismissed = null
        if (kind.value == PlaybackKind.NONE) playerController.restoreQueue(snapshot)
    }
}
