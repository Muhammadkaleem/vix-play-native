package com.devbytes.vixplayer.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.devbytes.vixplayer.app.data.db.dao.EqDao
import com.devbytes.vixplayer.app.data.db.dao.PlaybackPositionDao
import com.devbytes.vixplayer.app.data.db.entity.EqPreset
import com.devbytes.vixplayer.app.data.db.entity.EqProfile
import com.devbytes.vixplayer.app.data.db.entity.PlaybackPosition

@Database(
    entities = [PlaybackPosition::class, EqProfile::class, EqPreset::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(EqConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playbackPositionDao(): PlaybackPositionDao
    abstract fun eqDao(): EqDao
}
