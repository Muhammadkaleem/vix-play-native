package com.devbytes.vixplayer.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.devbytes.vixplayer.app.data.db.dao.EqDao
import com.devbytes.vixplayer.app.data.db.dao.PlaylistDao
import com.devbytes.vixplayer.app.data.db.dao.PlaybackPositionDao
import com.devbytes.vixplayer.app.data.db.entity.EqPreset
import com.devbytes.vixplayer.app.data.db.entity.EqProfile
import com.devbytes.vixplayer.app.data.db.entity.PlaybackPosition
import com.devbytes.vixplayer.app.data.db.entity.Playlist
import com.devbytes.vixplayer.app.data.db.entity.PlaylistItem

@Database(
    entities = [
        PlaybackPosition::class,
        EqProfile::class,
        EqPreset::class,
        Playlist::class,
        PlaylistItem::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(EqConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playbackPositionDao(): PlaybackPositionDao
    abstract fun eqDao(): EqDao
    abstract fun playlistDao(): PlaylistDao
}
