package com.devbytes.vixplayer.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A favourited video, keyed by its MediaStore id. */
@Entity(tableName = "favorite")
data class Favorite(
    @PrimaryKey val mediaStoreId: Long,
    val addedAt: Long,
)
