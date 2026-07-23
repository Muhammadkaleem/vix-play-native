package com.devbytes.vixplayer.app.player

import android.app.PictureInPictureParams

/**
 * Bridge between the composable-owned player ([PlayerScreen][com.devbytes.vixplayer.app.ui.player.PlayerScreen])
 * and the Activity-level Picture-in-Picture hooks (`onUserLeaveHint` /
 * `onPictureInPictureModeChanged`), which only exist on the Activity.
 *
 * PlayerScreen registers these callbacks while it's on screen and clears them on
 * dispose; MainActivity calls them. This keeps PlayerScreen the single owner of
 * *when* PiP is valid, while the Activity owns the OS hooks — no duplicated
 * playback state. Migrates to the Step-5 `PlayerController` cleanly.
 */
class PipController {
    /** True only while a video is actively playing and entering PiP makes sense. */
    var canEnterPip: () -> Boolean = { false }

    /** Current PiP params (video aspect + play/pause action); null when unsupported. */
    var buildParams: () -> PictureInPictureParams? = { null }

    /** Pushed down so PlayerScreen can hide chrome + refresh the action icon. */
    var onModeChanged: (Boolean) -> Unit = {}
}
