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

/**
 * One audio track. [albumArtUri] is the MediaStore album-art content URI — Coil loads it
 * directly with the app's existing ImageLoader; a null/failed load falls back to a
 * placeholder rather than blocking the row.
 */
data class AudioTrack(
    val mediaStoreId: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val albumArtUri: Uri,
)

/**
 * Audio-side MediaStore access. Deliberately separate from [MediaRepository]: the
 * projections, grouping and sort order share nothing with video, so folding them together
 * would be two repositories in one file — and worse with every grouping tab added later.
 */
@Singleton
class AudioRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun queryAllTracks(): List<AudioTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<AudioTrack>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
        )
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            // Keeps ringtones, notification sounds and system alerts out of the library.
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        )?.use { cursor ->
            val idCol      = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)
                tracks.add(
                    AudioTrack(
                        mediaStoreId = id,
                        uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id,
                        ),
                        title = cursor.getString(titleCol) ?: "Unknown",
                        // MediaStore reports unknown artists as a literal "<unknown>".
                        artist = cursor.getString(artistCol)
                            ?.takeIf { it != MediaStore.UNKNOWN_STRING } ?: "Unknown artist",
                        album = cursor.getString(albumCol) ?: "",
                        durationMs = cursor.getLong(durCol),
                        albumArtUri = ContentUris.withAppendedId(ALBUM_ART_BASE, albumId),
                    )
                )
            }
        }
        tracks
    }

    private companion object {
        val ALBUM_ART_BASE: Uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
    }
}
