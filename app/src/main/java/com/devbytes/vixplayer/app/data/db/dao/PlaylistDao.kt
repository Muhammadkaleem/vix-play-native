package com.devbytes.vixplayer.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.devbytes.vixplayer.app.data.db.entity.Playlist
import com.devbytes.vixplayer.app.data.db.entity.PlaylistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlist ORDER BY name COLLATE NOCASE ASC")
    fun observePlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlist WHERE id = :id")
    suspend fun getPlaylist(id: Long): Playlist?

    @Insert
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query("SELECT COUNT(*) FROM playlist_item WHERE playlistId = :playlistId")
    suspend fun itemCount(playlistId: Long): Int

    @Query("SELECT * FROM playlist_item WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observeItems(playlistId: Long): Flow<List<PlaylistItem>>

    @Query("SELECT * FROM playlist_item WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun itemsFor(playlistId: Long): List<PlaylistItem>

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_item WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: Long): Int

    @Insert
    suspend fun insertItem(item: PlaylistItem)

    @Delete
    suspend fun deleteItem(item: PlaylistItem)

    @Query("UPDATE playlist_item SET position = :position WHERE id = :itemId")
    suspend fun updatePosition(itemId: Long, position: Int)

    /** Rewrites the whole ordering in one transaction, called once on drag-drop. */
    @Transaction
    suspend fun reorder(items: List<PlaylistItem>) {
        items.forEachIndexed { index, item -> updatePosition(item.id, index) }
    }
}
