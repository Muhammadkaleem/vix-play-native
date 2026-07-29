package com.devbytes.vixplayer.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devbytes.vixplayer.app.data.db.entity.NetworkStream
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkStreamDao {

    @Query("SELECT * FROM network_stream ORDER BY lastPlayedAt DESC")
    fun observeRecents(): Flow<List<NetworkStream>>

    /** REPLACE so replaying an existing URL refreshes its timestamp instead of duplicating. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stream: NetworkStream)

    @Delete
    suspend fun delete(stream: NetworkStream)

    /** Keeps only the [keep] most recent rows so the list can't grow without bound. */
    @Query(
        "DELETE FROM network_stream WHERE url NOT IN " +
            "(SELECT url FROM network_stream ORDER BY lastPlayedAt DESC LIMIT :keep)"
    )
    suspend fun trim(keep: Int)
}
