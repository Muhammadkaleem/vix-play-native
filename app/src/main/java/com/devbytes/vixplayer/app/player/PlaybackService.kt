package com.devbytes.vixplayer.app.player

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Hosts the [MediaSession] so playback survives the Activity: rich notification,
 * lock-screen controls, Bluetooth and hardware media buttons.
 *
 * The session wraps the app-scoped [PlayerController.player] rather than creating its own,
 * so there is exactly one player and no state to synchronise between service and UI.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var controller: PlayerController

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        session = MediaSession.Builder(this, controller.player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /**
     * The task was swiped away. If nothing is playing there's no reason to stay alive, so
     * free the decoders and stop — otherwise keep running so audio continues.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = controller.player
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            controller.stop()
            stopSelf()
        }
    }

    override fun onDestroy() {
        // Release the session only. The player is an app-scoped singleton and must stay
        // usable if the user returns; the process going away is what actually frees it.
        session?.release()
        session = null
        super.onDestroy()
    }
}
