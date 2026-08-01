package com.devbytes.vixplayer.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.devbytes.vixplayer.app.data.db.entity.NetworkServer
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkServerDao {

    @Query("SELECT * FROM network_server ORDER BY addedAt DESC")
    fun observeServers(): Flow<List<NetworkServer>>

    @Query("SELECT * FROM network_server WHERE id = :id")
    suspend fun getById(id: Long): NetworkServer?

    @Insert
    suspend fun insert(server: NetworkServer): Long

    @Delete
    suspend fun delete(server: NetworkServer)
}
