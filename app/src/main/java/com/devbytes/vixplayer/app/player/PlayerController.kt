package com.devbytes.vixplayer.app.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.devbytes.vixplayer.app.player.subtitle.OffsetSubtitleParserFactory
import com.devbytes.vixplayer.app.player.subtitle.SubtitleOffsetHolder
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the shared player currently holds. Video and audio use the same [ExoPlayer], and
 * `stop()` keeps the playlist, so "something is loaded" alone can't tell them apart —
 * the mini-player would otherwise advertise a video the user just exited.
 */
enum class PlaybackKind { NONE, VIDEO, AUDIO }

/** One queued track plus the metadata the session, notification and UI all read back. */
data class QueueItem(
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUri: Uri?,
)

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

    private val _kind = MutableStateFlow(PlaybackKind.NONE)

    /** Drives mini-player visibility: audio should surface a bar, video should not. */
    val kind: StateFlow<PlaybackKind> = _kind.asStateFlow()

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
        _kind.value = PlaybackKind.VIDEO
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
    fun prepareQueue(items: List<QueueItem>, startIndex: Int) {
        if (items.isEmpty()) return
        subtitleOffset.offsetUs = 0L
        player.setMediaItems(
            items.map { it.toMediaItem() },
            startIndex.coerceIn(0, items.lastIndex),
            /* startPositionMs = */ 0L,
        )
        player.prepare()
        player.playWhenReady = true
        _kind.value = PlaybackKind.AUDIO
    }

    /**
     * Metadata travels on the [MediaItem] rather than in a map beside the queue, so the
     * player stays the single source of truth: the mini-player and the now-playing screen
     * read it back with no library lookup, and the MediaSession notification and lock
     * screen pick up title/artist/artwork automatically.
     */
    private fun QueueItem.toMediaItem(): MediaItem = MediaItem.Builder()
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(artworkUri)
                .build()
        )
        .build()

    /**
     * Frees decoders without destroying the instance. Used when the task is dismissed and
     * nothing is playing — the player must stay usable for when the user comes back, since
     * a released one would leave this singleton holding a dead object.
     */
    fun stop() {
        player.stop()
        player.clearMediaItems()
        _kind.value = PlaybackKind.NONE
    }
}
