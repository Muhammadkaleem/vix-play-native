package com.devbytes.vixplayer.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.devbytes.vixplayer.app.data.db.entity.EqPreset
import com.devbytes.vixplayer.app.data.db.entity.EqProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface EqDao {
    @Query("SELECT * FROM eq_profile WHERE output = :output")
    suspend fun getProfile(output: String): EqProfile?

    @Upsert
    suspend fun upsertProfile(profile: EqProfile)

    @Query("SELECT * FROM eq_preset ORDER BY name COLLATE NOCASE ASC")
    fun observePresets(): Flow<List<EqPreset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: EqPreset): Long

    @Delete
    suspend fun deletePreset(preset: EqPreset)
}
