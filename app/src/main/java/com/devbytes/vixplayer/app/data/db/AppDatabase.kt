package com.devbytes.vixplayer.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.devbytes.vixplayer.app.data.db.dao.PlaybackPositionDao
import com.devbytes.vixplayer.app.data.db.entity.PlaybackPosition

@Database(
    entities = [PlaybackPosition::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playbackPositionDao(): PlaybackPositionDao
}
