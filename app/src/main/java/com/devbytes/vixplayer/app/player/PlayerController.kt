package com.devbytes.vixplayer.app.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.devbytes.vixplayer.app.player.subtitle.OffsetSubtitleParserFactory
import com.devbytes.vixplayer.app.player.subtitle.SubtitleOffsetHolder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the one [ExoPlayer] instance for the whole app.
 *
 * Background playback requires the player to outlive the Activity, so it can no longer be
 * created inside `PlayerScreen`'s composition — this supersedes the earlier
 * `@ActivityRetainedScoped` plan recorded in CLAUDE.md.
 *
 * The app is single-process, so `PlaybackService` and the UI hold this *same* object and
 * the UI keeps calling ExoPlayer directly. That deliberately skips `MediaController`:
 * the `MediaSession` alone already publishes to the notification, lock screen, Bluetooth
 * and media buttons, while a controller would cost an async-null connection window and
 * hide ExoPlayer-only APIs the File Info sheet depends on (`videoFormat`/`audioFormat`).
 *
 * The tradeoff is that per-file state no longer resets implicitly when the screen leaves
 * composition — [prepareFor] is the single entry point that resets it explicitly.
 */
@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext context: Context,
) {
    /** Read by the subtitle parser at parse time; see [OffsetSubtitleParserFactory]. */
    val subtitleOffset = SubtitleOffsetHolder()

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(context)
                .setSubtitleParserFactory(OffsetSubtitleParserFactory(subtitleOffset))
        )
        // handleAudioFocus = true gives pause-on-transient-loss, ducking, and resume,
        // which is what the PRD's "audio focus behavior matches Android guidelines" means.
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            /* handleAudioFocus = */ true,
        )
        // Pause when headphones are unplugged rather than blasting the speaker.
        .setHandleAudioBecomingNoisy(true)
        .build()

    /**
     * Opens [uri], resetting every piece of per-file state in one place.
     *
     * Everything reset here used to be discarded implicitly when the composable's
     * `remember` went out of scope; with an app-scoped player it must be explicit, or a
     * previous video's subtitle offset silently applies to the next one.
     */
    fun prepareFor(uri: String, subtitleOffsetMs: Long) {
        subtitleOffset.offsetUs = subtitleOffsetMs * 1_000L
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = true
    }

    /**
     * Opens a queue of [uris] starting at [startIndex].
     *
     * ExoPlayer's own playlist *is* the queue — shuffle order, repeat modes and
     * prev/next transitions come from the engine rather than a parallel list that would
     * have to be reconciled with it on every event.
     *
     * Replaces whatever was playing. There is one player, so starting audio stops any
     * video — in practice `PlayerScreen` has already persisted its resume position when
     * it left composition, so the interrupted video keeps its place in Continue Watching.
     */
    fun prepareQueue(uris: List<String>, startIndex: Int) {
        if (uris.isEmpty()) return
        subtitleOffset.offsetUs = 0L
        player.setMediaItems(
            uris.map { MediaItem.fromUri(it) },
            startIndex.coerceIn(0, uris.lastIndex),
            /* startPositionMs = */ 0L,
        )
        player.prepare()
        player.playWhenReady = true
    }

    /**
     * Frees decoders without destroying the instance. Used when the task is dismissed and
     * nothing is playing — the player must stay usable for when the user comes back, since
     * a released one would leave this singleton holding a dead object.
     */
    fun stop() {
        player.stop()
        player.clearMediaItems()
    }
}
