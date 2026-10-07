package com.devbytes.vixplayer.app.data.repository

import com.devbytes.vixplayer.app.data.db.dao.FavoriteDao
import com.devbytes.vixplayer.app.data.db.entity.Favorite
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Favourite videos, keyed by MediaStore id. Shared by the dashboard, player, and card overflow. */
@Singleton
class FavoritesRepository @Inject constructor(
    private val dao: FavoriteDao,
) {
    fun observeIds(): Flow<List<Long>> = dao.observeIds()

    fun observeIsFavorite(mediaStoreId: Long): Flow<Boolean> = dao.observeIsFavorite(mediaStoreId)

    suspend fun toggle(mediaStoreId: Long) {
        if (dao.isFavorite(mediaStoreId)) {
            dao.delete(mediaStoreId)
        } else {
            dao.insert(Favorite(mediaStoreId, System.currentTimeMillis()))
        }
    }
}
