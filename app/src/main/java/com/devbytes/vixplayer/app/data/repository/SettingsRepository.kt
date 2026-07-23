package com.devbytes.vixplayer.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
        val BACKGROUND_PLAYBACK     = booleanPreferencesKey("background_playback")
        val SUB_PRESET              = stringPreferencesKey("subtitle_preset")
        val SUB_TEXT_SIZE           = floatPreferencesKey("subtitle_text_size")
        val SUB_BOTTOM_PADDING      = floatPreferencesKey("subtitle_bottom_padding")
        val SUB_TEXT_COLOR          = intPreferencesKey("subtitle_text_color")
        val SUB_BACKGROUND_COLOR    = intPreferencesKey("subtitle_background_color")
        val SUB_EDGE_TYPE           = intPreferencesKey("subtitle_edge_type")
        val SUB_TYPEFACE            = stringPreferencesKey("subtitle_typeface")
        val SUB_OVERRIDE_EMBEDDED   = booleanPreferencesKey("subtitle_override_embedded")
    }

    val playbackSpeed: Flow<Float> = context.dataStore.data
        .map { it[Keys.PLAYBACK_SPEED] ?: 1.0f }

    val useAmoledTheme: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.USE_AMOLED_THEME] ?: false }

    val showSubtitlesByDefault: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.SHOW_SUBTITLES_DEFAULT] ?: true }

    /**
     * Keep playing audio when the app goes to background. Defaults to **off**: this is a
     * video player, and silently continuing playback after backgrounding reads as a
     * battery bug rather than a feature. PiP already covers the Home-press case.
     */
    val backgroundPlayback: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.BACKGROUND_PLAYBACK] ?: false }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.PLAYBACK_SPEED] = speed }
    }

    suspend fun setUseAmoledTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_AMOLED_THEME] = enabled }
    }

    suspend fun setShowSubtitlesByDefault(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_SUBTITLES_DEFAULT] = enabled }
    }

    suspend fun setBackgroundPlayback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BACKGROUND_PLAYBACK] = enabled }
    }

    /**
     * Subtitle appearance. Global rather than per-file: this is a stable preference like
     * playback speed, unlike the sync offset which is a property of one release.
     */
    val subtitleStyle: Flow<SubtitleStyle> = context.dataStore.data.map { prefs ->
        val default = SubtitleStyle.DEFAULT
        SubtitleStyle(
            preset = prefs[Keys.SUB_PRESET]?.let { name ->
                SubtitlePreset.entries.firstOrNull { it.name == name }
            } ?: default.preset,
            textSizeFraction = prefs[Keys.SUB_TEXT_SIZE] ?: default.textSizeFraction,
            bottomPaddingFraction = prefs[Keys.SUB_BOTTOM_PADDING] ?: default.bottomPaddingFraction,
            textColor = prefs[Keys.SUB_TEXT_COLOR] ?: default.textColor,
            backgroundColor = prefs[Keys.SUB_BACKGROUND_COLOR] ?: default.backgroundColor,
            edgeType = prefs[Keys.SUB_EDGE_TYPE] ?: default.edgeType,
            typeface = prefs[Keys.SUB_TYPEFACE]?.let { name ->
                SubtitleTypeface.entries.firstOrNull { it.name == name }
            } ?: default.typeface,
            overrideEmbedded = prefs[Keys.SUB_OVERRIDE_EMBEDDED] ?: default.overrideEmbedded,
        )
    }

    suspend fun setSubtitleStyle(style: SubtitleStyle) {
        context.dataStore.edit {
            it[Keys.SUB_PRESET] = style.preset.name
            it[Keys.SUB_TEXT_SIZE] = style.textSizeFraction
            it[Keys.SUB_BOTTOM_PADDING] = style.bottomPaddingFraction
            it[Keys.SUB_TEXT_COLOR] = style.textColor
            it[Keys.SUB_BACKGROUND_COLOR] = style.backgroundColor
            it[Keys.SUB_EDGE_TYPE] = style.edgeType
            it[Keys.SUB_TYPEFACE] = style.typeface.name
            it[Keys.SUB_OVERRIDE_EMBEDDED] = style.overrideEmbedded
        }
    }
}
