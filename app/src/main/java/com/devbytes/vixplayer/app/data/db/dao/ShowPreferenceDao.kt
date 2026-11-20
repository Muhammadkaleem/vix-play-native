package com.devbytes.vixplayer.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devbytes.vixplayer.app.data.db.entity.ShowPreference

@Dao
interface ShowPreferenceDao {
    @Query("SELECT * FROM show_preference WHERE showKey = :showKey")
    suspend fun get(showKey: String): ShowPreference?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preference: ShowPreference)
}
