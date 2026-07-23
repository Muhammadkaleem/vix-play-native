package com.devbytes.vixplayer.app.di

import android.content.Context
import androidx.room.Room
import com.devbytes.vixplayer.app.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "vixplay.db").build()

    @Provides
    fun providePlaybackPositionDao(db: AppDatabase) = db.playbackPositionDao()
}
