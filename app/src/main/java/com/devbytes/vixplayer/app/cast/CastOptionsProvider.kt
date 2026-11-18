package com.devbytes.vixplayer.app.cast

import android.content.Context
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

/**
 * Referenced from the manifest (`OPTIONS_PROVIDER_CLASS_NAME`). Uses Google's Default Media
 * Receiver, which needs no developer-console registration and plays the formats a Chromecast
 * decodes natively (H.264/VP8/VP9 + AAC/MP3/Opus/Vorbis/FLAC; HEVC/AV1 on newer devices).
 *
 * No notification options yet: the Cast SDK's own notification/lock-screen controller needs
 * `MediaIntentReceiver` plumbing — deferred; the in-app controls and PlaybackService cover v1.
 */
class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions =
        CastOptions.Builder()
            .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
            // Session resume lets the SDK rejoin a running session after an app restart.
            .setResumeSavedSession(true)
            // Don't tear the session down when the app is swiped away — the PlaybackService
            // keeps the HTTP server alive, so the receiver can keep fetching.
            .setStopReceiverApplicationWhenEndingSession(true)
            .build()

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}
