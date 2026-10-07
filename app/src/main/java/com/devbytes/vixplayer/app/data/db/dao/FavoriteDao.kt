package com.devbytes.vixplayer.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devbytes.vixplayer.app.data.db.entity.Favorite
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT mediaStoreId FROM favorite ORDER BY addedAt DESC")
    fun observeIds(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE mediaStoreId = :id)")
    fun observeIsFavorite(id: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE mediaStoreId = :id)")
    suspend fun isFavorite(id: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: Favorite)

    @Query("DELETE FROM favorite WHERE mediaStoreId = :id")
    suspend fun delete(id: Long)
}
