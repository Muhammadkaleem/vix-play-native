package com.devbytes.vixplayer.app.player

import android.content.Context
import android.content.Intent
import android.media.AudioManager
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
    @ApplicationContext private val context: Context,
    private val audioEffects: AudioEffects,
) {
    /**
     * The app generates and owns its audio session, rather than letting ExoPlayer pick
     * one per track. Audio effects bind to this id once and stay bound: a session we own
     * cannot be invalidated by a track change or queue transition, which removes the
     * rebind problem entirely instead of handling it.
     */
    private val audioSessionId: Int =
        (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
            .generateAudioSessionId()

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
        .apply { setAudioSessionId(this@PlayerController.audioSessionId) }

    init {
        // Safe on unsupported hardware: attach() swallows the failure and reports
        // isSupported = false, which the UI renders as an explicit disabled state.
        audioEffects.attach(audioSessionId)
    }

    /**
     * Opens [uri], resetting every piece of per-file state in one place.
     *
     * Everything reset here used to be discarded implicitly when the composable's
     * `remember` went out of scope; with an app-scoped player it must be explicit, or a
     * previous video's subtitle offset silently applies to the next one.
     */
    fun prepareFor(uri: String, subtitleOffsetMs: Long) {
        ensureServiceRunning()
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
        ensureServiceRunning()
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
     * Appends to the end of the current queue without disturbing what is playing.
     * If nothing is loaded this behaves as [prepareQueue].
     */
    fun enqueue(items: List<QueueItem>) {
        if (items.isEmpty()) return
        if (player.mediaItemCount == 0) {
            prepareQueue(items, 0)
            return
        }
        player.addMediaItems(items.map { it.toMediaItem() })
        _kind.value = PlaybackKind.AUDIO
    }

    /**
     * Drops [uris] from the active queue, skipping onward if one of them is playing.
     *
     * Without this, deleting the current track leaves the player holding a dead URI and
     * surfaces as a playback error — which reads as a crash rather than a consequence of
     * what the user just did.
     */
    fun removeFromQueue(uris: Set<String>) {
        if (uris.isEmpty() || player.mediaItemCount == 0) return
        // Walk backwards so removals don't shift the indices still to be checked.
        for (index in player.mediaItemCount - 1 downTo 0) {
            val itemUri = player.getMediaItemAt(index).localConfiguration?.uri?.toString()
            if (itemUri != null && itemUri in uris) {
                player.removeMediaItem(index)
            }
        }
        if (player.mediaItemCount == 0) {
            player.stop()
            player.clearMediaItems()
            _kind.value = PlaybackKind.NONE
        }
    }

    /**
     * Starts [PlaybackService] so a `MediaSession` exists for whatever is about to play.
     *
     * Deliberately tied to **playback**, not to a screen: it was originally started from
     * `PlayerScreen`, which meant audio started from the library never got a session at
     * all — no notification, no lock-screen or Bluetooth controls. Both entry points into
     * playback go through this class, so this is the one place that sees every start.
     */
    private fun ensureServiceRunning() {
        runCatching {
            context.startService(Intent(context, PlaybackService::class.java))
        }
    }

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
