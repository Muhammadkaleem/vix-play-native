package com.devbytes.vixplayer.app.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class VideoFile(
    val mediaStoreId: Long,
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val path: String,
    val sizeBytes: Long,
    /** Pixel dimensions from MediaStore (0 when unknown). Used for the quality badge. */
    val width: Int = 0,
    val height: Int = 0,
    /** Epoch millis the file was added to MediaStore (0 when unknown). */
    val dateAddedMs: Long = 0,
)

data class FolderEntry(
    val bucketId: Long,
    val name: String,
    val videoCount: Int,
    val sampleUri: Uri,
    /** Newest DATE_MODIFIED in the bucket, for the "Recent" folder sort. */
    val dateModified: Long,
)

@Singleton
class MediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun queryAllVideos(): List<VideoFile> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<VideoFile>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
        )
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null, null,
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val idCol   = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durCol  = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                videos.add(
                    VideoFile(
                        mediaStoreId = id,
                        uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
                        name = cursor.getString(nameCol) ?: "Unknown",
                        durationMs = cursor.getLong(durCol),
                        path = cursor.getString(pathCol) ?: "",
                        sizeBytes = cursor.getLong(sizeCol),
                    )
                )
            }
        }
        videos
    }

    suspend fun queryVideoById(mediaStoreId: Long): VideoFile? = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
        )
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Video.Media._ID} = ?",
            arrayOf(mediaStoreId.toString()),
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val idCol   = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durCol  = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val id = cursor.getLong(idCol)
            VideoFile(
                mediaStoreId = id,
                uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
                name = cursor.getString(nameCol) ?: "Unknown",
                durationMs = cursor.getLong(durCol),
                path = cursor.getString(pathCol) ?: "",
                sizeBytes = cursor.getLong(sizeCol),
            )
        }
    }

    suspend fun queryFolders(): List<FolderEntry> = withContext(Dispatchers.IO) {
        // bucket -> (name, count, sampleUri, newestDate). Ordered by DATE_MODIFIED DESC so
        // the first video seen per bucket becomes the thumbnail and carries the newest date.
        val buckets = linkedMapOf<Long, Quad>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATE_MODIFIED,
        )
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, null, null,
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val idCol     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val bidCol    = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val bnameCol  = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val dateCol   = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            while (cursor.moveToNext()) {
                val id   = cursor.getLong(idCol)
                val bid  = cursor.getLong(bidCol)
                val name = cursor.getString(bnameCol) ?: "Unknown"
                val date = cursor.getLong(dateCol)
                val uri  = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val existing = buckets[bid]
                buckets[bid] = if (existing == null) Quad(name, 1, uri, date)
                               else existing.copy(second = existing.second + 1)
            }
        }
        // Name A–Z default: a folder's position changes only when it is renamed, never
        // when a file is added, moved or removed. The VM re-sorts for other orders.
        buckets.entries
            .sortedBy { it.value.first.lowercase() }
            .map { (bid, q) ->
                FolderEntry(
                    bucketId     = bid,
                    name         = q.first,
                    videoCount   = q.second,
                    sampleUri    = q.third,
                    dateModified = q.fourth,
                )
            }
    }

    /** Local 4-tuple: MediaStore aggregation needs name, running count, sample, newest date. */
    private data class Quad(
        val first: String,
        val second: Int,
        val third: Uri,
        val fourth: Long,
    )

    /**
     * The next video in the same folder as [mediaStoreId], following the folder's own
     * ordering (DATE_MODIFIED DESC — the order the folder browser lists them). Returns
     * null when the id isn't a MediaStore item, is the last file, or is the only one.
     */
    suspend fun nextInFolder(mediaStoreId: Long): VideoFile? {
        val bucketId = queryBucketId(mediaStoreId) ?: return null
        val folder = queryVideosInFolder(bucketId)
        val index = folder.indexOfFirst { it.mediaStoreId == mediaStoreId }
        return if (index in 0 until folder.lastIndex) folder[index + 1] else null
    }

    /** Other videos in the same folder as [mediaStoreId] (excluding it). For "From this folder". */
    suspend fun videosInFolderOf(mediaStoreId: Long): List<VideoFile> {
        val bucketId = queryBucketId(mediaStoreId) ?: return emptyList()
        return queryVideosInFolder(bucketId).filterNot { it.mediaStoreId == mediaStoreId }
    }

    /** The "up next" set for the end-of-video screen: folder videos after the current one
     *  first, then wrapping to the start (current excluded). Empty for non-MediaStore URIs. */
    suspend fun upcomingInFolder(mediaStoreId: Long, limit: Int): List<VideoFile> {
        val bucketId = queryBucketId(mediaStoreId) ?: return emptyList()
        val folder = queryVideosInFolder(bucketId)
        val index = folder.indexOfFirst { it.mediaStoreId == mediaStoreId }
        if (index < 0) return folder.take(limit)
        return (folder.drop(index + 1) + folder.take(index)).take(limit)
    }

    /** The current video's 1-based position and the folder's total count, or null if unknown. */
    suspend fun folderPosition(mediaStoreId: Long): Pair<Int, Int>? {
        val bucketId = queryBucketId(mediaStoreId) ?: return null
        val folder = queryVideosInFolder(bucketId)
        val index = folder.indexOfFirst { it.mediaStoreId == mediaStoreId }
        if (index < 0) return null
        return (index + 1) to folder.size
    }

    private suspend fun queryBucketId(mediaStoreId: Long): Long? = withContext(Dispatchers.IO) {
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Video.Media.BUCKET_ID),
            "${MediaStore.Video.Media._ID} = ?",
            arrayOf(mediaStoreId.toString()),
            null,
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }
    }

    suspend fun queryVideosInFolder(bucketId: Long): List<VideoFile> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<VideoFile>()
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            baseProjection,
            "${MediaStore.Video.Media.BUCKET_ID} = ?",
            arrayOf(bucketId.toString()),
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            while (cursor.moveToNext()) videos.add(cursor.toVideoFile())
        }
        videos
    }

    /**
     * Videos currently in the system trash (API 30+ only; there is no trash below 30).
     * `MATCH_ONLY` flips the default query, which *excludes* trashed items, to return only them.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun queryTrashedVideos(): List<VideoFile> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<VideoFile>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
        )
        val args = Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
            putString(
                ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC",
            )
        }
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, args, null,
        )?.use { cursor ->
            val idCol   = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durCol  = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                videos.add(
                    VideoFile(
                        mediaStoreId = id,
                        uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
                        name = cursor.getString(nameCol) ?: "Unknown",
                        durationMs = cursor.getLong(durCol),
                        path = cursor.getString(pathCol) ?: "",
                        sizeBytes = cursor.getLong(sizeCol),
                    )
                )
            }
        }
        videos
    }

    private val baseProjection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.DATA,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.WIDTH,
        MediaStore.Video.Media.HEIGHT,
        MediaStore.Video.Media.DATE_ADDED,
    )

    private fun android.database.Cursor.toVideoFile(): VideoFile {
        val id = getLong(getColumnIndexOrThrow(MediaStore.Video.Media._ID))
        return VideoFile(
            mediaStoreId = id,
            uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
            name = getString(getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)) ?: "Unknown",
            durationMs = getLong(getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)),
            path = getString(getColumnIndexOrThrow(MediaStore.Video.Media.DATA)) ?: "",
            sizeBytes = getLong(getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)),
            width = getInt(getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)),
            height = getInt(getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)),
            // DATE_ADDED is seconds since epoch → millis.
            dateAddedMs = getLong(getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)) * 1000L,
        )
    }

    /** The [limit] most recently added videos (DATE_ADDED DESC). Powers the dashboard rail. */
    suspend fun queryRecentlyAdded(limit: Int): List<VideoFile> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<VideoFile>()
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, baseProjection, null, null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            while (cursor.moveToNext() && videos.size < limit) videos.add(cursor.toVideoFile())
        }
        videos
    }

    /** Videos by MediaStore id (used to resolve the favourites set against the live library). */
    suspend fun queryVideosByIds(ids: List<Long>): List<VideoFile> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        val placeholders = ids.joinToString(",") { "?" }
        val videos = mutableListOf<VideoFile>()
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, baseProjection,
            "${MediaStore.Video.Media._ID} IN ($placeholders)",
            ids.map { it.toString() }.toTypedArray(),
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) videos.add(cursor.toVideoFile())
        }
        // Preserve the caller's id order (favourites are addedAt DESC).
        val byId = videos.associateBy { it.mediaStoreId }
        ids.mapNotNull { byId[it] }
    }

    /**
     * Portrait ("Shorts") videos — height > width. Rotation metadata isn't factored in for v1,
     * so a rotated-landscape clip can be misclassified; acceptable for a dashboard surface.
     */
    suspend fun queryShorts(limit: Int = Int.MAX_VALUE): List<VideoFile> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<VideoFile>()
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, baseProjection,
            "${MediaStore.Video.Media.HEIGHT} > ${MediaStore.Video.Media.WIDTH} " +
                "AND ${MediaStore.Video.Media.WIDTH} > 0",
            null,
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            while (cursor.moveToNext() && videos.size < limit) videos.add(cursor.toVideoFile())
        }
        videos
    }

    /**
     * High-resolution videos — longest side ≥ 1920 (Full HD 1080p and up, incl. 2K/4K),
     * highest resolution first. Powers the dashboard "HD & 4K" rail. WIDTH/HEIGHT can be
     * absent for some files; those simply fall out of the filter.
     */
    suspend fun queryHighResVideos(limit: Int = Int.MAX_VALUE): List<VideoFile> = withContext(Dispatchers.IO) {
        val w = MediaStore.Video.Media.WIDTH
        val h = MediaStore.Video.Media.HEIGHT
        val videos = mutableListOf<VideoFile>()
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, baseProjection,
            "$w >= 1920 OR $h >= 1920",
            null,
            "$w * $h DESC",
        )?.use { cursor ->
            while (cursor.moveToNext() && videos.size < limit) videos.add(cursor.toVideoFile())
        }
        videos
    }

    /**
     * Videos whose runtime fits within [maxDurationMs] (longest-fitting first), for the
     * "How much time do you have?" rail. Powers all four windows from one query; the UI
     * filters the pool client-side so switching window needs no re-query.
     */
    suspend fun queryByMaxDuration(maxDurationMs: Long, limit: Int = Int.MAX_VALUE): List<VideoFile> =
        withContext(Dispatchers.IO) {
            val videos = mutableListOf<VideoFile>()
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, baseProjection,
                "${MediaStore.Video.Media.DURATION} > 0 AND ${MediaStore.Video.Media.DURATION} <= ?",
                arrayOf(maxDurationMs.toString()),
                "${MediaStore.Video.Media.DURATION} DESC",
            )?.use { cursor ->
                while (cursor.moveToNext() && videos.size < limit) videos.add(cursor.toVideoFile())
            }
            videos
        }

    suspend fun countShorts(): Int = withContext(Dispatchers.IO) {
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Video.Media._ID),
            "${MediaStore.Video.Media.HEIGHT} > ${MediaStore.Video.Media.WIDTH} " +
                "AND ${MediaStore.Video.Media.WIDTH} > 0",
            null, null,
        )?.use { it.count } ?: 0
    }

    /** Epoch millis of the most recently added video, or 0 if the library is empty. */
    suspend fun queryNewestVideoAddedDate(): Long = withContext(Dispatchers.IO) {
        // Note: no "LIMIT 1" in the sort string — ContentResolver.query validates the sort
        // clause and rejects LIMIT on modern Android. We just read the first (newest) row.
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Video.Media.DATE_ADDED),
            null, null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                // DATE_ADDED is seconds since epoch.
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)) * 1000L
            } else 0L
        } ?: 0L
    }

    /** How many videos were added strictly after [sinceMillis]. Powers the "new in library" nudge. */
    suspend fun countVideosAddedAfter(sinceMillis: Long): Int = withContext(Dispatchers.IO) {
        val sinceSeconds = sinceMillis / 1000L
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Video.Media._ID),
            "${MediaStore.Video.Media.DATE_ADDED} > ?",
            arrayOf(sinceSeconds.toString()),
            null,
        )?.use { it.count } ?: 0
    }
}
