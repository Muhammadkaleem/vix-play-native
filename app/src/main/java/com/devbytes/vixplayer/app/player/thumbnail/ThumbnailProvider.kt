package com.devbytes.vixplayer.app.player.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Frames are decoded at keyframe granularity, so requests are snapped to this grid. */
private const val BUCKET_MS = 5_000L

/** Roughly 16:9 at a size that reads clearly in a HUD without wasting decode time. */
private const val FRAME_WIDTH = 320
private const val FRAME_HEIGHT = 180

/** How many decoded frames to keep; a drag back over the same region should be instant. */
private const val CACHE_SIZE = 24

/**
 * Supplies scrub-preview frames for a single video.
 *
 * Deliberately *not* Coil, even though the app registers `VideoFrameDecoder` app-wide:
 * Coil builds a [MediaMetadataRetriever] and calls `setDataSource` **per request**, which
 * is the expensive part. That's the right shape for the library grid (one frame each from
 * many videos) and the wrong shape for scrubbing (many frames from one video), so this
 * holds a single retriever open for the file and pays `setDataSource` once.
 *
 * Also not the PRD's sprite sheet: pre-decoding hundreds of frames would stall playback
 * start on a long file for a preview the user may never open. Sprite sheets are a
 * streaming pattern where a server builds them ahead of time.
 *
 * Not thread-safe by accident — [frameAt] serialises on a mutex because a retriever
 * cannot be used concurrently. Callers **must** call [release]; retrievers leak hard.
 */
class ThumbnailProvider(
    private val context: Context,
    private val uri: Uri,
) {
    private val retriever = MediaMetadataRetriever()
    private val cache = LruCache<Long, Bitmap>(CACHE_SIZE)
    private val mutex = Mutex()

    private var prepared = false
    private var unavailable = false
    private var released = false

    /**
     * The frame nearest [positionMs], or null when this source can't produce one
     * (network URIs, missing permission, codecs the retriever won't open).
     * Snapped to [BUCKET_MS] so a fast drag asks for tens of frames, not thousands.
     */
    suspend fun frameAt(positionMs: Long): Bitmap? {
        if (unavailable || released) return null
        val bucket = (positionMs / BUCKET_MS) * BUCKET_MS
        cache.get(bucket)?.let { return it }

        return withContext(Dispatchers.IO) {
            mutex.withLock {
                // Re-check: a queued caller may have decoded this bucket while we waited.
                cache.get(bucket)?.let { return@withLock it }
                if (released || !prepare()) return@withLock null
                val frame = runCatching { decode(bucket) }.getOrNull()
                if (frame == null) {
                    // One failure is a bad timestamp; keep serving other buckets.
                    null
                } else {
                    cache.put(bucket, frame)
                    frame
                }
            }
        }
    }

    /** Opens the source once. Returns false if it can't be read at all. */
    private fun prepare(): Boolean {
        if (prepared) return true
        if (unavailable) return false
        return try {
            retriever.setDataSource(context, uri)
            prepared = true
            true
        } catch (e: Exception) {
            unavailable = true
            false
        }
    }

    private fun decode(atMs: Long): Bitmap? {
        val us = atMs * 1_000L
        val option = MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(us, option, FRAME_WIDTH, FRAME_HEIGHT)
        } else {
            // getScaledFrameAtTime is API 27+; older devices decode full-size.
            retriever.getFrameAtTime(us, option)
        }
    }

    fun release() {
        if (released) return
        released = true
        cache.evictAll()
        runCatching { retriever.release() }
    }
}
