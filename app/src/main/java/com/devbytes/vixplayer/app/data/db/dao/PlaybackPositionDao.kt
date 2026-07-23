package com.devbytes.vixplayer.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devbytes.vixplayer.app.data.db.entity.PlaybackPosition

@Dao
interface PlaybackPositionDao {

    @Query("SELECT * FROM playback_positions WHERE mediaStoreId = :id")
    suspend fun getPosition(id: Long): PlaybackPosition?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(position: PlaybackPosition): Long

    @Query("SELECT * FROM playback_positions ORDER BY lastPlayedAt DESC LIMIT :limit")
    suspend fun getRecentPositions(limit: Int = 20): List<PlaybackPosition>

    @Query("SELECT * FROM playback_positions")
    suspend fun getAll(): List<PlaybackPosition>

    @Query("DELETE FROM playback_positions WHERE mediaStoreId = :id")
    suspend fun delete(id: Long): Int

    @Query("DELETE FROM playback_positions")
    suspend fun clearAll(): Int
}
