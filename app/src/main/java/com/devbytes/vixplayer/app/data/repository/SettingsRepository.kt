package com.devbytes.vixplayer.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vixplay_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val PLAYBACK_SPEED          = floatPreferencesKey("playback_speed")
        val USE_AMOLED_THEME        = booleanPreferencesKey("use_amoled_theme")
        val SHOW_SUBTITLES_DEFAULT  = booleanPreferencesKey("show_subtitles_by_default")
    }

    val playbackSpeed: Flow<Float> = context.dataStore.data
        .map { it[Keys.PLAYBACK_SPEED] ?: 1.0f }

    val useAmoledTheme: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.USE_AMOLED_THEME] ?: false }

    val showSubtitlesByDefault: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.SHOW_SUBTITLES_DEFAULT] ?: true }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.PLAYBACK_SPEED] = speed }
    }

    suspend fun setUseAmoledTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_AMOLED_THEME] = enabled }
    }

    suspend fun setShowSubtitlesByDefault(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_SUBTITLES_DEFAULT] = enabled }
    }
}
