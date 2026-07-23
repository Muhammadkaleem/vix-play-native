package com.devbytes.vixplayer.app.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
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
)

data class FolderEntry(
    val bucketId: Long,
    val name: String,
    val videoCount: Int,
    val sampleUri: Uri,
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
        // bucket -> (name, count, sampleUri). Ordered by DATE_MODIFIED DESC so the first
        // video seen per bucket becomes the thumbnail.
        val buckets = linkedMapOf<Long, Triple<String, Int, Uri>>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        )
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, null, null,
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val idCol     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val bidCol    = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val bnameCol  = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id   = cursor.getLong(idCol)
                val bid  = cursor.getLong(bidCol)
                val name = cursor.getString(bnameCol) ?: "Unknown"
                val uri  = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val existing = buckets[bid]
                buckets[bid] = if (existing == null) Triple(name, 1, uri)
                               else existing.copy(second = existing.second + 1)
            }
        }
        buckets.entries
            .sortedByDescending { it.value.second }
            .map { (bid, triple) ->
                FolderEntry(
                    bucketId    = bid,
                    name        = triple.first,
                    videoCount  = triple.second,
                    sampleUri   = triple.third,
                )
            }
    }

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
            "${MediaStore.Video.Media.BUCKET_ID} = ?",
            arrayOf(bucketId.toString()),
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
}
