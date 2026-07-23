package com.devbytes.vixplayer.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlist")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

/**
 * One entry in a playlist.
 *
 * [title] and [artist] are cached copies, not just a denormalisation shortcut: the PRD
 * requires missing items be *flagged, not silently skipped*, and a row for a deleted file
 * cannot be labelled unless its name was stored when it was added.
 *
 * Deleting a playlist cascades to its items.
 */
@Entity(
    tableName = "playlist_item",
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("playlistId")],
)
data class PlaylistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val mediaStoreId: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val position: Int,
)

/** A playlist plus the counts the list screen shows. */
data class PlaylistSummary(
    val id: Long,
    val name: String,
    val itemCount: Int,
    val artUri: String?,
)
