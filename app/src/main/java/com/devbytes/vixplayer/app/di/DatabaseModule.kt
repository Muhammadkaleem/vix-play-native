package com.devbytes.vixplayer.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.devbytes.vixplayer.app.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Adds the equalizer tables. Purely additive — written as a real migration rather than a
 * destructive fallback because v1 holds the resume-position history that Continue
 * Watching and the History screen are built on, and dropping it would silently erase
 * every saved playback position.
 */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `eq_profile` (" +
                "`output` TEXT NOT NULL, " +
                "`enabled` INTEGER NOT NULL, " +
                "`bandLevels` TEXT NOT NULL, " +
                "`presetName` TEXT NOT NULL, " +
                "`bassBoost` INTEGER NOT NULL, " +
                "`virtualizer` INTEGER NOT NULL, " +
                "`preampMb` INTEGER NOT NULL, " +
                "PRIMARY KEY(`output`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `eq_preset` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`bandLevels` TEXT NOT NULL, " +
                "`bassBoost` INTEGER NOT NULL, " +
                "`virtualizer` INTEGER NOT NULL, " +
                "`preampMb` INTEGER NOT NULL)"
        )
    }
}

/**
 * Adds the playlist tables. Additive like [MIGRATION_1_2], and the DDL below is copied
 * verbatim from Room's exported `schemas/3.json` rather than hand-written — schema
 * validation compares them exactly, and the foreign key and index are easy to omit by
 * hand.
 */
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlist` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlist_item` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`playlistId` INTEGER NOT NULL, " +
                "`mediaStoreId` INTEGER NOT NULL, " +
                "`uri` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`artist` TEXT NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`playlistId`) REFERENCES `playlist`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_playlist_item_playlistId` " +
                "ON `playlist_item` (`playlistId`)"
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "vixplay.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun providePlaybackPositionDao(db: AppDatabase) = db.playbackPositionDao()

    @Provides
    fun provideEqDao(db: AppDatabase) = db.eqDao()

    @Provides
    fun providePlaylistDao(db: AppDatabase) = db.playlistDao()
}
