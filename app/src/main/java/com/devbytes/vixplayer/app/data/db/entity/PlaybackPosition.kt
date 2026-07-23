package com.devbytes.vixplayer.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_positions")
data class PlaybackPosition(
    @PrimaryKey val mediaStoreId: Long,
    val positionMs: Long,
    val lastPlayedAt: Long,
)
