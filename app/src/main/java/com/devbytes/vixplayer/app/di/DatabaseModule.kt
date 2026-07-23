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

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "vixplay.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun providePlaybackPositionDao(db: AppDatabase) = db.playbackPositionDao()

    @Provides
    fun provideEqDao(db: AppDatabase) = db.eqDao()
}
